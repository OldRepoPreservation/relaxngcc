/*
 * NonXmlNodeList.java
 *
 * Created on 2001/09/30, 11:59
 */

package relaxngcc.dom;
import com.thaiopensource.relaxng.nonxml.Element;
import java.util.Vector;

public class NonXmlNodeList implements NGCCNodeList
{
	private Vector _NodeList;
	
	/** Creates new NonXmlNodeList */
    public NonXmlNodeList(Vector nl) { _NodeList=nl; }

	public int getLength() { return _NodeList.size(); }
	
	public NGCCElement item(int index)
	{
		Object c = _NodeList.get(index);
		if(c instanceof NonXmlElement)
			return (NonXmlElement)c;
		else
			return null;
	}
	
}
