/*
 * CodeBuilder.java
 *
 * Created on 2001/08/05, 14:34
 */

package relaxngcc.builder;
import java.io.Writer;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import relaxngcc.NGCCGrammar;
import relaxngcc.Options;
import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.Head;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.grammar.NGCCDefineParam;
import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.SimpleNameClass;

import relaxngcc.codedom.*;

/**
 * generates Java code that parses XML data via NGCCHandler interface
 */
public class CodeBuilder
{
	//utility classes: for switch-case-if structure of each handler
	private class CodeAboutState
	{
		public CDBlock prologue;
        public CDBlock epilogue;
		public CDIfStatement conditionalCodes;
		public CDBlock elsecode;
		
		public void addConditionalCode(CDExpression cond, CDBlock code)
		{
			if(conditionalCodes==null) conditionalCodes = new CDIfStatement(cond, code);
			else conditionalCodes.addClause(cond, code);
		}
        
        public CDBlock output(CDStatement errorHandleMethod) {
        	CDBlock sv = new CDBlock();
        	
            if(prologue!=null) sv.add(prologue);
            
            //elsecode, null‚È‚çerrorHandleMethod‚Å•Â‚¶‚é
            
            if(elsecode!=null) {
            	if(conditionalCodes!=null)
	            	conditionalCodes.closeClause(elsecode);
	            else
	            	sv.add(elsecode);
            } else if(errorHandleMethod!=null) {
            	if(conditionalCodes!=null)
	            	conditionalCodes.closeClause(new CDBlock(errorHandleMethod));
	            else
	            	sv.add(errorHandleMethod);
            }
            
           	if(conditionalCodes!=null)
	            sv.add(conditionalCodes);
            
            if(epilogue!=null)
                sv.add(epilogue);
                
            return sv;
        }
	}
    
    /**
     * Generates code in the following format:
     * 
     * <pre>
     * switch(state) {
     * case state #1:
     *     === prologue code ===
     *     
     *     if( conditional #1 ) {
     *         statement #1;
     *     } else
     *     if( conditional #2 ) {
     *         statement #2;
     *     } else {
     *     if ...
     *  
     *     } else {
     *         === else code ===
     *     }
     *     
     *     === epilogue code ===
     *     break;
     * case state #n:
     *     ...
     *     break;
     * }
     * </pre>
     */
	private class SwitchBlockInfo
	{
		public Map state2CodeFragment = new HashMap();
		
		private int _Type; //one of the constants in Alphabet class
		public int getType() { return _Type; }
		public SwitchBlockInfo(int type) {
			_Type=type;
		}
        
        private CodeAboutState getCAS( State state ) {
            CodeAboutState cas = (CodeAboutState)state2CodeFragment.get(state);
            if(cas==null) {
                cas = new CodeAboutState();
                state2CodeFragment.put(state, cas);
            }
            return cas;
        }
        
		//if "cond" is "", "code" is put with no if-else clause. this behavior is not smart...
		public void addConditionalCode(State state, CDExpression cond, CDBlock code) {
			getCAS(state).addConditionalCode(cond,code);
		}
        
		public void addElseCode(State state, CDBlock code) {
			CodeAboutState cas = getCAS(state);
            
			if(cas.elsecode==null)
				cas.elsecode = code;
			else
				cas.elsecode.add(code);
		}
        
		public void addPrologue(State state, CDStatement code) {
			CodeAboutState cas = getCAS(state);
			if(cas.prologue==null)
				cas.prologue = new CDBlock(code);
			else
				cas.prologue.add(code);
		}
        
        public void addEpilogue(State state, CDStatement code) {
            CodeAboutState cas = getCAS(state);
            if(cas.epilogue==null)
                cas.epilogue = new CDBlock(code);
            else
                cas.epilogue.add(code);
        }

        private CDBlock output(CDStatement errorHandleMethod) {
        	CDBlock sv = new CDBlock();
        	CDIfStatement ifblock = null;
            Iterator i = state2CodeFragment.entrySet().iterator();
            while(i.hasNext())
            {
                Map.Entry e = (Map.Entry)i.next();
                State st = (State)e.getKey();
                
                CDExpression condition = CDOp.EQ(
                    (st.getThreadIndex()==-1) ?
                        $state : getThreadStateExp(st.getThreadIndex()),
                     new CDConstant(st.getIndex()));
                    
                CDBlock whentrue = ((CodeAboutState)e.getValue()).output(errorHandleMethod);
                
                if(ifblock==null)
                	ifblock = new CDIfStatement(condition, whentrue);
                else
                	ifblock.addClause(condition, whentrue);
                
            }
            
            if(errorHandleMethod!=null) {
            	if(ifblock!=null) ifblock.closeClause(new CDBlock(errorHandleMethod));
            }
            
            if(ifblock!=null) sv.add(ifblock);
            return sv;
        }
	}
	
