/*
 * ScopeInfo.java
 *
 * Created on 2001/08/05, 14:43
 */

package relaxngcc.builder;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import java.io.PrintStream;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.automaton.Alphabet;
import relaxngcc.NGCCGrammar;
import relaxngcc.NGCCUtil;
import relaxngcc.Options;

/**
 * information about a scope
 */
public class ScopeInfo
{
	private NGCCGrammar _Grammar;
	public NGCCGrammar getGrammar() { return _Grammar; }
	
	private Set _AllStates;
	private Set _StartElement_States; //a set of states that have transition(s) with StartElement alphabet
	private Set _EndElement_States;   //a set of states that have transition(s) with EndElement alphabet
	private Set _Text_States;         //a set of states that have transition(s) with Typed-value alphabet
	private Set _Attribute_States;    //a set of states that have transition(s) with Attribute alphabet
	private Set _Ref_States;    //a set of states that have transition(s) with Attribute alphabet
	private Set _Acceptable_States;

	private Set _FollowAlphabet;
	private Set _ContainingScopeForFollowAlphabet;
	private Set _FirstAlphabet;
	private Set _ContainingScopeForFirstAlphabet;
	
    private Set _ChildScopes; //child lambda scopes
    
	private boolean _Dirty;
	private boolean _Root; //true if this ScopeInfo represents the content of <start> tag
	private Map _NSURItoStringConstant;
	
	private State _InitialState;
	private String _InitialCode;
    private boolean _IsLambda; //lambda scope or not
	private boolean _Nullable;
	private boolean _IsInline;
	private int _ThreadCount;

	public State getInitialState() { return _InitialState; }
	public boolean nullable() { return _Nullable; }	
	public boolean isLambda() { return _IsLambda; }
	public boolean isInline() { return _IsInline; }
    
    public void setNullable(boolean v) { _Nullable = v; }
	public void setInitialState(State s, String initialcode) { _InitialState = s; _InitialCode = initialcode; }
	public void setThreadCount(int n) { _ThreadCount = n; } 
	
    //simple attributes
	private String _Location;
	private String _NameForTargetLang; //java class name
	private String _PackageName; //java package name
    private String _Access; //java access identifier i.e. "private final"
	private String _Name; //scope name

	public String getNameForTargetLang() { return _NameForTargetLang; }
	public String getName() { return _Name; }
	public String getPackageName() { return _PackageName; }
	public String getLocation() { return _Location; }
	public int getStateCount() { return _AllStates.size(); }
	
	public Iterator iterateFirstAlphabets()  { return _FirstAlphabet.iterator(); }
	public Iterator iterateFollowAlphabets() { return _FollowAlphabet.iterator(); }
	public boolean isFollowAlphabet(Alphabet a) { return _FollowAlphabet.contains(a); }
	public boolean isFirstAlphabet(Alphabet a) { return _FirstAlphabet.contains(a); }
	
    public Iterator iterateChildScopes() { return _ChildScopes.iterator(); }
    
    public boolean containsFirstAttributeAlphabet()
    {
    	Iterator it = _FirstAlphabet.iterator();
    	while(it.hasNext())
    	{
    		Alphabet a = (Alphabet)it.next();
    		if(a.getType()==Alphabet.START_ATTRIBUTE) return true;
    	}
    	return false;
    }
    public boolean containsFollowAttributeAlphabet()
    {
    	Iterator it = _FollowAlphabet.iterator();
    	while(it.hasNext())
    	{
    		Alphabet a = (Alphabet)it.next();
    		if(a.getType()==Alphabet.START_ATTRIBUTE) return true;
    	}
    	return false;
    }
    
	//about header and body
	private String _HeaderSection = "";
	private String _Body = "";
	public void appendHeaderSection(String c) { _HeaderSection += c; }
	public void appendBody(String c) { _Body += c; }
	
	//type usage information. these flags affect the output of import statements
	private boolean _UsingBigInteger;
	private boolean _UsingCalendar;
	
	private class Alias
	{
		public final String name;
		public final String javatype;
		public final boolean isUserObject;
		public Alias(String n, String t, boolean user) { name=n; javatype=t; isUserObject=user; }
	}
	private Vector _Aliases;
	
	public ScopeInfo(NGCCGrammar g, int type, String location, boolean inline)
	{
		_Grammar = g;
		_IsLambda = (type==ScopeBuilder.TYPE_LAMBDA);
		_Root = (type==ScopeBuilder.TYPE_ROOT);
		_IsInline = inline;
		_Location = location;
		_Aliases = new Vector();
		_UsingBigInteger = false;
		_UsingCalendar = false;
	
		_AllStates = new TreeSet();
		_StartElement_States = new TreeSet();
		_EndElement_States = new TreeSet();
		_Text_States = new TreeSet();
		_Attribute_States = new TreeSet();
		_Ref_States = new TreeSet();
		_Acceptable_States = new TreeSet();
		
        _ChildScopes = new HashSet();
		_FollowAlphabet = new TreeSet();
        
		_NSURItoStringConstant = new TreeMap();
	}
	public void setClassNames(String name, String nameForTargetLang, String packageName, String access)
	{
		_Name = name;
		_NameForTargetLang = nameForTargetLang;
		_PackageName = packageName;
		_Access = access;
	}
	
