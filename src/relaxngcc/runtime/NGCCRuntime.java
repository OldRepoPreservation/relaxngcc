package relaxngcc.runtime;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Stack;
import java.util.StringTokenizer;

import org.relaxng.datatype.ValidationContext;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

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
// TODO: support redirecting a subtree to another ContentHandler.
public class NGCCRuntime extends XMLFilterImpl implements ValidationContext {
    
    public NGCCRuntime() {
        // add a dummy attributes at the bottom as a "centinel."
        attStack.push(new AttributesImpl());
    }

    // current content handler can be acccessed via set/getContentHandler.
    
    private Locator locator;
    public Locator getLocator() { return locator; }
    public void setDocumentLocator( Locator _loc ) { this.locator=_loc; }
    

    /** stack of {@link Attributes}. */
    private final Stack attStack = new Stack();
    /** current attributes set. always equal to attStack.peek() */
    private AttributesImpl currentAtts;
    
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
    
    public void pushHandler( NGCCHandler handler ) {
        handlerStack.push(handler);
        currentHandler = handler;
    }
    public void popHandler() {
        handlerStack.pop();
        currentHandler = (NGCCHandler)handlerStack.peek();
    }
    
    private void processPendingText() throws SAXException {
        if(text.length()==0)    return;
        consumeText(text.toString());
        
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
        
        processPendingText();
        attStack.push(currentAtts=new AttributesImpl(atts));
//        System.out.println("startElement:"+localname+"->"+_attrStack.size());
        currentHandler.enterElement(uri, localname, qname);
    }
    
    public void endElement(String uri, String localname, String qname)
            throws SAXException {
                
        processPendingText();
        currentHandler.leaveElement(uri, localname, qname);
//        System.out.println("endElement:"+localname);
        attStack.pop();
        currentAtts = (AttributesImpl)attStack.peek();
    }
    
    public void characters(char[] ch, int start, int length) throws SAXException {
        text.append(ch,start,length);
    }
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
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
        super.startPrefixMapping(prefix,uri);
        namespaces.add(prefix);
        namespaces.add(uri);
    }
    
    public void endPrefixMapping( String prefix ) throws SAXException {
        namespaces.remove(namespaces.size()-1);
        namespaces.remove(namespaces.size()-1);
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
    
    public String getBaseUri() {
        return null;    // TODO
    }
    
    public boolean isNotation(String s) {
        // there is no point in seriously implementing this method
        return true;
    }
    
    public boolean isUnparsedEntity(String s) {
        // there is no point in seriously implementing this method
        return true;
    }


//
//
// spawns a new child object from event handlers.
//
//
    public void spawnChildFromEnterElement(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.enterElement(uri,localname,qname);
    }
    public void spawnChildFromEnterAttribute(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.enterAttribute(uri,localname,qname);
    }
    public void spawnChildFromLeaveElement(
        NGCCHandler h, String uri, String localname, String qname) throws SAXException {
            
        pushHandler(h);
        currentHandler.enterElement(uri,localname,qname);
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
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie);
        currentHandler.enterElement(uri,local,qname);
    }
    public void revertToParentFromLeaveElement( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie);
        currentHandler.leaveElement(uri,local,qname);
    }
    public void revertToParentFromEnterAttribute( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie);
        currentHandler.enterAttribute(uri,local,qname);
    }
    public void revertToParentFromLeaveAttribute( Object result, int cookie,
        String uri,String local,String qname ) throws SAXException {
            
        popHandler();
        currentHandler.onChildCompleted(result,cookie);
        currentHandler.leaveAttribute(uri,local,qname);
    }



}
