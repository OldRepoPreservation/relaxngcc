package relaxngcc.runtime;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Runtime Engine for RELAXNGCC execution.
 * 
 * This class has the following functionalities:
 * 
 * <ol>
 *  <li>Managing a stack of NGCCHandler objects and
 *      switching between them appropriately.
 * 
 *  <li>Keep track of all Attributes.
 * 
 *  <li>manage mapping between namespace URIs and prefixes.
 * 
 *  <li>TODO: provide support for interleaving.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCRuntime implements ContentHandler {
    
    public NGCCRuntime() {
        reset();
    }
    
    
    /**
     * Cleans up all the data structure so that the object can be reused later.
     * Normally, applications do not need to call this method directly,
     * 
     * as the runtime resets itself after the endDocument method.
     */
    public void reset() {
        attStack.clear();
        currentAtts = null;
        currentHandler = null;
        handlerStack.clear();
        indent=0;
        listMode = false;
        locator = null;
        namespaces.clear();
        needIndent = true;
        redirect = null;
        redirectionDepth = 0;
        text = new StringBuffer();
        
        // add a dummy attributes at the bottom as a "centinel."
        attStack.push(new AttributesImpl());
    }

    // current content handler can be acccessed via set/getContentHandler.
    
    private Locator locator;
    public void setDocumentLocator( Locator _loc ) { this.locator=_loc; }
    /**
     * Gets the source location of the current event.
     * 
     * <p>
     * One can call this method from RelaxNGCC handlers to access
     * the line number information. Note that to 
     */
    public Locator getLocator() { return locator; }
    

    /** stack of {@link Attributes}. */
    private final Stack attStack = new Stack();
    /** current attributes set. always equal to attStack.peek() */
    private AttributesImpl currentAtts;
    
    /**
     * Attributes that belong to the current element.
     * <p>
     * It's generally not recommended for applications to use
     * this method. RelaxNGCC internally removes processed attributes,
     * so this doesn't correctly reflect all the attributes an element
     * carries.
     */
    public Attributes getCurrentAttributes() {
        return currentAtts;
    }
    
    /** accumulated text. */
    private StringBuffer text = new StringBuffer();
    private boolean listMode = false;
    
    
    
    
    /** NGCCHandler stack */
    private final Stack handlerStack = new Stack();
    /** The current NGCCHandler. Always equals to handlerStack.peek() */
    private NGCCHandler currentHandler;
    
    /**
     * Pushes the new NGCCHandler object on top of the stack so that
     * it will receive objects.
     */
    public void pushHandler( NGCCHandler handler ) {
        handlerStack.push(handler);
        currentHandler = handler;
        indent++;
    }
    /**
     * A NGCCHandler pops itself when it finishes its work.
     * So the applications shouldn't call this method directly.
     */
    public void popHandler() {
        indent--;
        handlerStack.pop();
        currentHandler = (NGCCHandler)handlerStack.peek();
    }
    
    /**
     * Processes buffered text.
     * 
     * This method will be called by the start/endElement event to process
     * buffered text as a text event.
     * 
     * <p>
     * Whitespace handling is a tricky business. Consider the following
     * schema fragment:
     * 
     * <xmp>
     * <element name="foo">
     *   <choice>
     *     <element name="bar"><empty/></element>
     *     <text/>
     *   </choice>
     * </element>
     * </xmp>
     * 
     * Assume we hit the following instance:
     * <xmp>
     * <foo> <bar/></foo>
     * </xmp>
     * 
     * Then this first space needs to be ignored (for otherwise, we will
     * end up treating this space as the match to &lt;text/> and won't
     * be able to process &lt;bar>.)
     * 
     * Now assume the following instance:
     * <xmp>
     * <foo/>
     * </xmp>
     * 
     * This time, we need to treat this empty string as a text, for
     * otherwise we won't be able to accept this instance.
     * 
     * <p>
     * This is very difficult to solve in general, but one seemingly
     * easy solution is to use the type of next event. If a text is
     * followed by a start tag, it follows from the constraint on
     * RELAX NG that that text must be either whitespaces or a match
     * to &lt;text/>.
     * 
     * <p>
     * On the contrary, if a text is followed by a end tag, then it
     * cannot be whitespace unless the content model can accept empty,
     * in which case sending a text event will be harmlessly ignored
     * by the NGCCHandler.
     * 
     * <p>
     * Thus this method take one parameter, which controls the
     * behavior of this method.
     * 
     * <p>
     * TODO: according to the constraint of RELAX NG, if characters
     * follow an end tag, then they must be either whitespaces or
     * must match to &lt;text/>.
     * 
     * @param   possiblyWhitespace
     *      True if the buffered character can be ignorabale. False if
     *      it needs to be consumed.
     */
    private void processPendingText(boolean ignorable) throws SAXException {
        if(ignorable && text.toString().trim().length()==0)
            ; // ignore. See the above javadoc comment for the description
        else
            consumeText(text.toString());   // otherwise consume this token.
        
        // truncate StringBuffer, but avoid excessive allocation.
        if(text.length()>1024)  text = new StringBuffer();
        else                    text.setLength(0);
    }
    
    private void consumeText(String str) throws SAXException {
        if(listMode) {
            listMode = false;
            StringTokenizer t = new StringTokenizer(str, " \t\r\n");
            while(t.hasMoreTokens())
                currentHandler.text(t.nextToken());
        }
        else
            currentHandler.text(str);
    }
    
    public void setListMode() { listMode=true; }
    
    public void startElement(String uri, String localname, String qname, Attributes atts)
            throws SAXException {
        
        if(redirect!=null) {
            redirect.startElement(uri,localname,qname,atts);
            redirectionDepth++;
        } else {
	        processPendingText(true);
	//        System.out.println("startElement:"+localname+"->"+_attrStack.size());
	        currentHandler.enterElement(uri, localname, qname, atts);
        }
    }
    
    /**
     * Pushes a new attribute set.
     * 
     * <p>
     * Note that attributes are NOT pushed at the startElement method,
     * because the processing of the enterElement event can trigger
     * other attribute events and etc.
     * <p>
     * This method will be called from one of handlers when it truely
     * consumes the enterElement event.
     */
    public void pushAttributes( Attributes atts ) {
        attStack.push(currentAtts=new AttributesImpl(atts));
    }
    
    public void endElement(String uri, String localname, String qname)
            throws SAXException {
        
        if(redirect!=null) {
            redirect.endElement(uri,localname,qname);
            redirectionDepth--;
            
            if(redirectionDepth!=0)
                return;
                
            // finished redirection.
	        for( int i=0; i<namespaces.size(); i+=2 )
	            redirect.endPrefixMapping((String)namespaces.get(i));
	        redirect.endDocument();
            
            redirect = null;
            // then process this element normally
        }
        
        processPendingText(false);
        
        currentHandler.leaveElement(uri, localname, qname);
//        System.out.println("endElement:"+localname);
        Attributes a = (Attributes)attStack.pop();
        if(a.getLength()!=0) {
            // when debugging, it's useful to set a breakpoint here.
            ;
        }
        currentAtts = (AttributesImpl)attStack.peek();
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(redirect!=null)
            redirect.characters(ch,start,length);
        else
            text.append(ch,start,length);
    }
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if(redirect!=null)
            redirect.ignorableWhitespace(ch,start,length);
        else
            text.append(ch,start,length);
    }
    
    public int getAttributeIndex(String uri, String localname) {
        return currentAtts.getIndex(uri, localname);
    }
    public void consumeAttribute(int index) throws SAXException {
        final String uri    = currentAtts.getURI(index);
        final String local  = currentAtts.getLocalName(index);
        final String qname  = currentAtts.getQName(index);
        final String value  = currentAtts.getValue(index);
        currentAtts.removeAttribute(index);
        
        currentHandler.enterAttribute(uri,local,qname);
        consumeText(value);
        currentHandler.leaveAttribute(uri,local,qname);
    }


    public void startPrefixMapping( String prefix, String uri ) throws SAXException {
        if(redirect!=null)
            redirect.startPrefixMapping(prefix,uri);
        else {
	        namespaces.add(prefix);
	        namespaces.add(uri);
        }
    }
    
    public void endPrefixMapping( String prefix ) throws SAXException {
        if(redirect!=null)
            redirect.endPrefixMapping(prefix);
        else {
	        namespaces.remove(namespaces.size()-1);
	        namespaces.remove(namespaces.size()-1);
        }
    }
    
    public void skippedEntity( String name ) throws SAXException {
        if(redirect!=null)
            redirect.skippedEntity(name);
    }
    
    public void processingInstruction( String target, String data ) throws SAXException {
        if(redirect!=null)
            redirect.processingInstruction(target,data);
    }
    
    /** Impossible token. This value can never be a valid XML name. */
    private static final String IMPOSSIBLE = "\u0000";
    
    public void endDocument() throws SAXException {
        // consume the special "end document" token so that all the handlers
        // currently at the stack will revert to their respective parents.
        //
        // this is necessary to handle a grammar like
        // <start><ref name="X"/></start>
        // <define name="X">
        //   <element name="root"><empty/></element>
        // </define>
        //
        // With this grammar, when the endElement event is consumed, two handlers
        // are on the stack (because a child object won't revert to its parent
        // unless it sees a next event.)
        
        // pass around an "impossible" token.
        currentHandler.leaveElement(IMPOSSIBLE,IMPOSSIBLE,IMPOSSIBLE);
        
        reset();
    }
    public void startDocument() {}