	private ScopeInfo _info;
	private NGCCGrammar _grammar;
	private Options _Options;
	
    public CodeBuilder(NGCCGrammar grm, ScopeInfo sci, Options o)
	{
		_info = sci;
		_grammar = grm;
		_Options = o;
    }
    
    
    /** Special transition that means "revert to the parent." */
    private static final Transition REVERT_TO_PARENT = new Transition(null,null);
    
    private class TransitionTable {
        private final Map table = new HashMap();
        
        public void add( State s, Alphabet alphabet, Transition action ) {
            Map m = (Map)table.get(s);
            if(m==null)
                table.put(s,m=new HashMap());
            
            if(m.containsKey(alphabet)) {
                // TODO: proper error report
                System.out.println(MessageFormat.format(
                    "State #{0}  of \"{1}\" has a conflict by {2}",
                    new Object[]{
                        Integer.toString(s.getIndex()),
                        s.getContainer().scope.name,
                        alphabet } ));
                alphabet.printLocator(System.out);
            }
            m.put(alphabet,action);
        }
        
        /**
         * If EVERYTHING_ELSE is added to a transition table,
         * we will store that information here.
         */
        private final Map eeAction = new HashMap();
        
        public void addEverythingElse( State s, Transition action ) {
            eeAction.put(s,action);
        }
        
        /**
         * Gets the transition associated to EVERYTHING_ELSE alphabet
         * in the given state if any. Or null.
         */
        public Transition getEverythingElse( State s ) {
            return (Transition)eeAction.get(s);
        }
        
        /**
         * Lists all entries of the transition table with
         * the specified state in terms of  {@link Map.Entry}.
         */
        public Iterator list( State s ) {
            Map m = (Map)table.get(s);
            if(m==null)
                return new Iterator() {
                    public boolean hasNext() { return false; }
                    public Object next() { return null; }
                    public void remove() { throw new UnsupportedOperationException(); }
                };
            else
                return m.entrySet().iterator();
        }
    }
    



    private CDClass classdef;
    /** Reference to _ngcc_current_state. */
    private CDExpression $state;
    /** Reference to _ngcc_threaded_state. */
    private CDExpression $threadState;
    private CDExpression getThreadStateExp( int idx ) {
        return $threadState.arrayRef(idx);
    }
    /** Reference to runtime. */
    private CDVariable $runtime;
    
    /** Reference to "super". */
    private static final CDExpression $super = CDConstant.SUPER;
    
    /** Reference to "this". */
    private static final CDExpression $this = CDConstant.THIS;
    
    
    

