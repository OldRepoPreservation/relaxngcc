/*
 * Grammar.java
 *
 * Created on 2001/08/11, 10:14
 */

package relaxngcc;
import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.NGCCNodeList;
import relaxngcc.runtime.NGCCRuntime;
import relaxngcc.util.ConcatIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import relaxngcc.builder.FirstFollow;
import relaxngcc.builder.ScopeBuilder;
import relaxngcc.builder.ScopeInfo;
import relaxngcc.builder.CodeWriter;
import relaxngcc.automaton.State;

/**
 * read grammar content via DOM interface
 */
public class NGCCGrammar
{
	public static final String NGCC_NSURI = "http://www.xml.gr.jp/xmlns/relaxngcc";
	public static final String RELAXNG_NSURI_09 = "http://relaxng.org/ns/structure/0.9";
	public static final String RELAXNG_NSURI_10 = "http://relaxng.org/ns/structure/1.0";
	public static String RELAXNG_NSURI;

	private Map _Scopes; //a map from scope names to ScopeBuilder objects
	private Map _LambdaScopes;
	private ScopeBuilder _Root;
	private Vector _DataTypes;
	private String _Package;
	private String _GlobalBody;
	private String _GlobalImport;
	private String _DefaultNSURI;
	private Options _Options;
    
    /**
     * Type name of the runtime.
     * By default it is relaxngcc.runtime.NGCCRuntime,
     * but the application can implement a derived class
     * and use it instead of the default one.
     */
    private String runtimeType;
    /**
     * Gets the class name of the runtime type
     * alone without the package name.
     */
    public String getRuntimeTypeShortName() {
        int idx = runtimeType.lastIndexOf('.');
        if(idx<0)   return runtimeType;
        else        return runtimeType.substring(idx+1);
    }
    public String getRuntimeTypeFullName() {
        return runtimeType;
    }
	
	private class GrammarLoadingContext
	{
		public String location;
		public Set excluding_names;
		public GrammarLoadingContext(String l, Set e) { location=l; excluding_names=e; }
	}
	
    public NGCCGrammar(Options o) throws NGCCException
	{
		_Options = o;
		NGCCElement e = RelaxNGCC.readNGCCGrammar(o, o.sourcefile);
		_Scopes = new TreeMap();
		_LambdaScopes = new TreeMap();
		_DataTypes = new Vector();
		_Package = e.attributeNGCC("package","");
        runtimeType = e.attributeNGCC("runtime-type",null);
        if(runtimeType==null) {
            if(o.usePrivateRuntime) {
                if(_Package.length()!=0)
                    runtimeType = _Package + ".NGCCRuntime";
                else
                    runtimeType = "NGCCRuntime";
            } else {
                runtimeType = "relaxngcc.runtime.NGCCRuntime";
            }
        }
		_DefaultNSURI = e.getAttribute("ns");
		_GlobalBody = "";
		_GlobalImport = "";
		processElement(e, new GrammarLoadingContext(o.sourcefile, new TreeSet()));
    }