	public void addNSURI(String nsuri)
	{
		if(_NSURItoStringConstant.containsKey(nsuri)) return;
		
		String result = "";
		if(nsuri.length()==0)
			result = "DEFAULT_NSURI";
		else
		{
			StringTokenizer tok = new StringTokenizer(nsuri, ":./%-~"); //loose check
			while(tok.hasMoreTokens())
			{
				String part = tok.nextToken();
				if(result.length()>0) result+="_"; //delimiter
				result += part.toUpperCase();
			}
		}
		_NSURItoStringConstant.put(nsuri, result);
	}
	
	public String getNSStringConstant(String uri)
	{
		Object o = _NSURItoStringConstant.get(uri);
		return (String)o;
	}
	
    public void addChildScope(ScopeInfo info) { _ChildScopes.add(info); }
    
	public Iterator iterateStatesHavingStartElementOrRef()
	{
		if(_Dirty) collectStates();
		TreeSet result = new TreeSet(_StartElement_States);
		result.addAll(_Ref_States);
		return result.iterator();
	}
	public Iterator iterateStatesHavingEndElement()
	{ if(_Dirty) collectStates(); return _EndElement_States.iterator(); }
	public Iterator iterateStatesHavingAttribute()
	{ if(_Dirty) collectStates(); return _Attribute_States.iterator(); }
	public Iterator iterateStatesHavingText()
	{ if(_Dirty) collectStates(); return _Text_States.iterator(); }
	public Iterator iterateAllStates()
	{ return _AllStates.iterator(); }
	public Iterator iterateStatesHavingRef()
	{ if(_Dirty) collectStates(); return _Ref_States.iterator(); }
	public Iterator iterateAcceptableStates()
	{ if(_Dirty) collectStates(); return _Acceptable_States.iterator(); }
	
	private void collectStates()
	{
		_StartElement_States.clear();
		_EndElement_States.clear();
		_Text_States.clear();
		_Attribute_States.clear();
		
		Iterator i = _AllStates.iterator();
		while(i.hasNext())
		{
			State state = (State)i.next();
			if(state.hasStartElementTransition())
				_StartElement_States.add(state);
			if(state.hasEndElementTransition())
				_EndElement_States.add(state);
			if(state.hasAttributeTransition())
				_Attribute_States.add(state);
			if(state.hasTextTransition())
				_Text_States.add(state);
			if(state.hasRefTransition())
				_Ref_States.add(state);
			if(state.isAcceptable())
				_Acceptable_States.add(state);
		}
		_Dirty = false;
	}
	
	public void addState(State state)
	{
		_AllStates.add(state);
		_Dirty = true;
	}
	
	public void addAlias(String name, String xsdtype)
	{
		String javatype = NGCCUtil.XSDTypeToJavaType(xsdtype);
		_Aliases.add(new Alias(name, javatype, false));
		if(!_UsingBigInteger && javatype.equals("BigInteger"))
			_UsingBigInteger = true;
		else if(!_UsingCalendar && javatype.equals("GregorianCalendar"))
			_UsingCalendar = true;
	}
	public void addUserDefinedAlias(String name, String classname)
	{
		_Aliases.add(new Alias(name, classname, true));
	}
	
	
	public void calcFirst_Step1()
	{
		_FirstAlphabet = new TreeSet();
		_ContainingScopeForFirstAlphabet = new HashSet();
		
		Iterator it = _InitialState.iterateTransitions();
		while(it.hasNext())
		{
			Transition tr = (Transition)it.next();
			Alphabet a = tr.getAlphabet();
			if(a.getType()==Alphabet.REF_BLOCK)
				_ContainingScopeForFirstAlphabet.add(_Grammar.getScopeInfoByName(a.getValue()));
			else
				_FirstAlphabet.add(a);
		}
	}
	public void calcFirst_Step2()
	{
		ScopeList list = new ScopeList(this);
		Iterator it = _ContainingScopeForFirstAlphabet.iterator();
		while(it.hasNext())
		{
			ScopeInfo neighbor = (ScopeInfo)it.next();
			_FirstAlphabet.addAll(neighbor.calcFirst_Step2(list));
		}
	}
	private Set calcFirst_Step2(ScopeList list)
	{
		ScopeList nextlist = new ScopeList(this, list);
		Iterator it = _ContainingScopeForFirstAlphabet.iterator();
		while(it.hasNext())
		{
			ScopeInfo neighbor = (ScopeInfo)it.next();
			if(!list.contains(neighbor))
				_FirstAlphabet.addAll(neighbor.calcFirst_Step2(nextlist));
		}
		return _FirstAlphabet;
	}
	