    /**
     * Builds the code.
     */
    public CDClass createClassCode(Options options, String globalimport) {
        StringBuffer buf = new StringBuffer();
        
        //notice
        println(buf, options, "/* this file is generated by RelaxNGCC */");
        //package
        if(_grammar.packageName.length()>0)
            println(buf, options, "package " + _grammar.packageName + ";");
        
        /*
        //imports
        if(_UsingBigInteger)
            output.println("import java.math.BigInteger;");
        if(_UsingCalendar)
            output.println("import java.util.GregorianCalendar;");
        */

        println(buf, options, "import org.xml.sax.SAXException;");
        println(buf, options, "import org.xml.sax.XMLReader;");
        println(buf, options, "import org.xml.sax.Attributes;");
        
        if(!options.usePrivateRuntime)
            println(buf, options, "import relaxngcc.runtime.NGCCHandler;");
        if(!_grammar.getRuntimeTypeFullName().equals(_grammar.getRuntimeTypeShortName()))
            println(buf, options, "import "+_grammar.getRuntimeTypeFullName()+";");
        
        if(_info.isRoot()) {
            println(buf, options, "import javax.xml.parsers.SAXParserFactory;");
            println(buf, options, "import org.xml.sax.XMLReader;");
        }

        println(buf, options, globalimport);
        
        if(_info.scope.getImport()!=null)
            println(buf, options, _info.scope.getImport());

        println(buf, options, _info.getHeaderSection());
        
        //class name
        NGCCDefineParam param = _info.scope.getParam();
        
        CDClass classdef = new CDClass(new CDLanguageSpecificString[]{ new CDLanguageSpecificString(buf.toString()) }, new CDLanguageSpecificString(param.access), param.className, new CDLanguageSpecificString("extends NGCCHandler"));
        
        //NSURI constants
        for (Iterator itr = _info.iterateNSURIConstants(); itr.hasNext();) {
            Map.Entry e = (Map.Entry) itr.next();
            
            classdef.addMember(
                new CDLanguageSpecificString("public static final"),
                CDType.STRING,
                (String)e.getValue(),
                new CDConstant((String)e.getKey()));
        }
        
        // aliases
        for (Iterator itr = _info.iterateAliases(); itr.hasNext();) {
            Alias a = (Alias)itr.next();
            
            // if the alias is already declared explicitly by the <java-body>,
            // don't write it again.
            if(_info.isUserDefinedField(a.name))
                continue;
            
            classdef.addMember(
                new CDLanguageSpecificString("private"), a.type, a.name);
        }

        {// runtime field and the getRuntime method.
            String runtimeBaseName = "relaxngcc.runtime.NGCCRuntime";
            if(options.usePrivateRuntime) runtimeBaseName = "NGCCRuntime";
    
            
            $runtime = classdef.addMember(
                new CDLanguageSpecificString("protected final"),
                new CDType(_grammar.getRuntimeTypeShortName()), "runtime" );
            
            CDMethod getRuntime = new CDMethod(
                new CDLanguageSpecificString("public final"),
                new CDType(runtimeBaseName),
                "getRuntime", null );
            classdef.addMethod(getRuntime);
            
            getRuntime.body()._return($runtime);
        }


        // create references to variables
        $state = classdef.addMember(
            new CDLanguageSpecificString("private"),
            CDType.INTEGER,
            "_ngcc_current_state");
            
        if(_info.getThreadCount()>0)
            $threadState = classdef.addMember(
                new CDLanguageSpecificString("private"),
                new CDType("int[]"),
                "_ngcc_threaded_state");
        
        
        
        Alias[] constructorParams = _info.getConstructorParams();
        
        {// internal constructor
            CDMethod cotr1 = new CDMethod(
                new CDLanguageSpecificString("public"),
                null, param.className, null );
            classdef.addMethod(cotr1);
            
            // add three parameters (parent,runtime,cookie) and call the super class initializer.
            CDVariable $parent = cotr1.param( new CDType("NGCCHandler"), "_parent" );
            CDVariable $_runtime = cotr1.param( new CDType(_grammar.getRuntimeTypeShortName()), "_runtime" );
            CDVariable $cookie = cotr1.param( CDType.INTEGER, "_cookie" );
            cotr1.body().invoke("super").arg($parent).arg($cookie);
            cotr1.body().assign($runtime,$_runtime);
            
            // append additional constructor arguments
            for( int i=0; i<constructorParams.length; i++ ) {
                CDVariable v = cotr1.param(
                    constructorParams[i].type,
                    '_'+constructorParams[i].name);
                cotr1.body().assign( $this.prop(constructorParams[i].name),
                    v );
            }
            
            // move to the initial state
            cotr1.body().assign( $state,
                new CDConstant(_info.getInitialState().getIndex()) );
    
            if(_info.getThreadCount()>0)
                cotr1.body().assign( $threadState,
                    new CDLanguageSpecificString("new int[" + _info.getThreadCount() + "]"));
        }        
        
        {// external constructor
            CDMethod cotr2 = new CDMethod(
                    new CDLanguageSpecificString("public"),
                    null, param.className, null );
            classdef.addMethod(cotr2);

            CDVariable $_runtime = cotr2.param( new CDType(_grammar.getRuntimeTypeShortName()), "_runtime" );
            
            // call the primary constructor
            CDMethodInvokeExpression callThis = cotr2.body().invoke("this")
                .arg( CDConstant.NULL )
                .arg( $_runtime )
                .arg( new CDConstant(-1) );
            
            // append additional constructor arguments
            for( int i=0; i<constructorParams.length; i++ ) {
                CDVariable v = cotr2.param(
                    constructorParams[i].type,
                    '_'+constructorParams[i].name);
                callThis.arg(v);
            }
        }


                
        // action functions
        for (Iterator itr = _info.iterateActions(); itr.hasNext();) {
            ScopeInfo.Action a = (ScopeInfo.Action) itr.next();
            a.generate(classdef);
        }
        
        //simple entry point.
        if(_info.isRoot() && _info.scope.getParam().params==null) {
            String rt = _grammar.packageName;
            if(rt.length()!=0)  rt+='.';
            rt+="NGCCRuntime";
            
            if(_grammar.getRuntimeTypeFullName().equals(rt)) {
                classdef.addLanguageSpecificString(new CDLanguageSpecificString(
                    "    public static void main( String[] args ) throws Exception {\n"+
                    "        SAXParserFactory factory = SAXParserFactory.newInstance();\n"+
                    "        factory.setNamespaceAware(true);\n"+
                    "        XMLReader reader = factory.newSAXParser().getXMLReader();\n"+
                    "        NGCCRuntime runtime = new NGCCRuntime();\n"+
                    "        reader.setContentHandler(runtime);\n"+
                    "        for( int i=0; i<args.length; i++ ) {\n"+
                    "            runtime.pushHandler(new "+_info.getClassName()+"(runtime));\n"+
                    "            reader.parse(args[i]);\n"+
                    "            runtime.reset();\n"+
                    "        }\n"+
                    "    }"));
            }
        }
        
        return classdef;

    }
    
    
	public CDClass output() throws IOException {
		classdef = createClassCode(_Options,_grammar.globalImportDecls);
        
        classdef.addMethod(createAcceptedMethod());
        
        // build transition table map< state, map<non-ref alphabet,transition> >
        TransitionTable table = new TransitionTable();
        
        Iterator itr = _info.iterateAllStates();
        while(itr.hasNext()) {
            State s = (State)itr.next();
            
            if(s.isAcceptable())
                table.addEverythingElse( s, REVERT_TO_PARENT );
            
            Iterator jtr = s.iterateTransitions();
            while(jtr.hasNext()) {
                Transition t = (Transition)jtr.next();
                
                Set head = t.head(true);
                if(head.contains(Head.EVERYTHING_ELSE)) {
                    // TODO: check ambiguity
                    table.addEverythingElse( s, t );
                    head.remove(Head.EVERYTHING_ELSE);
                }
                for (Iterator ktr = head.iterator(); ktr.hasNext();)
                    table.add(s,(Alphabet)ktr.next(),t);
            }
        }
        
        CDType[] types = new CDType[] { new CDType("Attributes") };
        String[] args = new String[] { "attrs" };
        classdef.addMethod(writeEventHandler(table,Alphabet.ENTER_ELEMENT,   "enterElement", types, args));
        classdef.addMethod(writeEventHandler(table,Alphabet.LEAVE_ELEMENT,   "leaveElement"));
        classdef.addMethod(writeEventHandler(table,Alphabet.ENTER_ATTRIBUTE, "enterAttribute"));
        classdef.addMethod(writeEventHandler(table,Alphabet.LEAVE_ATTRIBUTE, "leaveAttribute"));
    
		classdef.addMethod(writeTextHandler(table));
		classdef.addMethod(writeAttributeHandler());
        classdef.addMethod(writeChildCompletedHandler());

/*        
        Iterator lambda_scopes = _Info.iterateChildScopes();
        while(lambda_scopes.hasNext())
        {
            CodeBuilder wr = new CodeBuilder(_Grammar, (ScopeInfo)lambda_scopes.next(), _Options);
            wr.output(out);
        }
*/
        if(_info.scope.getBody()!=null)
            classdef.addLanguageSpecificString(new CDLanguageSpecificString(_info.scope.getBody()));
        classdef.addLanguageSpecificString(new CDLanguageSpecificString(_grammar.globalBody));
        
		return classdef;
	}
    

	
    private CDMethod createAcceptedMethod()
    {
        Iterator states = _info.iterateAcceptableStates();
        CDExpression statecheckexpression = null;
		while(states.hasNext())
		{
			State s = (State)states.next();
            CDExpression temp = null;
			if(s.getThreadIndex()==-1)
				temp = $state;
			else
				temp = getThreadStateExp(s.getThreadIndex());
			
            temp = CDOp.EQ( temp, new CDConstant(s.getIndex()) );
            
			statecheckexpression = (statecheckexpression==null)? temp : CDOp.OR(temp, statecheckexpression);
        }
        
        if(statecheckexpression==null) statecheckexpression = new CDConstant(false);
        
        CDMethod m = new CDMethod(new CDLanguageSpecificString("public"), CDType.BOOLEAN, "accepted",null);
        m.body()._return(statecheckexpression);
        return m;
    }