	private void processElement(NGCCElement e, GrammarLoadingContext ctx) throws NGCCException
	{
		NGCCNodeList nl = e.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			NGCCElement c = nl.item(i);
			if(c==null) continue; //ignoring comment, PI, etc.
			
			String name = c.getLocalName();
			if(c.getNamespaceURI().equals(RELAXNG_NSURI))
			{
				if(name.equals("define"))
					processDefine(c, ctx);
				else if(name.equals("start"))
					processStart(c, ctx);
				else if(name.equals("include"))
					processInclude(c, ctx);
				else if(name.equals("div"))
					processElement(c, ctx);
			}
			else if(c.getNamespaceURI().equals(NGCC_NSURI))
			{	
				if(name.equals("java-body")) _GlobalBody += c.getFullText();
				else if(name.equals("java-import")) _GlobalImport += c.getFullText();
			}
		}
	}
	
	private void processDefine(NGCCElement e, GrammarLoadingContext ctx) throws NGCCException
	{
		if(ctx.excluding_names.contains(e.getAttribute("name"))) return; //ignore this block
		ScopeBuilder b = ScopeBuilder.create(this, ctx.location, e);
		
		ScopeBuilder existing = (ScopeBuilder)_Scopes.get(b.getName());
		if(existing!=null) //when a scope with identical name is already defined
		{
			String combine = e.getAttribute("combine");
			if(combine.equals("interleave") || combine.equals("choice"))
			{
				existing.extend(e);
			}
		}
		else //first scope
			_Scopes.put(b.getName(), b);
	}
	private void processStart(NGCCElement e, GrammarLoadingContext ctx) throws NGCCException
	{
		ScopeBuilder b = ScopeBuilder.createAsRoot(this, ctx.location, e);
		_Scopes.put(b.getName(), b);
		_Root = b;
	}
	private void processInclude(NGCCElement e, GrammarLoadingContext ctx) throws NGCCException
	{
		Set excluding_names = new TreeSet(ctx.excluding_names);
		NGCCNodeList nl = e.getChildNodes(); //e.getElementsByTagNameNS(RELAXNG_NSURI, "define");
		for(int i=0; i<nl.getLength(); i++)
		{
			NGCCElement def = nl.item(i);
			if(def==null || !def.getLocalName().equals("define")) continue;
			processDefine(def, ctx);
			excluding_names.add(def.getAttribute("name"));
		}
		
		String location = NGCCUtil.combineURL(ctx.location, e.getAttribute("href"));
		_Options.from_include = true;
		processElement(RelaxNGCC.readNGCCGrammar(_Options, location), new GrammarLoadingContext(location, excluding_names));
	}
	
	public MetaDataType addDataType(NGCCElement e)
	{
		MetaDataType mdt = new MetaDataType(e, _DataTypes.size());
		if(!mdt.hasFacets())
		{
			Iterator it = _DataTypes.iterator();
			while(it.hasNext())
			{
				MetaDataType t = (MetaDataType)it.next();
				if(!t.hasFacets() && t.getXSTypeName().equals(mdt.getXSTypeName())) return t; //hits in cache
			}
		}
		_DataTypes.add(mdt);
		return mdt;
	}
    /** Iterates all ScopeInfos, including lambda scopes. */
    public Iterator iterateAllScopeBuilder() {
        return new ConcatIterator(
            _Scopes.values().iterator(), _LambdaScopes.values().iterator() );
    }
    
	public ScopeBuilder getScopeBuilderByName(String name) { return (ScopeBuilder)_Scopes.get(name); }
	public ScopeInfo getScopeInfoByName(String name) { return getScopeBuilderByName(name).getScopeInfo(); }
	
    public String createLambdaName() { return "_ngcc_lambda" + _Scopes.size(); }
    public void addLambdaScope(ScopeBuilder b)
    {
        _LambdaScopes.put(b.getName(), b);
    }
    
	public void output() throws IOException
	{
		//step1 datatypes
		if(_Options.style==Options.STYLE_TYPED_SAX && _DataTypes.size() > 0)
		{
			PrintStream s = new PrintStream(new FileOutputStream(
                new File(_Options.targetdir, "DataTypes.java")));
			printDataTypes(s);
		}
		//step2 scopes
		Iterator it = _Scopes.values().iterator();
		while(it.hasNext()) {
			ScopeInfo si = ((ScopeBuilder)it.next()).getScopeInfo();
			if(!si.isLambda() && !si.isInline()) {
				CodeWriter w = new CodeWriter(this, si, _Options);
                File f = new File(_Options.targetdir, si.getNameForTargetLang() + ".java");
                f.delete();
                PrintStream out = new PrintStream(new FileOutputStream(f));
				w.output(out);
                out.close();
                f.setReadOnly();
			}
		}
        // copy runtime code if necessary
        if(_Options.usePrivateRuntime) {
            copyResourceAsFile("NGCCHandler.java");
            copyResourceAsFile("AttributesImpl.java");
            copyResourceAsFile("NGCCRuntime.java");
        }
	}
    
    /**
     * Copies a resource file to the target directory.
     */
    private void copyResourceAsFile( String file ) throws IOException {
        File out = new File(_Options.targetdir,file);
        
//        if(!out.exists()) {
//            System.out.println(file+" doesn't exist. Generating.");
            BufferedReader in = new BufferedReader(
                new InputStreamReader(NGCCRuntime.class.getResourceAsStream(file)));
                
            PrintWriter os = new PrintWriter(new FileWriter(out));
            byte[] buf = new byte[256];
            
            String s;
            while((s=in.readLine())!=null) {
                if(s.startsWith("package ")) {
                    if(getPackageName().length()!=0)
                        s = "package "+getPackageName()+";";
                    else
                        s="";
                }
                os.println(s);
            }
            
            in.close();
            os.close();
//        }
    }
	
	public void buildAutomaton() throws NGCCException
	{
		Iterator it = _Scopes.values().iterator();
		while(it.hasNext())
		{
			((ScopeBuilder)it.next()).determineNullable();
		}
		it = _Scopes.values().iterator();
		while(it.hasNext())
		{
			((ScopeBuilder)it.next()).buildAutomaton();
		}
		_Scopes.putAll(_LambdaScopes);
	}
    
    /** Computes FIRST and FOLLOW and updates ScopeInfo appropriately. */
    public void calcFirstAndFollow() {
        FirstFollow ff = new FirstFollow(this);
        Iterator itr = iterateAllScopeBuilder();
        while(itr.hasNext()) {
            ScopeInfo si = ((ScopeBuilder)itr.next()).getScopeInfo();
            si.setFirstAndFollow( ff.getFirst(si), ff.getFollow(si) );
        }
    }
	
	//for debug
	public void dump(PrintStream strm)
	{
		Iterator it = _Scopes.values().iterator();
		while(it.hasNext())
		{
			ScopeInfo sci = ((ScopeBuilder)it.next()).getScopeInfo();
			sci.dump(strm);
		}
	}
    
    /** generates automaton gif files. */
    public void dumpAutomata(File outDir) throws IOException, InterruptedException {
        Iterator it = _Scopes.values().iterator();
        while(it.hasNext()) {
            ScopeInfo sci = ((ScopeBuilder)it.next()).getScopeInfo();
            sci.dumpAutomaton(new File(outDir,sci.getNameForTargetLang()+".gif"));
        }
    }
	
	//outputs data type definition. This is for only typed-sax mode.
	private void printDataTypes(PrintStream output)
	{
		output.println("/* this file is generated by RelaxNGCC */");
		if(_Package.length()>0)	output.println("package " + _Package + ";");
		output.println("import com.sun.msv.datatype.xsd.XSDatatype;");
		output.println("import com.sun.msv.datatype.xsd.DatatypeFactory;");
		output.println("import com.sun.msv.datatype.xsd.TypeIncubator;");
		output.println("import org.relaxng.datatype.DatatypeException;");
		output.println();
		output.println("class DataTypes {");
		output.println("public static XSDatatype[] dt;");
		output.println("public static void init() throws DatatypeException {");
		output.println("\tTypeIncubator ti;");
		output.println("\tdt = new XSDatatype[" + _DataTypes.size() + "];");
		for(int i=0; i<_DataTypes.size(); i++)
		{
			MetaDataType mdt = (MetaDataType)_DataTypes.get(i);
			mdt.printDataTypeConstructionCode("dt["+i+"]", output);
		}
		output.println("}");
		output.println("}");
	}
	
    /**
     * Gets the package name to store generated classes.
     * "" is used to denote the root package.
     * 
     * @return      non-null vaild string.
     */
	public String getPackageName() { return _Package; }
	public String getGlobalBody() { return _GlobalBody; }
	public String getGlobalImport() { return _GlobalImport; }
	public String getDefaultNSURI() { return _DefaultNSURI; }
}
