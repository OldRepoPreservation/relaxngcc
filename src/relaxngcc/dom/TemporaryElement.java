/*
 * TemporaryElement.java
 *
 * Created on 2002/01/02, 17:51
 */

package relaxngcc.dom;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Map;
import java.util.TreeMap;
import relaxngcc.NGCCGrammar;

/**
 * a placeholder of define elements which have combine attribute.
 */
public class TemporaryElement extends NGCCElement
{
	private String _Name;
	private final Vector _Children = new Vector();
	private final Map _Attributes = new Hashtable();
	
    public TemporaryElement(String localname) {
		_Name = localname;
    }
	public String getAttribute(String name)
	{
		Object o = _Attributes.get(name);
		if(o!=null) return (String)o;
		else return "";
	}
    
    
	public boolean hasAttribute(String name) {
		return _Attributes.get(name)!=null;
	}
    
    public boolean hasAttributeNGCC(String localName) {
        return hasAttribute(localName); // ??? Kohsuke
    }
    
	public String getAttributeNS(String uri, String name) { return ""; } //not supported
	public String getLocalName() { return _Name; }
	public String getNamespaceURI()	{ return NGCCGrammar.RELAXNG_NSURI;	}
	public String getPath() { throw new UnsupportedOperationException("temporary element does not support getPath()"); }
	
	public String getFullText()
	{
		String s = new String();
		for(int i=0; i<_Children.size(); i++)
		{
			Object c = _Children.get(i);
			if(c instanceof String)
				return s += (String)c;
		}
		return s;
	}
	
	public NGCCNodeList getChildNodes() { return new TemporaryNodeList(_Children); }
	
	public void addAttribute(String name, String value) { _Attributes.put(name,value); }
	public void addChild(Object o) { _Children.add(o); }
	
	public NGCCElement getFirstChild()
	{
		return new TemporaryNodeList(_Children).item(0);
	}
	
	
}
