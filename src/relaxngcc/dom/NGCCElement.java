/*
 * NGCCElement.java
 *
 * Created on 2001/09/30, 11:33
 */

package relaxngcc.dom;

import relaxngcc.NGCCGrammar;

/**
 * abstraction of W3C dom and nonXML-style dom
 */
public abstract class NGCCElement
{
	public abstract String getAttribute(String name);
	public abstract boolean hasAttribute(String name);
	public abstract String getAttributeNS(String uri, String name);

    public final String getAttribute(String name,String defaultValue) {
        if(hasAttribute(name))  return getAttribute(name);
        else                    return defaultValue;
    }
    
    /**
     * Gets the value of NGCC attribute.
     * If the attribute is not specified, the defaultValue
     * will be returned.
     */
    public final String attributeNGCC( String localName, String defaultValue ) {
        if(hasAttributeNGCC(localName))
            return getAttributeNS(NGCCGrammar.NGCC_NSURI,localName);
        else
            return defaultValue;
    }
    
    /**
     * Checks if this element has the specified NGCC attribute.
     */
    public abstract boolean hasAttributeNGCC( String localName );
    
	public abstract NGCCNodeList getChildNodes();
	public abstract NGCCElement getFirstChild();
	public abstract String getLocalName();
	public abstract String getNamespaceURI();
	public abstract String getFullText();
	public abstract String getPath();
}
