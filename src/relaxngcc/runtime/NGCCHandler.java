package relaxngcc.runtime;

import java.text.MessageFormat;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
    
    protected final NGCCHandler parent;
    protected abstract NGCCRuntime getRuntime();
    
    /**
     * Cookie assigned by the parent.
     * 
     * This value will be passed to the onChildCompleted handler
     * of the parent.
     */
    protected final int cookie;
    
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
    private void unexpectedXXX(String token) throws SAXException {
        throw new SAXParseException(MessageFormat.format(
            "Unexpected {0} appears at line {1} column {2}",
            new Object[]{
                token,
                new Integer(getRuntime().getLocator().getLineNumber()),
                new Integer(getRuntime().getLocator().getColumnNumber()) }),
            getRuntime().getLocator());
    }
    public void unexpectedEnterElement(String qname) throws SAXException {
        unexpectedXXX('<'+qname+'>');
    }
    public void unexpectedLeaveElement(String qname) throws SAXException {
        unexpectedXXX("</"+qname+'>');
    }
    public void unexpectedEnterAttribute(String qname) throws SAXException {
        unexpectedXXX('@'+qname);
    }
    public void unexpectedLeaveAttribute(String qname) throws SAXException {
        unexpectedXXX("/@"+qname);
    }
    
    /** NGCCHandler that will be pushed to the runtime initially. */
/*    protected static final NGCCHandler terminator = new NGCCHandler() {
        protected void enterElement(String uri, String localName, String qname) {}
        protected void leaveElement(String uri, String localName, String qname) {}
        protected void text(String value) {}
        protected void enterAttribute(String uri, String localName, String qname) {}
        protected void leaveAttribute(String uri, String localName, String qname) {}
        protected void onChildCompleted( Object result, int cookie, boolean needAttCheck ) {}
        protected void processAttribute() {}
    };
*/
}
