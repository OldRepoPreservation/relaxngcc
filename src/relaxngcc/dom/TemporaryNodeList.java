/*
 * TemporaryNodeList.java
 *
 * Created on 2002/01/03, 11:51
 */

package relaxngcc.dom;
import java.util.Vector;

public class TemporaryNodeList implements NGCCNodeList
{
	private Vector _Children;
    
	public TemporaryNodeList(Vector c)
	{
		_Children = c;
    }
	
	public int getLength() { return _Children.size(); }
	
	public NGCCElement item(int index)
	{
		Object t = _Children.get(index);
		if(t instanceof relaxngcc.dom.NGCCElement) return (relaxngcc.dom.NGCCElement)t;
		else
		{
			System.err.println("unexpected object at index " + index + " " + t.getClass().getName());
			return null;
		}
	}
	
}
