/*
 * ScopeInfo.java
 *
 * Created on 2001/08/05, 14:43
 */

package relaxngcc.builder;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import relaxngcc.NGCCGrammar;
import relaxngcc.NGCCUtil;
import relaxngcc.Options;
import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.javabody.JavaBodyParser;
import relaxngcc.util.SelectiveIterator;

/**
 * information about a scope
 */
public final class ScopeInfo
{
	private NGCCGrammar _Grammar;
	public NGCCGrammar getGrammar() { return _Grammar; }
	
	private Set _AllStates;

	private Set _FollowAlphabet;
	private Set _FirstAlphabet;
    public void setFirstAndFollow( Set first, Set follow ) {
        _FirstAlphabet = first;
        _FollowAlphabet = follow;
    }
	
    private Set _ChildScopes; //child lambda scopes
    
	private boolean _Root; //true if this ScopeInfo represents the content of <start> tag
	private Map _NSURItoStringConstant;
	
	private State _InitialState;
    /**
     * Code that gets executed at the beginning of this scope.
     */
    // TODO: initial action is not used!
	private Action _InitialAction;
    private boolean _IsLambda; //lambda scope or not
	private boolean _Nullable;
	private boolean _IsInline;
	private int _ThreadCount;

	public State getInitialState() { return _InitialState; }
	public boolean isNullable() { return _Nullable; }	
	public boolean isLambda() { return _IsLambda; }
	public boolean isInline() { return _IsInline; }
    
    public void setNullable(boolean v) { _Nullable = v; }
	public void setInitialState(State s, Action initAction) {
        _InitialState = s;
        _InitialAction = initAction;
    }
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
	
    
    /**
     * Parameters to the constructor. Array of Aliases.
     */
    private Alias[] constructorParams;
    
    /**
     * The name of the variable that will be "returned" to the
     * parent scope.
     * 
     * The parent scope will access this variable via the alias.
     * Defaults to "this", meaning that the class itself will be
     * returned.
     */
    private String returnVariable = "this";
    public String getReturnVariable() { return returnVariable; }
    
    /**
     * The type of the <code>returnVariable</code> field.
     * Ideally, one could figure this out by parsing the Java code.
     * For now, we will ask the user to specify this value.
     * 
     * If null, the type of this class will be used as the default value.
     */
    private String returnType = null;
    public String getReturnType() {
        if(returnType!=null)    return returnType;
        
        if(_PackageName==null)  return _NameForTargetLang;
        else    return _PackageName+'.'+_NameForTargetLang;
    }
    
    
    private class AlphabetIterator extends SelectiveIterator {
        AlphabetIterator( Iterator base, int _typeMask ) {
            super(base);
            this.typeMask=_typeMask;
        }
        private final int typeMask;
        protected boolean filter( Object o ) {
            return (((Alphabet)o).getType()&typeMask)!=0;
        }
    }
    
	public Iterator iterateFirstAlphabets()  { return _FirstAlphabet.iterator(); }
    /** Iterates FIRST alphabets that match the given type mask. */
	public Iterator iterateFirstAlphabets( int typeMask ) {
        return new AlphabetIterator(iterateFirstAlphabets(),typeMask);
    }
    
    public Iterator iterateFollowAlphabets() { return _FollowAlphabet.iterator(); }
    /** Iterates FOLLOW alphabets that match the given type mask. */
    public Iterator iterateFollowAlphabets( int typeMask ) {
        return new AlphabetIterator(iterateFollowAlphabets(),typeMask);
    }
    
    
	public boolean isFollowAlphabet(Alphabet a) { return _FollowAlphabet.contains(a); }
	public boolean isFirstAlphabet(Alphabet a) { return _FirstAlphabet.contains(a); }
    
    
    public Iterator iterateChildScopes() { return _ChildScopes.iterator(); }
    
    /**
     * Returns true if the FIRST alphabets includes alphabets
     * of given types.
     */
    public boolean hasFirstAlphabet(final int type) {
        Iterator itr = new SelectiveIterator(_FirstAlphabet.iterator()) {
            public boolean filter( Object o ) {
                return ((Alphabet)o).getType()==type;
            }
        };
        return itr.hasNext();
    }
    
    public boolean containsFollowAttributeAlphabet() {
        return iterateFollowAlphabets(Alphabet.ENTER_ATTRIBUTE).hasNext();
    }
    
