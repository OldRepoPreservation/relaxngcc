/*
 * W3CDOMNodeList.java
 *
 * Created on 2001/09/30, 11:39
 */

package relaxngcc.dom;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class W3CDOMNodeList implements NGCCNodeList
{
	private NodeList _NodeList;
	
    public W3CDOMNodeList(NodeList nl) { _NodeList=nl; }

	public int getLength() { return _NodeList.getLength(); }
	
	public NGCCElement item(int index)
	{
		Node n = _NodeList.item(index);
		if(n.getNodeType()==Node.ELEMENT_NODE)
			return new W3CDOMElement((Element)n);
		else
			return null;
	}
}
