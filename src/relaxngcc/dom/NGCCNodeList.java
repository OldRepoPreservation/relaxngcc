/*
 * NGCCNodeList.java
 *
 * Created on 2001/09/30, 11:36
 */

package relaxngcc.dom;

/**
 * abstraction of W3C dom and nonXML-style dom
 */
public interface NGCCNodeList
{
	public int getLength();
	public NGCCElement item(int index);
}
