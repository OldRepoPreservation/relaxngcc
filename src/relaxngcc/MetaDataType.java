/*
 * MetaDataType.java
 *
 * Created on 2001/08/04, 21:52
 */

package relaxngcc;
import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.NGCCNodeList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.PrintStream;
import relaxngcc.NGCCUtil;

/**
 * MetaDataType has the ability to generate a code that verifies data types described in original grammar.
 * At the present, only XML Schema Part2 is supported as the data type library for RelaxNGCC.
 */
public class MetaDataType
{
	private String _BaseName;
	private Map _Facets;
	private int _Index;
	
    public MetaDataType(NGCCElement e, int index)
	{
		_Index = index;
		_BaseName = e.getAttribute("type");
		_Facets = new TreeMap();
		NGCCNodeList nl = e.getChildNodes(); //e.getElementsByTagName("param");
		for(int i=0; i<nl.getLength(); i++)
		{
			NGCCElement facet = nl.item(i);
			if(facet==null) continue;
			_Facets.put(facet.getAttribute("name"), facet.getFullText());
		}
	}
	//for special type
	private MetaDataType(String typename) { _BaseName=typename; }
	
	public static MetaDataType STRING = new MetaDataType("string");
	
	public int getIndex() { return _Index; }
	
	public void printDataTypeConstructionCode(String instance_name, PrintStream out)
	{
		if(_Facets.size() > 0)
		{
			out.println("\tti=new TypeIncubator(" + typeClassName(_BaseName) + ");");
			Iterator i = _Facets.entrySet().iterator();
			while(i.hasNext())
			{
				Map.Entry e = (Map.Entry)i.next();
				out.println("\tti.addFacet(\"" + (String)e.getKey() + "\",\"" + (String)e.getValue() + "\",false,null);");
			}
			out.println("\t" + instance_name + "=ti.derive(null);");
		}
		else //easy case
			out.println("\t" + instance_name + "=" + typeClassName(_BaseName) + ";");
	}
	
	public String getXSTypeName() { return _BaseName; }
	public String getJavaTypeName() { return NGCCUtil.XSDTypeToJavaType(_BaseName); }
	
	public boolean hasFacets() { return !_Facets.isEmpty(); }
	
	private static String typeClassName(String base)
	{
		return "DatatypeFactory.getTypeByName(\"" + base + "\")";
	}

}