//
//
// redirection of SAX2 events.
//
//
    /** When redirecting a sub-tree, this value will be non-null. */
    private ContentHandler redirect = null;
    
    /**
     * Counts the depth of the elements when we are re-directing
     * a sub-tree to another ContentHandler.
     */
    private int redirectionDepth = 0;

    /**
     * This method can be called only from the enterElement handler.
     * The sub-tree rooted at the new element will be redirected
     * to the specified ContentHandler.
     * 
     * <p>
     * Currently active NGCCHandler will only receive the leaveElement
     * event of the newly started element.
     * 
     * @param   uri,local,qname
     *      Parameters passed to the enter element event. Used to
     *      simulate the startElement event for the new ContentHandler.
     */
    public void redirectSubtree( ContentHandler child,
        String uri, String local, String qname ) throws SAXException {
        
        redirect = child;
        redirect.setDocumentLocator(locator);
        redirect.startDocument();
        
        // TODO: when a prefix is re-bound to something else,
        // the following code is potentially dangerous. It should be
        // modified to report active bindings only.
        for( int i=0; i<namespaces.size(); i+=2 )
            redirect.startPrefixMapping(
                (String)namespaces.get(i),
                (String)namespaces.get(i+1)
            );
        
        redirect.startElement(uri,local,qname,currentAtts);
        redirectionDepth=1;
    }

