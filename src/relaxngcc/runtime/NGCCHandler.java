package relaxngcc.runtime;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class NGCCHandler {
    protected NGCCHandler(
        NGCCHandler _parent, int _parentCookie ) {
            
        this.parent = _parent;
        this.cookie = _parentCookie;
    }
    
    /**
     * Parent NGCCHandler, if any.
     * If this is the root handler, this field will be null.
     */
    protected final NGCCHandler parent;
    /**
     * This method will be implemented by the generated code
     * and returns a reference to the current runtime.
     */
    protected abstract NGCCRuntime getRuntime();
    
    /**
     * Cookie assigned by the parent.
     * 
     * This value will be passed to the onChildCompleted handler
     * of the parent.
     */
    protected final int cookie;
    
    // used to copy parameters to (enter|leave)(Element|Attribute) events.
    protected String localName,uri,qname;
    
    protected abstract void enterElement(String uri, String localName, String qname,Attributes atts) throws SAXException;
    protected abstract void leaveElement(String uri, String localName, String qname) throws SAXException;
    protected abstract void text(String value) throws SAXException;
    protected abstract void enterAttribute(String uri, String localName, String qname) throws SAXException;
    protected abstract void leaveAttribute(String uri, String localName, String qname) throws SAXException;
    
    /**
     * Notifies the completion of a child object.
     * 
     * @param result
     *      The parsing result of the child state.
     * @param cookie
     *      The cookie value passed to the child object
     *      when it is created.
     * @param needAttCheck
     *      This flag is true when the callee needs to call the
     *      processAttribute method to check attribute transitions.
     *      This flag is set to false when this method is triggered by
     *      attribute transition.
     */
    protected abstract void onChildCompleted( Object result, int cookie, boolean needAttCheck ) throws SAXException;
    
    /**
     * Checks if it can perform transitions by attributes.
     * If it can, perform transitions.
     */
    protected abstract void processAttribute() throws SAXException;
// TODO: what is this used for?
//    protected abstract boolean accepted();


//
//
// error handler
//
//
    public void unexpectedEnterElement(String qname) throws SAXException {
        getRuntime().unexpectedXXX('<'+qname+'>');
    }
    public void unexpectedLeaveElement(String qname) throws SAXException {
        getRuntime().unexpectedXXX("</"+qname+'>');
    }
    public void unexpectedEnterAttribute(String qname) throws SAXException {
        getRuntime().unexpectedXXX('@'+qname);
    }
    public void unexpectedLeaveAttribute(String qname) throws SAXException {
        getRuntime().unexpectedXXX("/@"+qname);
    }
}