    /**
     * Makes the automaton smaller.
     * 
     * In actuality, this method only removes unreachable states.
     */
    public void minimizeStates() {
        Stack queue = new Stack();
        Set reachable = new HashSet();
        
        queue.push(getInitialState());
        reachable.add(getInitialState());
        
        while(!queue.isEmpty()) {
            State s = (State)queue.pop();
            Iterator itr = s.iterateTransitions();
            while(itr.hasNext()) {
                Transition t = (Transition)itr.next();
                
                if(reachable.add(t.nextState()))
                    queue.push(t.nextState());
            }
        }
        
        _AllStates.retainAll(reachable);
    }
    
	//about header and body
	private String _HeaderSection = "";
	public void appendHeaderSection(String c) { _HeaderSection += c; }
    
    private String _Body = "";

    /** Name of fields defined in &lt;cc:java-body>. */
    private final Set userDefinedFields = new HashSet();
    
    /** Appends new &lt;java-body>. */
    public void appendBody(String c) {
        _Body += c;
        
        JavaBodyParser p = new JavaBodyParser(new StringReader(c));
        
        try {
            p.JavaBody();       // parse the text
        } catch( relaxngcc.javabody.ParseException e ) {
            // TODO: report error location and such.
            System.err.println("[Warning] unable to parse <java-body>");
        }
        
        userDefinedFields.addAll(p.fields);
    }
	
	//type usage information. these flags affect the output of import statements
	private boolean _UsingBigInteger;
	private boolean _UsingCalendar;
    
    /** All actions in this scope. */
    private final Vector actions = new Vector();
    
    /** Creates a new Action object inside this scope. */
    public Action createAction( String code ) {
        Action a = new Action(code);
        actions.add(a);
        return a;
    }
    public Action createAction( StringBuffer code ) {
        return createAction(code.toString());
    }
    
    /**
     * User-defined code fragment.
     */
    public final class Action {
        Action( String _codeFragment ) {
            this.codeFragment = _codeFragment;
            this.uniqueId = actionIdGen++;
        }
        
        /** A code fragment that the user wrote. */
        private final String codeFragment;
        public String getCodeFragment() { return codeFragment; }
        
        /** Gets the code to invoke this action. */
        public String invoke() { return "action"+uniqueId+"();"; }
        
        /** ID number that uniquely identifies this fragment. */
        private final int uniqueId;
        public int getUniqueId() { return uniqueId; }
        
        /** Generates the action function. */
        void generate( PrintStream out ) {
            out.println("void action"+uniqueId+"() throws SAXException {");
            out.println(codeFragment);
            out.println("}");
        }
    }
    
    /** used to generate unique IDs for Actions. */
    private int actionIdGen = 0;
	
    
	private class Alias
	{
		public final String name;
		public final String javatype;
		public final boolean isUserObject;
		public Alias(String n, String t, boolean user) { name=n; javatype=t; isUserObject=user; }
	}
    /** All the aliases indexed by their names. */
	private final Map aliases = new TreeMap();
	
	public ScopeInfo(NGCCGrammar g, int type, String location, boolean inline)
	{
		_Grammar = g;
		_IsLambda = (type==ScopeBuilder.TYPE_LAMBDA);
		_Root = (type==ScopeBuilder.TYPE_ROOT);
		_IsInline = inline;
		_Location = location;
		_UsingBigInteger = false;
		_UsingCalendar = false;
	
		_AllStates = new TreeSet();
		
        _ChildScopes = new HashSet();
		_FollowAlphabet = new TreeSet();
        
		_NSURItoStringConstant = new TreeMap();
	}
    
