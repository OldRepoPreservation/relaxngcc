/*
 * NGCCTypedContentHandler.java
 *
 * Created on 2001/08/14, 20:51
 */

package relaxngcc.runtime;
import com.sun.msv.verifier.psvi.TypedContentHandler;
import com.sun.msv.grammar.ElementExp;
import com.sun.msv.grammar.AttributeExp;
import com.sun.msv.verifier.psvi.TypeDetector;
import com.sun.msv.datatype.xsd.XSDatatype;
import org.relaxng.datatype.ValidationContext;
import org.relaxng.datatype.Datatype;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import java.util.Stack;
import java.util.Vector;

/**
 * Base class for classes generated by RelaxNGCC msv mode.  
 */
public abstract class NGCCTypedContentHandler implements TypedContentHandler
{
	protected TypeDetector _ngcc_reader; //XML data source
	protected NGCCTypedContentHandler _ngcc_parent; //handler to be recovered at the end of current automaton 
	protected Stack _attrStack;

	private class TypedValue
	{
		public String literal;
		public XSDatatype type;
		public TypedValue(String lit, XSDatatype t)
		{ literal=lit; type=t; }
	}
	private class Attribute
	{
		public String uri;
		public String localname;
		public String qname;
		public Vector values;
		public Attribute(String u, String l, String q)
		{ uri=u; localname=l; qname=q; values=new Vector(); }
	}
	
	private Attribute _current_attribute;
	
	public NGCCTypedContentHandler(TypeDetector reader)
	{ this(reader, null); }
	
	public NGCCTypedContentHandler(TypeDetector reader, NGCCTypedContentHandler parent)
	{
		_ngcc_reader = reader;
		_ngcc_parent = parent;
		_attrStack = new Stack();
		initState();
    }
	//main handler. the classes generated by RelaxNGCC overrides these methods.
	public abstract void enterElement(String uri, String localName, String qname) throws SAXException;
	public abstract void leaveElement(String uri, String localName, String qname) throws SAXException;
	public abstract void text(String value, XSDatatype type) throws SAXException;
	public abstract void processAttribute() throws SAXException;
    public abstract boolean accepted();
	protected abstract void initState();


	public void startDocument(ValidationContext context) throws SAXException
	{
	}
	public void endDocument() throws SAXException
	{
        if(!accepted()) throw new SAXException("Unexpected end of document");
	}

	
	private String reserved_uri, reserved_localname, reserved_qname;
	public void startElement(String uri, String localname, String qname) throws SAXException
	{
		_attrStack.push(new Vector());
		reserved_uri = uri;
		reserved_localname = localname;
		reserved_qname = qname;
	}
	public void endElement(String uri, String localname, String qname, ElementExp type) throws SAXException
	{
		if(!_attrStack.empty()) _attrStack.pop();
		leaveElement(uri, localname, qname);
	}
	
	public void startAttribute(String uri, String localname, String qname) throws SAXException
	{
		_current_attribute = new Attribute(uri, localname, qname);
	}
	public void endAttribute(String uri, String localname, String qname, AttributeExp type) throws SAXException
	{
		((Vector)_attrStack.peek()).add(_current_attribute);
		_current_attribute = null;
	}
	public void endAttributePart() throws SAXException
	{
		releaseReservedEnterElement();
	}
	
	public void characterChunk(String literal, Datatype type) throws SAXException
	{
		XSDatatype dt = (type instanceof XSDatatype)? (XSDatatype)type : null;
		if(_current_attribute!=null) //in attribute
			_current_attribute.values.add(new TypedValue(literal, dt));
		else
			text(literal, dt);
	}
	
	private void releaseReservedEnterElement() throws SAXException
	{
		enterElement(reserved_uri, reserved_localname, reserved_qname);
		reserved_uri = reserved_localname = reserved_qname = null;
	}
	
	protected int getAttributeIndex(String uri, String localname)
	{
		Vector attrs = (Vector)_attrStack.peek();
		for(int i=0; i<attrs.size(); i++)
		{
			Attribute a = (Attribute)attrs.get(i);
			if(a.localname.equals(localname) && a.uri.equals(uri)) return i;
		}
		return -1; //when not exists
	}
	protected void consumeAttribute(int index) throws SAXException
	{
		Vector attrs = (Vector)_attrStack.peek();
		Attribute a = (Attribute)attrs.get(index);
		attrs.remove(index);
		for(int i=0; i<a.values.size(); i++)
		{
			TypedValue t = (TypedValue)a.values.get(i);
			text(t.literal, t.type);
		}
	}

	protected Locator getLocator() { return _ngcc_reader.getLocator(); }
	
	protected void setupNewHandler(NGCCTypedContentHandler h, String uri, String localname, String qname) throws SAXException
	{
		_ngcc_reader.setContentHandler(h);
		h.startElement(uri,localname,qname);
		h.replaceTopOfAttributeStack((Vector)_attrStack.pop());
		h.endAttributePart();
		h.processAttribute();
	}
	protected void setupNewHandler(NGCCTypedContentHandler h) throws SAXException
	{
		_ngcc_reader.setContentHandler(h);
		h.replaceTopOfAttributeStack((Vector)_attrStack.peek());
		h.processAttribute();
	}
	private void replaceTopOfAttributeStack(Vector attrs) //called from only setupNewHandler
	{
		_attrStack.pop();
		_attrStack.push(attrs);
	}
	
	protected void resetHandlerByAttr() throws SAXException
	{ _ngcc_reader.setContentHandler(_ngcc_parent); _ngcc_parent.processAttribute(); }
	protected void resetHandlerByStart(String uri, String localname, String qname) throws SAXException
	{
		_ngcc_reader.setContentHandler(_ngcc_parent);
		_ngcc_parent.startElement(uri,localname,qname);
		_ngcc_parent.replaceTopOfAttributeStack((Vector)_attrStack.peek());
		_ngcc_parent.endAttributePart();
	}
	protected void resetHandlerByEnd(String uri, String localname, String qname) throws SAXException
	{
		_ngcc_reader.setContentHandler(_ngcc_parent);
		_ngcc_parent.endElement(uri,localname,qname,null);
	}
	
	protected void throwUnexpectedElementException(String qname) throws SAXException
	{ throw new SAXException("Unexpected element [" + qname + "] appears. (L" + getLocator().getLineNumber() + ",C" + getLocator().getColumnNumber() + ")"); } 
}
