/*
 * NonXmlElement.java
 *
 * Created on 2001/09/30, 11:54
 */

package relaxngcc.dom;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import java.util.TreeMap;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import com.thaiopensource.relaxng.nonxml.Element;
import com.thaiopensource.relaxng.nonxml.SchemaBuilderImpl;
import com.thaiopensource.relaxng.nonxml.NonXmlSyntax;
import relaxngcc.NGCCGrammar;
import relaxngcc.NGCCException;


public class NonXmlElement extends NGCCElement
{
	public static final int RELAXNG_ELEMENT = 1;
	public static final int NGCC_ELEMENT = 2;
	
	private String _LocalName;
	private int _Type;
	private final Map _Attributes = new HashMap();
	private Vector _Children;
	
	public NGCCNodeList getChildNodes() { return new NonXmlNodeList(_Children); }
	
	public String getAttributeNS(String uri, String name)
	{ return getAttribute(name); } //don't care namespace for nonxml support

	
	public String getAttribute(String name)
	{
		Object o = _Attributes.get(name);
		return o==null? "" : (String)o;
	}
	
	public String getLocalName()
	{
		return _LocalName;
	}
	public String getNamespaceURI()
	{
		if(_Type==RELAXNG_ELEMENT)
			return NGCCGrammar.RELAXNG_NSURI;
		else
			return NGCCGrammar.NGCC_NSURI;
	}
	
	public String getFullText()
	{
		return (String)_Children.get(0);
	}
	
	public NGCCElement getFirstChild()
	{
		for(int i=0; i<_Children.size(); i++)
		{
			Object c = _Children.get(i);
			if(c instanceof NonXmlElement)
				return (NonXmlElement)c;
		}
		return null;
	}
	
	public boolean hasAttribute(String name)
	{
		return _Attributes.get(name)!=null;
	}
    
    public boolean hasAttributeNGCC( String localName ) {
        return hasAttribute(localName); // ??? - Kohsuke
    }
	
	public String getPath()
	{
		throw new UnsupportedOperationException("path information is not available for NonXML-syntax.");
	}
	
	public static NonXmlElement create(Element root) throws NGCCException
	{
		//find prefix of RelaxNGCC
		Vector attrs = root.attributes;
		String ngcc_prefix = null;
		for(int i=1; i<attrs.size(); i+=2)
			if(((String)attrs.get(i)).equals(NGCCGrammar.NGCC_NSURI)) ngcc_prefix = afterColon((String)attrs.get(i-1));
		if(ngcc_prefix==null) throw new NGCCException("RelaxNGCC namespace declaration not found");
		return new NonXmlElement(RELAXNG_ELEMENT, root, ngcc_prefix);
	}
	
	private NonXmlElement(int type, Element e, String ngcc_prefix) throws NGCCException
	{
		_LocalName = afterColon(e.name);
		//System.err.println("<E> " + _LocalName);
		_Type = type; 
        
		for(int i=0; i<e.attributes.size(); i+=2) {
			String an = (String)e.attributes.get(i);
			String av = (String)e.attributes.get(i+1);
			_Attributes.put(afterColon(an), av);
		}
		
		_Children = new Vector();
		if(e.childAnnotations!=null)
		{
			for(int i=0; i<e.childAnnotations.size(); i++)
			{
				NonXmlElement c = guessAnnotation((String)e.childAnnotations.get(i), ngcc_prefix);
				if(c!=null) _Children.add(c);
			}
		}
		for(int i=0; i<e.children.size(); i++)
		{
			Object o = e.children.get(i);
			if(o instanceof String)
			{ _Children.add(o); System.err.println("Str " + (String)o); }
			else if(o instanceof Element)
			{
				Element ch = (Element)o;
				_Children.add(new NonXmlElement(RELAXNG_ELEMENT, ch, ngcc_prefix));
				if(ch.followingAnnotations!=null)
				{
					for(int j=0; j<ch.followingAnnotations.size(); j++)
					{
						NonXmlElement nc = guessAnnotation((String)ch.followingAnnotations.get(j), ngcc_prefix);
						if(nc!=null) _Children.add(nc);
					}
				}
			}
		}
	}
	
	private NonXmlElement(int type, String localName, String content)
	{
		_LocalName = localName;
		_Type = type; 
		_Children = new Vector();
		_Children.add(content);
	}
	
	private NonXmlElement guessAnnotation(String src, String ngcc_prefix) throws NGCCException
	{
		src = src.trim();
		String head = "<"+ngcc_prefix+":";
		if(!src.startsWith(head)) return null; //ignore
		int end_of_open_tag = src.indexOf('>');
		if(end_of_open_tag==-1) throw new NGCCException("invalid annotation for RelaxNGCC: " + src);
		String localName = src.substring(head.length(), end_of_open_tag);
		String tail = "</"+ngcc_prefix+":"+localName+">";
		if(!src.endsWith(tail)) throw new NGCCException("invalid annotation for RelaxNGCC: " + src);
		
		String content = src.substring(end_of_open_tag+1, src.length()-tail.length());
		return new NonXmlElement(NGCC_ELEMENT, localName, content);
	}
	
	private static String afterColon(String qname)
	{
		int n = qname.indexOf(':');
		if(n==-1) return qname;
		else return qname.substring(n+1);
	}
	private static String beforeColon(String qname)
	{
		int n = qname.indexOf(':');
		if(n==-1) return "";
		else return qname.substring(0,n);
	}
	
	public static void main(String[] args) throws Exception
	{
		NonXmlSyntax parser = new NonXmlSyntax(new InputStreamReader(new FileInputStream(args[0])));
		SchemaBuilderImpl sb = new SchemaBuilderImpl();
		parser.Input(sb);
		Element r = sb.finish(parser.getPreferredNamespace());
		//OutputStreamWriter w = new OutputStreamWriter(System.out);
		//r.dump(w, "utf-8");
		//w.flush();
		create(r);
	}
}