	public void checkFirstAlphabetAmbiguousity()
	{
		Iterator it = iterateStatesHavingRef();
		while(it.hasNext())
		{
			State s = (State)it.next();
			s.checkFirstAlphabetAmbiguousity();
		}
	}
	
	public void calcFollow_Step0()
	{
		_ContainingScopeForFollowAlphabet = new HashSet();
		if(_Dirty) collectStates();
	}
	
	public void calcFollow_Step1()
	{
		Iterator refs = _Ref_States.iterator();
		while(refs.hasNext())
		{
			State s = (State)refs.next();
			Iterator trs = s.iterateRefTransitions();
			while(trs.hasNext())
			{
				Transition tr = (Transition)trs.next();
				ScopeInfo target = _Grammar.getScopeInfoByName(tr.getAlphabet().getValue());
				State next = tr.nextState();
				if(next.isAcceptable())
				{
					_ContainingScopeForFollowAlphabet.add(target);
					target._ContainingScopeForFollowAlphabet.add(this);
				}
				
				Iterator next_trs = next.iterateTransitions();
				while(next_trs.hasNext())
				{
					Alphabet next_alphabet = ((Transition)next_trs.next()).getAlphabet();
					if(next_alphabet.getType()==Alphabet.REF_BLOCK)
						target._FollowAlphabet.addAll(_Grammar.getScopeInfoByName(next_alphabet.getValue())._FirstAlphabet);
					else
						target._FollowAlphabet.add(next_alphabet);
				}
			}
		}
	}
	
	public void calcFollow_Step2()
	{
		ScopeList list = new ScopeList(this);
		Iterator it = _ContainingScopeForFollowAlphabet.iterator();
		while(it.hasNext())
		{
			ScopeInfo neighbor = (ScopeInfo)it.next();
			_FollowAlphabet.addAll(neighbor.calcFollow_Step2(list));
		}
	}
	private Set calcFollow_Step2(ScopeList list)
	{
		ScopeList nextlist = new ScopeList(this, list);
		Iterator it = _ContainingScopeForFollowAlphabet.iterator();
		while(it.hasNext())
		{
			ScopeInfo neighbor = (ScopeInfo)it.next();
			if(!list.contains(neighbor))
				_FollowAlphabet.addAll(neighbor.calcFollow_Step2(nextlist));
		}
		return _FollowAlphabet;
	}
	
	public void checkFollowAlphabetAmbiguousity()
	{
		Iterator it = _AllStates.iterator();
		while(it.hasNext())
		{
			State s = (State)it.next();
			s.checkFollowAlphabetAmbiguousity();
		}
	}
	
