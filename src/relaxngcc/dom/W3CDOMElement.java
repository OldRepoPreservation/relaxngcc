/*
 * W3CDOMElement.java
 *
 * Created on 2001/09/30, 11:37
 */

package relaxngcc.dom;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class W3CDOMElement implements NGCCElement
{
	private Element _Element;
	
    public W3CDOMElement(Element e)
	{ _Element = e; }
	
	public NGCCNodeList getChildNodes() { return new W3CDOMNodeList(_Element.getChildNodes()); }
	
	public String getAttributeNS(String uri, String name) { return _Element.getAttributeNS(uri,name); }
	
	public String getAttribute(String name) { return _Element.getAttribute(name); }
	
	public boolean hasAttribute(String name) { return _Element.hasAttribute(name); }
	
	public String getLocalName() { return _Element.getLocalName(); }
	
	public String getNamespaceURI() { return _Element.getNamespaceURI(); }

	public String getFullText()
	{
		String r = "";
		NodeList nl = _Element.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node n = nl.item(i);
			if(n.getNodeType()==Node.TEXT_NODE || n.getNodeType()==Node.CDATA_SECTION_NODE) r += n.getNodeValue();
		}
		return r;
	}
	
	public String getPath()
	{
		StringBuffer buf = new StringBuffer();
		fillXPath(_Element, buf);
		return buf.toString();
	}
	
	private static String safeStr(String s)
	{
		return s==null? "" : s;
	}
	
	public NGCCElement getFirstChild()
	{
		NodeList nl = _Element.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node n = nl.item(i);
			if(n.getNodeType()==Node.ELEMENT_NODE) return new W3CDOMElement((Element)n);
		}
		return null;
	}

	private void fillXPath(Element e, StringBuffer buf)
	{
		Node pn = e.getParentNode();
		if(pn!=null && pn instanceof Element) fillXPath((Element)pn, buf);
		String localname = e.getLocalName();
		int count = 0;
		Node brother = e.getPreviousSibling();
		while(brother!=null)
		{
			if(brother.getNodeType()==Node.ELEMENT_NODE && brother.getLocalName().equals(localname)) count++;
			brother = brother.getPreviousSibling();
		}
		
		buf.append('/');
		buf.append(localname);
		if(count > 0)
		{
			buf.append('[');
			buf.append(count+1);
			buf.append(']');
		}
	}	
}
