/*
 * NGCCElement.java
 *
 * Created on 2001/09/30, 11:33
 */

package relaxngcc.dom;

/**
 * abstraction of W3C dom and nonXML-style dom
 */
public interface NGCCElement
{
	public String getAttribute(String name);
	public boolean hasAttribute(String name);
	public String getAttributeNS(String uri, String name);
	public NGCCNodeList getChildNodes();
	public NGCCElement getFirstChild();
	public String getLocalName();
	public String getNamespaceURI();
	public String getFullText();
	public String getPath();
}