    /**
     * Sets various information about this scope.
     */
	public void setParameters(
        String name, String nameForTargetLang, String packageName, String access,
        String returnType, String returnValue,
        String params)
	{
		_Name = name;
		_NameForTargetLang = nameForTargetLang;
		_PackageName = packageName;
		_Access = access;
        
        this.returnType = returnType;
        this.returnVariable = returnValue;
        
        Vector vec = new Vector();
        // parse constructor parameters
        if(params!=null) {
            StringTokenizer tokens = new StringTokenizer(params,",");
            while(tokens.hasMoreTokens()) {
                // (type,name) pair.
                String pair = tokens.nextToken();
                int idx = pair.indexOf(' ');
                String vartype = pair.substring(0,idx).trim();
                String varname = pair.substring(idx+1).trim();
                
                vec.add(
                    addUserDefinedAlias(varname,vartype));
            }
        }
        constructorParams = (Alias[])vec.toArray(new Alias[vec.size()]);
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
    
    
    /**
     * Iterates states that have transitions with one of specified
     * alphabets.
     */
	public Iterator iterateStatesHaving( final int alphabetTypes ) {
        return new SelectiveIterator(iterateAllStates()) {
            protected boolean filter( Object o ) {
                return ((State)o).hasTransition(alphabetTypes);
            }
        };
    }
    
	public Iterator iterateAcceptableStates() {
        return new SelectiveIterator(_AllStates.iterator()) {
            protected boolean filter( Object o ) {
                return ((State)o).isAcceptable();
            }
        };
    }
    public Iterator iterateAllStates() {
        return _AllStates.iterator();
    }
    
    
	
	public void addState(State state) {
		_AllStates.add(state);
	}
	
	public Alias addAlias(String name, String xsdtype)
	{
        String javatype = NGCCUtil.XSDTypeToJavaType(xsdtype);
		if(!_UsingBigInteger && javatype.equals("BigInteger"))
			_UsingBigInteger = true;
		else if(!_UsingCalendar && javatype.equals("GregorianCalendar"))
			_UsingCalendar = true;

        Alias a = new Alias(name, javatype, false);
        aliases.put(name,a);
        return a;
	}
	public Alias addUserDefinedAlias(String name, String classname)
	{
        Alias a = new Alias(name, classname, true);
		aliases.put(name,a);
        return a;
	}
	
    
    
	
	public void checkFirstAlphabetAmbiguousity()
	{
		Iterator it = iterateStatesHaving(Alphabet.REF_BLOCK);
		while(it.hasNext())
		{
			State s = (State)it.next();
			s.checkFirstAlphabetAmbiguousity();
		}
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
	
    /**
     * Writes the beginning of the class to the specified output.
     */
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
            output.println("import relaxngcc.runtime.NGCCHandler;");
            output.println("import "+_Grammar.getRuntimeTypeFullName()+";");

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
		output.println(_Access + "class " + _NameForTargetLang + " extends NGCCHandler {");
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
        
        // aliases
        Iterator itr = aliases.values().iterator();
		while(itr.hasNext()) {
			Alias a = (Alias)itr.next();
            
            // if the alias is already declared explicitly by the <java-body>,
            // don't write it again.
            if(userDefinedFields.contains(a.name))
                continue;
                
			if(options.style==Options.STYLE_PLAIN_SAX && !a.isUserObject)
				output.println("private String " + a.name + ";");
			else
				output.println("private " + a.javatype + " " + a.name + ";");
		}
        
        String argList; // constructor arguments
        String argAssign;   // constructor aguments assignments
        String argParam;
        {// build up constructor arguments
            StringBuffer buf = new StringBuffer();
            for( int i=0; i<constructorParams.length; i++ ) {
                buf.append(',');
                buf.append(constructorParams[i].javatype);
                buf.append(" _");
                buf.append(constructorParams[i].name);
            }
            argList = buf.toString();
            
            buf = new StringBuffer();
            for( int i=0; i<constructorParams.length; i++ )
                buf.append(MessageFormat.format("this.{0}=_{0};\n",
                    new Object[]{constructorParams[i].name}));
            argAssign = buf.toString();

            buf = new StringBuffer();
            for( int i=0; i<constructorParams.length; i++ ) {
                buf.append(",_");
                buf.append(constructorParams[i].name);
            }
            argParam = buf.toString();
        }
        
		//constructor
		output.println(MessageFormat.format(
            "public {0}(NGCCHandler parent, {1} _runtime, int cookie {2} ) '{'\n"+
            "    super(parent,cookie);\n"+
            "    this.runtime = _runtime;\n"+
            "    {3}",
            new Object[]{
                getNameForTargetLang(),
                _Grammar.getRuntimeTypeShortName(),
                argList, argAssign }
        ));
        
        output.println("\t_ngcc_current_state=" + _InitialState.getIndex() + ";");
        if(_ThreadCount>0)
            output.println("_ngcc_threaded_state = new int[" + _ThreadCount + "];");
		output.println("}");

        output.println(MessageFormat.format(
            "public {0}( {1} _runtime {2} ) '{'\n"+
            "    this(null,_runtime,-1{3});\n"+
            "'}'",
            new Object[]{
                getNameForTargetLang(),
                _Grammar.getRuntimeTypeShortName(),
                argList, argParam }
        ));

        output.println(MessageFormat.format(
            "    protected final {0} runtime;\n"+
            "    public final relaxngcc.runtime.NGCCRuntime getRuntime() '{' return runtime; '}'",
            new Object[]{ _Grammar.getRuntimeTypeShortName() }
        ));
		
        // action functions
        for( int i=0; i<actions.size(); i++ )
            ((Action)actions.get(i)).generate(output);
        
		//simple entry point
		if(_Root)
		{
            if(options.style==Options.STYLE_MSV)
            {
                output.println("public static XMLReader getPreparedReader(SAXParserFactory f, DocumentDeclaration g) throws ParserConfigurationException, SAXException {");
                output.println("\tXMLReader r = f.newSAXParser().getXMLReader();");
                output.println("\tTypeDetector v = new TypeDetector(g, new ErrorHandlerImpl());");
                output.println("\tr.setContentHandler(v);");
                output.println("\tv.setContentHandler(new " + _NameForTargetLang + "(null));");
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
            // removed because this won't work well with
            // custom NGCCRuntime, which we don't know how to instanciate
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
    
    
    
    
    
    /** Gets the display name of a state. */
    private String getStateName( State s ) {
        StringBuffer buf = new StringBuffer();
        buf.append('"');
        if(s==getInitialState()) {
            buf.append("init(");
            buf.append(s.getIndex());
            buf.append(")");
        } else {
            buf.append("s");
            buf.append(s.getIndex());
        }
        if(s.getListMode()==State.LISTMODE_ON)
            buf.append('*');
            
        buf.append(buildActionList('+',s.getActionsOnExit()));
        buf.append('"');
        return buf.toString();
    }
    
    /** Gets the hue of the color for an alphabet. */
    private static String getColor( Alphabet a ) {
        // H S V
        double d;
        switch(a.getType()) {
        case Alphabet.ENTER_ELEMENT:     return "0";
        case Alphabet.LEAVE_ELEMENT:     return "0.16";
        case Alphabet.ENTER_ATTRIBUTE:   return "0.32";
        case Alphabet.LEAVE_ATTRIBUTE:   return "0.48";
        case Alphabet.REF_BLOCK:         return "0.64";
        case Alphabet.DATA_TEXT:
        case Alphabet.VALUE_TEXT:        return "0.80";
        default:
            throw new Error(); // assertion failed
        }
    }
    
    /**
     * Writes the automaton by using
     * <a href="http://www.research.att.com/sw/tools/graphviz/">GraphViz</a>.
     */
    public void dumpAutomaton( File target ) throws IOException, InterruptedException {
        
        System.err.println("generating a graph to "+target.getPath());
        
//        Process proc = Runtime.getRuntime().exec("dot");
        Process proc = Runtime.getRuntime().exec(
            new String[]{"dot","-Tgif","-o",target.getPath()});
        PrintWriter out = new PrintWriter(
            new BufferedOutputStream(proc.getOutputStream()));
    
        out.println("digraph G {");
        out.println("node [shape=\"circle\"];");

        Iterator itr = iterateAllStates();
        while( itr.hasNext() ) {
            State s = (State)itr.next();
            if(s.isAcceptable())
                out.println(getStateName(s)+" [shape=\"doublecircle\"];");
            
            Iterator jtr = s.iterateTransitions();
            while(jtr.hasNext() ) {
                Transition t = (Transition)jtr.next();
                
                String str = MessageFormat.format(
                        "{0} -> {1} [ label=\"{2}{3}{4}\",color=\"{5} 1 .5\",fontcolor=\"{5} 1 .3\" ];",
                        new Object[]{
                            getStateName(s),
                            getStateName(t.nextState()),
                            t.getAlphabet().toString(),
                            buildActionList('^',t.getPrologueActions()),
                            buildActionList('_',t.getEpilogueActions()),
                            getColor(t.getAlphabet()) });
                out.println(str);
            }
        }
        
        out.println("}");
        out.flush();
        out.close();
        
        BufferedReader in = new BufferedReader(
            new InputStreamReader(proc.getInputStream()));
        while(true) {
            String s = in.readLine();
            if(s==null)     break;
            System.out.println(s);
        }
        in.close();
        
        proc.waitFor();
    }
    
    /** concatanates all action names (so that it can be printed out.) */
    private static String buildActionList( char head, Action[] actions ) {
        if(actions.length==0)   return "";
        
        StringBuffer label = new StringBuffer();
        label.append(head);
        for( int i=0; i<actions.length; i++ ) {
            if(i!=0)    label.append(',');
            label.append(actions[i].getUniqueId());
        }
        return label.toString();
    }
}