//
//
// validation context implementation
//
//
    /** in-scope namespace mapping.
     * namespaces[2n  ] := prefix
     * namespaces[2n+1] := namespace URI */
    private final ArrayList namespaces = new ArrayList();
    
    public String resolveNamespacePrefix( String prefix ) {
        for( int i = namespaces.size()-2; i>=0; i-=2 )
            if( namespaces.get(i).equals(prefix) )
                return (String)namespaces.get(i+1);
        
        // no binding was found.
        if(prefix.equals(""))   return "";  // return the default no-namespace
        else    return null;    // prefix undefined
    }


// error reporting
    protected void unexpectedXXX(String token) throws SAXException {
        throw new SAXParseException(MessageFormat.format(
            "Unexpected {0} appears at line {1} column {2}",
            new Object[]{
                token,
                new Integer(getLocator().getLineNumber()),
                new Integer(getLocator().getColumnNumber()) }),
            getLocator());
    }


//
//
// spawns a new child object from event handlers.
//
//
    public void spawnChildFromEnterElement(
        NGCCHandler h, String uri, String localname, String qname, Attributes atts) throws SAXException {
            
        pushHandler(h);
        currentHandler.enterElement(uri,localname,qname,atts);
    }
    public void spawnChildFromEnterAttribute(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.enterAttribute(uri,localname,qname);
    }
    public void spawnChildFromLeaveElement(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.leaveElement(uri,localname,qname);
    }
    public void spawnChildFromLeaveAttribute(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.leaveAttribute(uri,localname,qname);
    }
    public void spawnChildFromText(
        NGCCHandler h, String value) throws SAXException {
            
        pushHandler(h);
        currentHandler.text(value);
    }
    
//
//
// reverts to the parent object from the child handler
//
//
    public void revertToParentFromEnterElement( Object result, int cookie,
        String uri,String local,String qname, Attributes atts ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie,true);
        currentHandler.enterElement(uri,local,qname,atts);
    }
    public void revertToParentFromLeaveElement( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
        
        if(uri==IMPOSSIBLE && local==IMPOSSIBLE && qname==IMPOSSIBLE
        && handlerStack.size()==1)
            // all the handlers are properly finalized.
            // quit now, because we don't have any more NGCCHandler.
            // see the endDocument handler for detail
            return;
        
        popHandler();
        currentHandler.onChildCompleted(result,cookie,true);
        currentHandler.leaveElement(uri,local,qname);
    }
    public void revertToParentFromEnterAttribute( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie,false);
        currentHandler.enterAttribute(uri,local,qname);
    }
    public void revertToParentFromLeaveAttribute( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie,false);
        currentHandler.leaveAttribute(uri,local,qname);
    }
    public void revertToParentFromText( Object result, int cookie,
        String text ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie,true);
        currentHandler.text(text);
    }


//
//
// trace functions
//
//
    private int indent=0;
    private boolean needIndent=true;
    private void printIndent() {
        for( int i=0; i<indent; i++ )
            System.out.print("  ");
    }
    public void trace( String s ) {
        if(needIndent) {
            needIndent=false;
            printIndent();
        }
        System.out.print(s);
    }
    public void traceln( String s ) {
        trace(s);
        trace("\n");
        needIndent=true;
    }
}