    private CDMethod writeEventHandler( TransitionTable table, int type, String eventName ) {
        return writeEventHandler(table,type,eventName,new CDType[0],new String[0]);
    }
	
    /**
     * Writes event handlers for (enter|leave)(Attribute|Element) methods.
     */
    private CDMethod writeEventHandler( TransitionTable table, int type, String eventName,
        CDType[] additionalTypes, String[] additionalArgs ) {

        CDMethod method = new CDMethod(
            new CDLanguageSpecificString("public"),
            CDType.VOID, eventName,
            new CDLanguageSpecificString("throws SAXException") );
        
        CDVariable $uri = method.param( CDType.STRING, "uri" );
        CDVariable $localName = method.param( CDType.STRING, "local" );
        CDVariable $qname = method.param( CDType.STRING, "qname" );
        
        CDVariable[] additionalVars = new CDVariable[additionalTypes.length];
        for( int i=0; i<additionalTypes.length; i++ )
            additionalVars[i] = method.param( additionalTypes[i], additionalArgs[i] );
        
        CDBlock sv = method.body();
		//printSection(eventName);
            
        // QUICK HACK
        // copy them to the instance variables so that they can be 
        // accessed from action functions.
        // we should better not keep them at Runtime, because
        // this makes it impossible to emulate events.
        sv.assign($super.prop("uri"),       $uri);
        sv.assign($super.prop("localName"), $localName);
        sv.assign($super.prop("qname"),     $qname);
            
		if(_Options.debug) {
			sv.invoke( $runtime, "traceln")
                    .arg(new CDLanguageSpecificString("\""+eventName + " \"+qname+\" #\" + _ngcc_current_state"));
        }
        
		SwitchBlockInfo bi = new SwitchBlockInfo(type);
		CDExpression[] arguments = new CDExpression[3 + additionalArgs.length];
		arguments[0] = $uri;
		arguments[1] = $localName;
		arguments[2] = $qname;
		for(int i=0; i<additionalArgs.length; i++)
			arguments[3+i] = additionalVars[i];
		
        Iterator states = _info.iterateAllStates();
		while(states.hasNext()) {
			State st = (State)states.next();
            
            // list all the transition table entry
            Iterator itr = table.list(st);
            while(itr.hasNext()) {
                Map.Entry e = (Map.Entry)itr.next();
                
                Alphabet a = (Alphabet)e.getKey();      // alphabet
                Transition tr = (Transition)e.getValue();// action to perform
                
                if(a.getType()!=type)
                    continue;   // we are not interested in this attribute now.
                
				bi.addConditionalCode(st,
                    (CDExpression)a.asMarkup().getKey().apply(
                        new NameTestBuilder($uri,$localName)),
					buildTransitionCode(st,tr,eventName,arguments));
			}

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null)
                bi.addElseCode(st,
                    buildTransitionCode(st,tr,eventName,arguments));
		}
        
