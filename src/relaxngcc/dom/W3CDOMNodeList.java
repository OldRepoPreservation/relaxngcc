/*
 * W3CDOMNodeList.java
 *
 * Created on 2001/09/30, 11:39
 */

package relaxngcc.dom;
import java.util.ArrayList;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class W3CDOMNodeList implements NGCCNodeList
{
	private ArrayList _NodeList = new ArrayList();
	
    public W3CDOMNodeList(NodeList nl) {
        int len = nl.getLength();
        for( int i=0; i<len; i++ )
            if(nl.item(i).getNodeType()==Node.ELEMENT_NODE)
                _NodeList.add(nl.item(i));
    }

	public int getLength() { return _NodeList.size(); }
	
	public NGCCElement item(int index)
	{
        return new W3CDOMElement((Element)_NodeList.get(index));
        /*
		Node n = _NodeList.item(index);
        return 
		if(n.getNodeType()==Node.ELEMENT_NODE)
			return new W3CDOMElement((Element)n);
		else
			return null;
*/	}
}
