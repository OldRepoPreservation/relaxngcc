/*
 * NGCCUtil.java
 *
 * Created on 2001/08/11, 10:32
 */

package relaxngcc;
import java.io.File;

public class NGCCUtil
{
	public static String combineURL(String base, String relative)
	{
		try
		{
			//As for common case, 'base' and 'relative' are local file names.
			File parent = new File(base).getParentFile();
			File result;
			if(parent==null)
				result = new File(relative);
			else
				result = new File(parent, relative);
			return result.getCanonicalPath();
		}
		catch(java.io.IOException e) { return null; } //!!後でましなハンドリングへ変更すること
	}
	
	public static String XSDTypeToJavaType(String xsd)
	{
		if(xsd.equals("float")) return "Float";
		if(xsd.equals("double")) return "Double";
		if(xsd.equals("boolean")) return "Boolean";
		if(xsd.equals("byte")) return "Byte";
		if(xsd.equals("short")) return "Short";
		if(xsd.equals("int")) return "Integer";
		if(xsd.equals("long")) return "Long";
		if(xsd.equals("unsignedByte")) return "Short";
		if(xsd.equals("unsignedShort")) return "Integer";
		if(xsd.equals("unsignedInt")) return "Long";
		if(xsd.equals("unsignedLong")) return "BigInteger";
		if(xsd.equals("integer") || xsd.endsWith("Integer")) return "BigInteger";
		if(xsd.equals("base64Binary") || xsd.equals("hexBinary")) return "byte[]";
		if(xsd.equals("date") || xsd.equals("time")
		   || xsd.equals("dateTime") || xsd.equals("gYear") || xsd.equals("gYearMonth")
		   || xsd.equals("gMonth") || xsd.equals("gMonthDay") || xsd.equals("gDay")) return "GregorianCalendar";
		return "String"; //if none of previous types match
	}
	
	public static String getFileNameFromPath(String path)
	{
		int n = path.indexOf('/');
		if(n!=-1) return path.substring(n+1);
		n = path.indexOf('\\');
		if(n!=-1) return path.substring(n+1);
		return path;
	}
}