        CDStatement eh = new CDMethodInvokeExpression("unexpected"+capitalize(eventName))
                .arg($qname).asStatement();
		sv.add(bi.output(eh));

		
		return method;
		//_output.println(MessageFormat.format(
        //    "public void {0}(String uri,String localName,String qname{1}) throws SAXException '{'",
        //    new Object[]{eventName,argumentsWithTypes}));

		
	}
    
    /**
     * Gets a code fragment that corresponds to a strate transition.
     * This includes house-keeping, any associated actions, changing
     * the current state, etc.
     * 
     * @param params
     *      Additional parameters that need to be passed to
     *      the revertToParentFromXXX method or the
     *      spawnChildFromXXX method.
     */
    private CDBlock buildTransitionCode( State current, Transition tr, String eventName, CDExpression[] additionalparams ) {
	    
	    if(tr==REVERT_TO_PARENT) {
            
            CDType retType  = _info.scope.getParam().returnType;
            String boxType = getJavaBoxType(retType);
            
            CDExpression r = _info.scope.getParam().returnValue;
            if(boxType!=null)
                r = new CDType(boxType)._new().arg(r);
            
	    	CDBlock sv = current.invokeActionsOnExit();
	    	sv.invoke( $runtime, "revertToParentFrom"+capitalize(eventName))
                    .arg(r)
                    .arg($super.prop("cookie"))
                    .args(additionalparams);
	    	return sv;
        }
        
        if(tr.getAlphabet().isEnterElement()) {
        	CDBlock sv = new CDBlock();
            sv.invoke( $runtime, "pushAttributes")
                    .arg(new CDLanguageSpecificString("attrs"));
            sv.add(buildMoveToStateCode(tr));
            
            return sv;
        }
        
        if(tr.getAlphabet().isText()) {
            CDBlock sv = new CDBlock();
            Alphabet.Text ta = tr.getAlphabet().asText();
            String alias = ta.getAlias();
            if(alias!=null)
                sv.assign(new CDLanguageSpecificString(alias), new CDLanguageSpecificString("___$value"));
            sv.add(buildMoveToStateCode(tr));
            
            return sv;
        }
	    if(tr.getAlphabet().isRef())
	        return buildCodeToSpawnChild(eventName,tr,additionalparams);
            
        return buildMoveToStateCode(tr);
    }
    
    /**
     * Generates a code fragment that creates a new child object
     * and switches to it.
     * 
     * @param eventName
     *      The event name for which we are writing an event handler.
     * @param ref_tr
     *      The transition with REF_BLOCK type alphabet.
     * @param eventParams
     *      Additional parameters that will be passed to the
     *      spawnChildFromXXX method. Usually the parameters
     *      given to the event handler.
     * @return
     *      code fragment.
     */
    private CDBlock buildCodeToSpawnChild(String eventName,Transition ref_tr, CDExpression[] eventParams) {
        
        CDBlock sv = new CDBlock();
        Alphabet.Ref alpha = ref_tr.getAlphabet().asRef();
        ScopeInfo ref_block = alpha.getTargetScope();
        
        sv.add(ref_tr.invokePrologueActions());
        
        /* Caution
         *  alpha.getParams() may return more than one argument concatinated by ','.
         *  But I give it away because separating the string into CDExpression[] is hard.
         */
        String extraarg = alpha.getParams();
        
        CDObjectCreateExpression oe = 
            new CDType(ref_block.getClassName())._new()
                .arg($this)
                .arg($runtime)
                .arg(new CDConstant(ref_tr.getUniqueId()));
        if(extraarg.length()>0)
            oe.arg(new CDLanguageSpecificString(extraarg.substring(1)));
            
        CDExpression $h = sv.decl(new CDType("NGCCHandler"), "h", oe );
        
        
        if(_Options.debug) {
        	CDExpression msg = new CDConstant(MessageFormat.format(
                "Change Handler to {0} (will back to:#{1})",
                new Object[]{
                    ref_block.getClassName(),
                    new Integer(ref_tr.nextState().getIndex())
                }));
                
        	sv.invoke($runtime, "traceln").arg(msg);
        }
        
        sv.invoke($runtime, "spawnChildFrom"+capitalize(eventName))
            .arg($h).args(eventParams);
                
        return sv;
    }

    
    /**
     * Creates code that changes the current state to the nextState
     * of the transition.
     */
    private CDBlock buildMoveToStateCode(Transition tr)
    {
        CDBlock sv = new CDBlock();
        
        sv.add(tr.invokePrologueActions());
        State nextstate = tr.nextState();
        
        if(tr.getDisableState()!=null) {
        	CDExpression dest;
            if(tr.getDisableState().getThreadIndex()==-1)
                dest = $state;
            else
                dest = getThreadStateExp(tr.getDisableState().getThreadIndex());
            
            sv.assign(dest, new CDConstant(-1));
        }
        
        if(tr.getEnableState()!=null) {
        	CDExpression dest;
            if(tr.getEnableState().getThreadIndex()==-1)
                dest = $state;
            else
                dest = getThreadStateExp(tr.getEnableState().getThreadIndex());
        
            sv.assign(dest, new CDConstant(tr.getEnableState().getIndex()));
        }
        
        State result = appendStateTransition(sv, nextstate);
        sv.add(tr.invokeEpilogueActions());
        
        return sv;
    }
    
    /** Capitalizes the first character. */
    private String capitalize( String name ) {
        return Character.toUpperCase(name.charAt(0))+name.substring(1);
    }
    
	
	//outputs text consumption handler. this handler branches by output method
	private CDMethod writeTextHandler(TransitionTable table) {
		//printSection("text");

        CDMethod method = new CDMethod(
            new CDLanguageSpecificString("public"),
            CDType.VOID,
            "text",
            new CDLanguageSpecificString("throws SAXException"));
		
        CDVariable $value = method.param( CDType.STRING, "___$value" );
        
		CDBlock sv = method.body();
		
        if(_Options.debug) {
        	sv.invoke( $runtime, "trace" )
                .arg(new CDLanguageSpecificString("\"text '\"+___$value.trim()+\"' #\" + _ngcc_current_state"));
        }

		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.VALUE_TEXT);
        
        Iterator states = _info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            // if a transition by <data> is present, then
            // we cannot execute "everything_else" action.
            boolean dataPresent = false;
            
            // list all the transition table entry
            Iterator itr = table.list(st);
            while(itr.hasNext()) {
                Map.Entry e = (Map.Entry)itr.next();
                
                Alphabet a = (Alphabet)e.getKey();      // alphabet
                Transition tr = (Transition)e.getValue();// action to perform
                
                if(!a.isText())
                    continue;   // we are not interested in this attribute now.
                
                CDBlock code = buildTransitionCode(st,tr,"text",new CDExpression[]{ $value });
                if(a.isValueText())
                    bi.addConditionalCode(st,
                        CDOp.STREQ( $value, new CDConstant(a.asValueText().getValue())), code);
                else {
                    dataPresent = true;
                    bi.addElseCode(st, code);
                }
            }

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null && !dataPresent)
                bi.addElseCode(st, buildTransitionCode(st,tr,"text",new CDExpression[]{ $value }));
        }
        
        CDStatement errorHandler = null;
        if(_Options.debug)
            errorHandler = $runtime.invoke("traceln")
                    .arg(new CDConstant("ignored")).asStatement();
		sv.add(bi.output(errorHandler));
        
		//_output.println("public void text(String ___$value) throws SAXException");
		//_output.println("{");

        return method;
	}
    
    private static final String[] boxTypes = {
            "boolean","Boolean",
            "char","Character",
            "byte","Byte",
            "short","Short",
            "int","Integer",
            "long","Long",
            "float","Float",
            "double","Double"};
    
    private String getJavaBoxType( CDType type ) {
        for( int i=0; i<boxTypes.length; i+=2 )
            if( boxTypes[i].equals(type.name) )
                return boxTypes[i+1];
        return null;
    }
    
    private CDMethod writeChildCompletedHandler() {
        //printSection("child completed");
        CDMethod method = new CDMethod(
            new CDLanguageSpecificString("public"),
            CDType.VOID,
            "onChildCompleted",
            new CDLanguageSpecificString("throws SAXException") );

        CDVariable $result = method.param( new CDType("Object"), "result" );
        CDVariable $cookie = method.param( CDType.INTEGER, "cookie" );
        CDVariable $attCheck = method.param( CDType.BOOLEAN, "needAttCheck" );
        
        CDBlock sv = method.body();
        
        if(_Options.debug) {
        	sv.invoke( $runtime, "traceln" )
                .arg( new CDLanguageSpecificString("\"onChildCompleted(\"+cookie+\") back to "+_info.getClassName()+"\""));
        }
        
        CDSwitchStatement switchstatement = new CDSwitchStatement($cookie);
        
        Set processedTransitions = new HashSet();
        
        Iterator states = _info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            
            Iterator trans =st.iterateTransitions(Alphabet.REF_BLOCK);
            while(trans.hasNext()) {
                Transition tr = (Transition)trans.next();
                Alphabet.Ref a = tr.getAlphabet().asRef();
                
                if(processedTransitions.add(tr)) {
                    
                    CDBlock block = new CDBlock();
                    // if there is an alias, assign to that variable
                    String alias = a.getAlias();
                    if(alias!=null) {
                        ScopeInfo childBlock = a.getTargetScope();
                        CDType returnType = childBlock.scope.getParam().returnType;
                        
                        String boxType = getJavaBoxType(returnType);
                        CDExpression rhs;
                        if(boxType==null)
                            rhs = new CDCastExpression( returnType, $result);
                        else
                            rhs = new CDCastExpression( new CDType(boxType),
                                $result).invoke(returnType.name+"Value");
                            
                        block.assign( $this.prop(alias), rhs );
                    }
                    
                    block.add(tr.invokeEpilogueActions());

                    appendStateTransition(block, tr.nextState(), $attCheck);
                        
                    switchstatement.addCase(new CDConstant(tr.getUniqueId()), block);
                }
            }
        }
        sv.add(switchstatement);
        
        // TODO: assertion failed
        //_output.println("default:");
        //_output.println("    throw new InternalError();");
        
        //_output.println("public void onChildCompleted(Object result, int cookie,boolean needAttCheck) throws SAXException {");
        return method;
    }
    
    
	private CDMethod writeAttributeHandler() {
		//printSection("attribute");
		//_output.println("public void processAttribute() throws SAXException");

        CDMethod method = new CDMethod(
            new CDLanguageSpecificString("public"),
            CDType.VOID,
            "processAttribute",
            new CDLanguageSpecificString("throws SAXException") );
		
		CDBlock sv = method.body();
        
		CDVariable $ai = sv.decl(CDType.INTEGER, "ai");
		if(_Options.debug)
			sv.invoke( $runtime, "traceln")
                .arg( new CDLanguageSpecificString("\"processAttribute (\" + runtime.getCurrentAttributes().getLength() + \" atts) #\" + _ngcc_current_state")); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.ENTER_ATTRIBUTE);
        
        Iterator states = _info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            writeAttributeHandler(bi,st,st,$ai);
        }
        
        sv.add(bi.output(null));
        
        return method;
    }
    
    private void writeAttributeHandler( SwitchBlockInfo bi, State source, State current, CDVariable $ai ) {
        
        Set attHead = current.attHead();
        for(Iterator jtr=attHead.iterator(); jtr.hasNext(); ) {
            Alphabet a = (Alphabet)jtr.next();
            
            if(a.isRef()) {
                writeAttributeHandler( bi, source, a.asRef().getTargetScope().getInitialState(), $ai );
            } else {
                writeAttributeHandlerBlock( bi, source, a.asEnterAttribute(), $ai );
            }
        }
    }

    private void writeAttributeHandlerBlock( SwitchBlockInfo bi, State st, Alphabet.EnterAttribute a, CDVariable $ai ) {
        
        NameClass nc = a.getKey();
        if(nc instanceof SimpleNameClass) {
            SimpleNameClass snc = (SimpleNameClass)nc;
            
            CDExpression condition = new CDLanguageSpecificString(MessageFormat.format(
	            "(ai = runtime.getAttributeIndex(\"{0}\",\"{1}\"))>=0",
	                new Object[]{
	                    snc.nsUri, snc.localName})); //chotto sabori gimi
	       
            CDBlock sv = new CDBlock();
            sv.invoke( $runtime, "consumeAttribute").arg($ai);
	        	
	        bi.addConditionalCode(st, condition, sv);
        } else {
            // if the name class is complex
            throw new UnsupportedOperationException(
                "attribute with a complex name class is not supported yet  name class:"+nc.toString());
        }
    }
    
    private State appendStateTransition(CDBlock sv, State deststate ) {
        return appendStateTransition(sv,deststate,null);
    }
    
    /**
     * @param flagVarName
     *      If this parameter is non-null, the processAttribute method
     *      should be called if and only if this variable is true.
     */
	// What's the difference of this method and "buildMoveToStateCode"? - Kohsuke
	private State appendStateTransition(CDBlock sv, State deststate, CDVariable flagVar)
	{
		
		CDExpression statevariable = null;
		if(deststate.getThreadIndex()==-1)
			statevariable = $state;
		else
			statevariable = getThreadStateExp(deststate.getThreadIndex());
		
		sv.assign(statevariable, new CDConstant(deststate.getIndex()));
		
		if(_Options.debug) {
        	String trace;	
            if(deststate.getThreadIndex()==-1)
                trace = "-> #" + deststate.getIndex();
            else
                trace = "-> #[" + deststate.getThreadIndex() + "]"+ deststate.getIndex();

        	sv.invoke( $runtime, "traceln" )
                .arg( new CDConstant(trace));
               
        }

        if(!deststate.attHead().isEmpty()) {
        	
        	CDStatement processAttribute =
                new CDMethodInvokeExpression("processAttribute").asStatement();
            
            if(flagVar!=null)
                sv.add(new CDIfStatement(flagVar, new CDBlock(processAttribute)));
            else
	            sv.add(processAttribute);
        }
		
		if(deststate.getMeetingDestination()!=null) {
			CDExpression condition = null;
			Iterator it = deststate.getMeetingDestination().iterateStatesForWait();
			while(it.hasNext()) {
				State s = (State)it.next();
				if(s==deststate) continue;
				
				CDExpression t = CDOp.EQ(
                    getThreadStateExp(s.getThreadIndex()),
					new CDConstant(s.getIndex()));
				
				condition = condition==null? t : CDOp.AND(condition, t);
			}
			
			CDBlock whentrue = new CDBlock();
			State t = appendStateTransition(whentrue, deststate.getMeetingDestination());
			if(condition==null)
				sv.add(whentrue);
			else
				sv.add(new CDIfStatement(condition, whentrue));
			return t;
		}
		else
			return deststate;
	}


    private static void println(StringBuffer buf, Options options, String data) {
        buf.append(data);
        buf.append(options.newline);
    }
}