	public void printHeaderSection(PrintStream output, Options options, String globalimport)
	{
        if(!_IsLambda)
        {
            //notice
            output.println("/* this file is generated by RelaxNGCC */");
            //package
            if(_PackageName.length()>0)
                output.println("package " + _PackageName + ";");
            //imports
            if(_UsingBigInteger)
                output.println("import java.math.BigInteger;");
            if(_UsingCalendar)
                output.println("import java.util.GregorianCalendar;");

            output.println("import org.xml.sax.SAXException;");
            output.println("import org.xml.sax.XMLReader;");
            if(options.style==Options.STYLE_MSV)
            {
                output.println("import relaxngcc.runtime.NGCCTypedContentHandler;");
                output.println("import com.sun.msv.datatype.xsd.XSDatatype;");
                output.println("import com.sun.msv.verifier.DocumentDeclaration;");
                output.println("import com.sun.msv.verifier.psvi.TypeDetector;");
                output.println("import com.sun.msv.verifier.util.ErrorHandlerImpl;");
                output.println("import com.sun.msv.driver.textui.DebugController;");
                output.println("import com.sun.msv.reader.util.GrammarLoader;");
            }
            else
            {
                output.println("import relaxngcc.runtime.NGCCPlainHandler;");
            }

            if(_Root)
            {
                output.println("import javax.xml.parsers.SAXParserFactory;");
                output.println("import javax.xml.parsers.ParserConfigurationException;");
            }

            output.println(globalimport);

            if(_HeaderSection.length()>0)
                output.println(_HeaderSection);
        }
		//class name
		if(_Access.length()>0) _Access += " ";
		output.println(_Access + "class " + _NameForTargetLang + " extends " + (options.style==Options.STYLE_MSV? "NGCCTypedContentHandler" : "NGCCPlainHandler") + " {");
		//NSURI constants
		Iterator uris = _NSURItoStringConstant.entrySet().iterator();
		while(uris.hasNext())
		{
			Map.Entry e = (Map.Entry)uris.next();
			output.println("public static final String " + (String)e.getValue() + " = \"" + (String)e.getKey() + "\";");
		}
		//data member
		output.println("private int _ngcc_current_state;");
		if(_ThreadCount>0)
			output.println("private int[] _ngcc_threaded_state;");
		for(int i=0; i<_Aliases.size(); i++)
		{
			Alias a = (Alias)_Aliases.get(i);
			if(options.style==Options.STYLE_PLAIN_SAX && !a.isUserObject)
				output.println("private String " + a.name + ";");
			else
				output.println("private " + a.javatype + " " + a.name + ";");
		}
		//constructor
		if(options.style==Options.STYLE_MSV)
			output.println("public " + _NameForTargetLang + "(TypeDetector reader, NGCCTypedContentHandler parent) {");
		else
			output.println("public " + _NameForTargetLang + "(XMLReader reader, NGCCPlainHandler parent) {");
		output.println("\tsuper(reader, parent);");
		output.println("}");
		output.println("protected void initState() { _ngcc_current_state=" + _InitialState.getIndex() + "; ");
		if(_InitialCode!=null) output.println(_InitialCode);
		if(_ThreadCount>0)
			output.println("_ngcc_threaded_state = new int[" + _ThreadCount + "]; }");
		else
			output.println("}");
		
		//simple entry point
		if(_Root)
		{
			if(options.style==Options.STYLE_MSV)
			{
				output.println("public static XMLReader getPreparedReader(SAXParserFactory f, DocumentDeclaration g) throws ParserConfigurationException, SAXException {");
				output.println("\tXMLReader r = f.newSAXParser().getXMLReader();");
				output.println("\tTypeDetector v = new TypeDetector(g, new ErrorHandlerImpl());");
				output.println("\tr.setContentHandler(v);");
				output.println("\tv.setContentHandler(new " + _NameForTargetLang + "(v, null));");
				output.println("\treturn r;");
				output.println("}");
				output.println("public static void main(String[] args) throws Exception {");
				output.println("\tif(args.length!=2) { System.err.println(\"usage: " + _NameForTargetLang + " <grammarfile> <instancefile>\"); return; }");
				output.println("\tSAXParserFactory f = SAXParserFactory.newInstance();");
				output.println("\tf.setNamespaceAware(true);");
				output.println("\tGrammarLoader loader = new GrammarLoader();");
				output.println("\tloader.setController( new DebugController(false,false) );");
				output.println("\tDocumentDeclaration g = loader.loadVGM( args[0] );");
				output.println("\tif(g==null) { System.err.println(\"Failed to load grammar.\"); return; }");
				output.println("\tXMLReader r = getPreparedReader(f,g);");
				output.println("\tr.parse(args[1]);");
				output.println("}");
			}
			else
			{
				output.println("public static XMLReader getPreparedReader(SAXParserFactory f) throws ParserConfigurationException, SAXException {");
				output.println("\tXMLReader r = f.newSAXParser().getXMLReader();");
				output.println("\tr.setContentHandler(new " + _NameForTargetLang + "(r, null));");
				output.println("\treturn r;");
				output.println("}");
				output.println("public static void main(String[] args) throws Exception {");
				output.println("\tif(args.length!=1) { System.err.println(\"usage: " + _NameForTargetLang + " <instancefile>\"); return; }");
				output.println("\tSAXParserFactory f = SAXParserFactory.newInstance();");
				output.println("\tf.setNamespaceAware(true);");
				output.println("\tXMLReader r = getPreparedReader(f);");
				if(options.style==Options.STYLE_TYPED_SAX) output.println("\tDataTypes.init();");
				output.println("\tr.parse(args[0]);");
				output.println("}");
			}
		}
        
	}

	public void printTailSection(String globalbody, PrintStream output)
	{
        if(!_IsLambda)
        {
            if(_Body.length()>0)
                output.println(_Body);
            output.println(globalbody);
        }
		output.println("}"); //end of class
	}
	
	public void dump(PrintStream strm)
	{
		strm.println("Scope " + _Name);
		strm.print(" FIRST: ");
		Iterator it = _FirstAlphabet.iterator();
		while(it.hasNext())
		{
			strm.print(((Alphabet)it.next()).toString());
			strm.print(", ");
		}
		strm.println();
		
		strm.print(" FOLLOW: ");
		it = _FollowAlphabet.iterator();
		while(it.hasNext())
		{
			strm.print(((Alphabet)it.next()).toString());
			strm.print(", ");
		}
		strm.println();
	}
}
