/*
 * NameClass.java
 *
 * Created on 2002/01/02, 20:07
 */

package relaxngcc.builder;
import java.util.Vector;
import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.NGCCNodeList;
import relaxngcc.dom.TemporaryElement;

public class NameClass implements Comparable
{
	//creates from 'element' element
	public static NameClass fromElementElement(ScopeInfo sci, NGCCElement e, String nsuri)
	{
		TemporaryElement t = new TemporaryElement("name");
		t.addAttribute("ns", nsuri);
		t.addChild(e.getAttribute("name"));
		return new NameClass(sci, t, true, nsuri);
	}
	public static NameClass fromNameClassElement(ScopeInfo sci, NGCCElement e, String nsuri)
	{
		return new NameClass(sci, e, false, nsuri);
	}
	
	private NGCCElement _Element;
	private String _NSURI; //available only when 'name' element
	private Vector _Children;
	
	/** Creates new NameClass */
    private NameClass(ScopeInfo sci, NGCCElement elem, boolean as_name, String nsuri)
	{
		_Element = elem;
		_Children = new Vector();
		if(_Element.hasAttribute("ns"))
		{
			sci.addNSURI(_Element.getAttribute("ns"));
			nsuri = _Element.getAttribute("ns");
		}
		_NSURI = nsuri;
		
		if(!as_name)
		{
			NGCCNodeList nl = elem.getChildNodes();
			for(int i=0; i<nl.getLength(); i++)
			{
				NGCCElement ch = nl.item(i);
				if(ch==null) continue;
				if(ch.getLocalName().equals("name"))
					_Children.add(new NameClass(sci, ch, true, nsuri));
				else if(ch.getLocalName().equals("choice"))
					_Children.add(new NameClass(sci, ch, false, nsuri));
				else
					_Children.add(new NameClass(sci, ch, false, nsuri));
			}
		}
    }
	
	public String getName()
	{
		String n = _Element.getLocalName();
		if(n.equals("name")) return _Element.getFullText();
		else return "";
	}
	public String getNSURI() { return _NSURI; }
	
	public String createJudgementClause(ScopeInfo sci, String nsuri_variable, String localname_variable)
	{
		StringBuffer buf = new StringBuffer();
		_createJudgementClause(sci, buf, nsuri_variable, localname_variable);
		return buf.toString();
	}
	
	private void _createJudgementClause(ScopeInfo sci, StringBuffer buf, String nsuri_variable, String localname_variable)
	{
		String n = _Element.getLocalName();
		if(n.equals("name"))
		{
			buf.append(localname_variable);
			buf.append(".equals(\"");
			buf.append(_Element.getFullText());
			buf.append("\") && ");
			buf.append(nsuri_variable);
			buf.append(".equals(");
            // TODO: this doesn't work for attributes but I couldn't figure out why.
            // - Kohsuke
//			buf.append(sci.getNSStringConstant(_NSURI));
            buf.append("\""+_NSURI +"\"");
			buf.append(")");
		}
		else if(n.equals("anyName"))
		{
			if(_Children.size()==0)
				buf.append("true");
			else
			{
				//skips 'except' element
				Vector v = ((NameClass)_Children.get(0))._Children;
				for(int i=0; i<v.size(); i++)
				{
					if(i!=0) buf.append(" && ");
					buf.append("!(");
					((NameClass)v.get(i))._createJudgementClause(sci, buf, nsuri_variable, localname_variable);
					buf.append(")");
				}
			}
		}
		else if(n.equals("nsName"))
		{
			buf.append(nsuri_variable);
			buf.append(".equals(");
			buf.append(sci.getNSStringConstant(_Element.getAttribute("ns")));
			buf.append(")");
			
			//skips 'except' element
            if(_Children.size()!=0) {
    			Vector v = ((NameClass)_Children.get(0))._Children;
    			for(int i=0; i<v.size(); i++)
    			{
    				buf.append(" && ");
    				buf.append("!(");
    				((NameClass)v.get(i))._createJudgementClause(sci, buf, nsuri_variable, localname_variable);
    				buf.append(")");
    			}
            }
		}
		else if(n.equals("choice"))
		{
			for(int i=0; i<_Children.size(); i++)
			{
				if(i!=0) buf.append(" || ");
				buf.append("(");
				((NameClass)_Children.get(i))._createJudgementClause(sci, buf, nsuri_variable, localname_variable);
				buf.append(")");
			}
		}
	}
	
	public int compareTo(Object obj)
	{
		NameClass opp = (NameClass)obj;
		String n1 = _Element.getLocalName();
		String n2 = opp._Element.getLocalName();
		int t = n1.compareTo(n2);
		if(t!=0) return t;
		
		if(n1.equals("name"))
		{
			t = _Element.getFullText().compareTo(opp._Element.getFullText());
			if(t!=0) return t;
			t = _Element.getAttribute("ns").compareTo(opp._Element.getAttribute("ns"));
			if(t!=0) return t;
		}
		
		t = _Children.size() - opp._Children.size();
		if(t!=0) return t;
		
		for(int i=0; i<_Children.size(); i++)
		{
			t = ((NameClass)_Children.get(i)).compareTo(opp._Children.get(i));
			if(t!=0) return t;
		}
		return 0;
	}
	public boolean equals(Object obj)
	{
		return compareTo(obj)==0;
	}
	public String toString()
	{
		String n = _Element.getLocalName();
		if(n.equals("name"))
			return _Element.getFullText();
		else
			return _Element.getLocalName();
	}
}
