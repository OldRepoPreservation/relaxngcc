/*
 * CodeWriter.java
 *
 * Created on 2001/08/05, 14:34
 */

package relaxngcc.builder;
import java.io.PrintStream;
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
import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.SimpleNameClass;

/**
 * generates Java code that parses XML data via NGCCHandler interface
 */
public class CodeWriter
{
	//utility classes: for switch-case-if structure of each handler
	private class ConditionalCode
	{
		public String condition;
		public String code;
		public ConditionalCode(String t1, String t2) { condition=t1; code=t2; }
	}
	private class CodeAboutState
	{
		public String prologue;
        public String epilogue;
		public Vector conditionalCodes;
		public String elsecode;
		
		public void addConditionalCode(String cond, String code)
		{
			if(conditionalCodes==null) conditionalCodes = new Vector();
			conditionalCodes.add(new ConditionalCode(cond, code));
		}
        
        public void output(String errorHandleMethod) {
            if(prologue!=null) _output.println(prologue);
            
            boolean flag = false;
            if(conditionalCodes!=null)
            {
                Iterator ccs = conditionalCodes.iterator();
                while(ccs.hasNext())
                {
                    ConditionalCode cc = (ConditionalCode)ccs.next();
                    _output.print(flag? "else if(" : "if(");
                    _output.print(cc.condition);
                    _output.println(") {");
                    //_output.println(_Options.newline);
                    _output.println(cc.code);
                    //_output.println(_Options.newline);
                    _output.println("}");
                    flag = true;
                }
            }
            
            if(elsecode!=null)
            {
                if(flag) _output.println("else {"); else _output.println("{");
                //_output.println(_Options.newline);
                _output.println(elsecode);
                //_output.println(_Options.newline);
                _output.println("}");
            } else
            if(errorHandleMethod!=null) {
                if(flag) _output.print("else ");
                _output.println(errorHandleMethod);
            }
            
            if(epilogue!=null)
                _output.println(epilogue);
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
		public void addConditionalCode(State state, String cond, String code) {
			getCAS(state).addConditionalCode(cond,code);
		}
        
		public void addElseCode(State state, String code) {
			CodeAboutState cas = getCAS(state);
            
			if(cas.elsecode==null)
				cas.elsecode = code;
			else
				cas.elsecode += code;
		}
        
		public void addPrologue(State state, String code) {
			CodeAboutState cas = getCAS(state);
			if(cas.prologue==null)
				cas.prologue = code;
			else
				cas.prologue += code;
		}
        
        public void addEpilogue(State state, String code) {
            CodeAboutState cas = getCAS(state);
            if(cas.epilogue==null)
                cas.epilogue = code;
            else
                cas.epilogue += code;
        }

        private void output(String errorHandleMethod) {
            boolean first = true;
            Iterator i = state2CodeFragment.entrySet().iterator();
            while(i.hasNext())
            {
                Map.Entry e = (Map.Entry)i.next();
                State st = (State)e.getKey();
                if(!first) _output.print("else ");
                if(st.getThreadIndex()==-1)
                    _output.println("if(_ngcc_current_state==" + st.getIndex()+") {");
                else
                    _output.println("if(_ngcc_threaded_state[" + st.getThreadIndex() + "]=="+ st.getIndex()+") {");
                    
                ((CodeAboutState)e.getValue()).output(errorHandleMethod);
                
                _output.println("}");
                _output.println();
                first = false;
            }
            
            if(errorHandleMethod!=null) {
                if(!first)    _output.print("else ");
                _output.println(errorHandleMethod);
            }
        }
	}
	
	private ScopeInfo _Info;
	private PrintStream _output;
	private NGCCGrammar _Grammar;
	private Options _Options;
	
    public CodeWriter(NGCCGrammar grm, ScopeInfo sci, Options o)
	{
		_Info = sci;
		_Grammar = grm;
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
                    "State {0} has a conflict by {1}",
                    new Object[]{
                        Integer.toString(s.getIndex()),
                        alphabet } ));
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
    
	public void output(PrintStream out)
	{
		_output = out;
		_Info.printHeaderSection(out, _Options, _Grammar.globalImportDecls);
        
        out.println("private String uri,localName,qname;");
        
        writeAcceptableStates();
        
        // build transition table map< state, map<non-ref alphabet,transition> >
        TransitionTable table = new TransitionTable();
        
        Iterator itr = _Info.iterateAllStates();
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
        
        
        writeEventHandler(table,Alphabet.ENTER_ELEMENT,   "enterElement",",Attributes atts",",atts");
        writeEventHandler(table,Alphabet.LEAVE_ELEMENT,   "leaveElement");
        writeEventHandler(table,Alphabet.ENTER_ATTRIBUTE, "enterAttribute");
        writeEventHandler(table,Alphabet.LEAVE_ATTRIBUTE, "leaveAttribute");
    
		writeTextHandler(table);
		writeAttributeHandler();
        writeChildCompletedHandler();

/*        
        Iterator lambda_scopes = _Info.iterateChildScopes();
        while(lambda_scopes.hasNext())
        {
            CodeWriter wr = new CodeWriter(_Grammar, (ScopeInfo)lambda_scopes.next(), _Options);
            wr.output(out);
        }
*/
		_Info.printTailSection(_Grammar.globalBody, out);
	}
	
    private void writeAcceptableStates()
    {
        _output.println("public boolean accepted() {");
		_output.print("return ");
        boolean first = true;
        Iterator states = _Info.iterateAcceptableStates();
		while(states.hasNext())
		{
            if(!first) _output.print(" || ");
			State s = (State)states.next();
			if(s.getThreadIndex()==-1)
				_output.print("_ngcc_current_state==" + s.getIndex());
			else
				_output.print("_ngcc_threaded_state[" + s.getThreadIndex() + "]=="+ s.getIndex());
			first = false;
        }
        
        if(first)   _output.print("false");
        
        _output.println(";");
        _output.println("}"); //end of method
    }

    private void writeEventHandler( TransitionTable table, int type, String eventName ) {
        writeEventHandler(table,type,eventName,"","");
    }
	
    /**
     * Writes event handlers for (enter|leave)(Attribute|Element) methods.
     */
    private void writeEventHandler( TransitionTable table, int type, String eventName,
        String argumentsWithTypes, String arguments ) {
        
		printSection(eventName);
		_output.println(MessageFormat.format(
            "public void {0}(String uri,String localName,String qname{1}) throws SAXException '{'",
            new Object[]{eventName,argumentsWithTypes}));
            
        // QUICK HACK
        // copy them to the instance variables so that they can be 
        // accessed from action functions.
        // we should better not keep them at Runtime, because
        // this makes it impossible to emulate events.
        _output.println("this.uri=uri;");
        _output.println("this.localName=localName;");
        _output.println("this.qname=qname;");
            
		if(_Options.debug) {
			_output.println(MessageFormat.format(
                "runtime.trace(\"{0} \"+qname+\" #\" + _ngcc_current_state);",
                new Object[]{
                    eventName
                }));
        }
        
		SwitchBlockInfo bi = new SwitchBlockInfo(type);
		
        Iterator states = _Info.iterateAllStates();
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
//                    a.asMarkup().getKey().createJudgementClause(_Info, "uri", "localName"),
                    (String)a.asMarkup().getKey().apply(new NameTestBuilder("uri","localName")),
					buildTransitionCode(st,tr,eventName,"uri,localName,qname"+arguments));
			}

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null)
                bi.addElseCode(st,
                    buildTransitionCode(st,tr,eventName,"uri,localName,qname"+arguments));
		}
        
        
		
		bi.output("unexpected"+capitalize(eventName)+"(qname);");
		
		_output.println("}");   //end of method
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
    private String buildTransitionCode( State current, Transition tr, String eventName, String params ) {
	    if(tr==REVERT_TO_PARENT) {
	        return current.invokeActionsOnExit()+
                MessageFormat.format(
		            "runtime.revertToParentFrom{0}({1},cookie, {2});",
		            new Object[]{
		                capitalize(eventName),
		                _Info.scope.getParam().returnValue,
	                    params,
		            });
        }
        if(tr.getAlphabet().isEnterElement()) {
            StringBuffer buf = new StringBuffer();
            buf.append("runtime.pushAttributes(atts);");
            buf.append(buildMoveToStateCode(tr));
            
            return buf.toString();
        }
        if(tr.getAlphabet().isText()) {
            Alphabet.Text ta = tr.getAlphabet().asText();
            StringBuffer buf = new StringBuffer();
            String alias = ta.getAlias();
            if(alias!=null)
                buf.append(alias + "=___$value;");
            buf.append(buildMoveToStateCode(tr));
            
            return buf.toString();
        }
	    if(tr.getAlphabet().isRef())
	        return buildCodeToSpawnChild(eventName,tr,params);
            
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
    private String buildCodeToSpawnChild(String eventName,Transition ref_tr,
        String eventParams) {
        
        Alphabet.Ref alpha = ref_tr.getAlphabet().asRef();
        StringBuffer code = new StringBuffer();
        ScopeInfo ref_block = alpha.getTargetScope();
        
        code.append(ref_tr.invokePrologueActions());
        
        code.append(MessageFormat.format(
            "NGCCHandler h = new {0}(this,runtime,{1}{2});{3}", new Object[]{
                ref_block.getClassName(),
                new Integer(ref_tr.getUniqueId()).toString()/*to avoid ,*/,
                alpha.getParams(),
                _Options.newline}));
            
        if(_Options.debug) {
            code.append(MessageFormat.format(
                "runtime.traceln(\"\");\n"+
                "runtime.traceln(\"Change Handler to {0} (will back to:#{1})\");" + _Options.newline,
                new Object[]{
                    ref_block.getClassName(),
                    new Integer(ref_tr.nextState().getIndex())
                }));
        }
        code.append(MessageFormat.format(
            "runtime.spawnChildFrom{0}(h,{1});\n",
            new Object[]{
                capitalize(eventName),
                eventParams}));
                
        code.append(_Options.newline);
        
        return code.toString();
    }

    
    /**
     * Creates code that changes the current state to the nextState
     * of the transition.
     */
    private String buildMoveToStateCode(Transition tr/*, boolean output_process_attribute*/)
    {
        StringBuffer buf = new StringBuffer();
        
        buf.append(tr.invokePrologueActions());
        State nextstate = tr.nextState();
        if(tr.getDisableState()!=null)
        {
            if(tr.getDisableState().getThreadIndex()==-1)
                buf.append("_ngcc_current_state=-1;");
            else
                buf.append("_ngcc_threaded_state[" + tr.getDisableState().getThreadIndex() + "]=-1;");
            buf.append(_Options.newline);
        }
        if(tr.getEnableState()!=null)
        {
            if(tr.getEnableState().getThreadIndex()==-1)
                buf.append("_ngcc_current_state=" + tr.getEnableState().getIndex());
            else
                buf.append("_ngcc_threaded_state[" + tr.getEnableState().getThreadIndex() + "]="+ tr.getEnableState().getIndex());
            buf.append(";");
            buf.append(_Options.newline);
        }
        buf.append(tr.invokeEpilogueActions());
        State result = appendStateTransition(buf, nextstate);
        buf.append(_Options.newline);
        
//        if(_Options.style!=Options.STYLE_MSV)
//        {
            if(result.getListMode()==State.LISTMODE_ON)
                buf.append("runtime.setListMode();"); // _tokenizeText=true;");
//  TODO: why do we need this?
//          else if(result.getListMode()==State.LISTMODE_OFF)
//              buf.append("_tokenizeText=false;");
//        }
        
        boolean in_if_block = false;
                                
        return buf.toString();
    }
    
    /** Capitalizes the first character. */
    private String capitalize( String name ) {
        return Character.toUpperCase(name.charAt(0))+name.substring(1);
    }
    
	
	//outputs text consumption handler. this handler branches by output method
	private void writeTextHandler(TransitionTable table) {
		printSection("text");
		_output.println("public void text(String ___$value) throws SAXException");
		_output.println("{");

        if(_Options.debug) {
            _output.println("runtime.trace(\"text '\"+___$value.trim()+\"' #\" + _ngcc_current_state);");
        }

		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.VALUE_TEXT);
        
        Iterator states = _Info.iterateAllStates();
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
                
                String code = buildTransitionCode(st,tr,"text","___$value");
                if(a.isValueText())
                    bi.addConditionalCode(st, "___$value.equals(\""
                        + a.asValueText().getValue() + "\")", code );
                else {
                    dataPresent = true;
                    bi.addElseCode(st, code );
                }
            }

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null && !dataPresent)
                bi.addElseCode(st,
                    buildTransitionCode(st,tr,"text","___$value"));
        }
        
        String errorHandler = null;
        if(_Options.debug)
            errorHandler = "runtime.traceln(\" ignored\");";
		bi.output(errorHandler);
        
		_output.println("}");   //end of function
	}
    
    private void writeChildCompletedHandler() {
        printSection("child completed");
        _output.println("public void onChildCompleted(Object result, int cookie,boolean needAttCheck) throws SAXException {");
        if(_Options.debug) {
            _output.println("runtime.traceln(\"\");");
            _output.println(MessageFormat.format(
                "runtime.trace(\"onChildCompleted(\"+cookie+\") back to {0}\");",
                new Object[]{
                    _Info.getClassName()
                }));
        }
        _output.println("switch(cookie) {");
        
        
        Set processedTransitions = new HashSet();
        
        Iterator states = _Info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            
            Iterator trans =st.iterateTransitions(Alphabet.REF_BLOCK);
            while(trans.hasNext()) {
                Transition tr = (Transition)trans.next();
                Alphabet.Ref a = tr.getAlphabet().asRef();
                
                if(processedTransitions.add(tr)) {
                    _output.println("case "+tr.getUniqueId()+":");
                    
                    // if there is an alias, assign to that variable
                    String alias = a.getAlias();
                    if(alias!=null) {
                        ScopeInfo childBlock = a.getTargetScope();
        
                        _output.println(MessageFormat.format(
                            "this.{0}=({1})result;",
                            new Object[]{ alias, childBlock.scope.getParam().returnType }));
                    }
                    
                    StringBuffer buf= new StringBuffer();

                    buf.append(tr.invokeEpilogueActions());

                    appendStateTransition(buf, tr.nextState(), "needAttCheck");
                        
                    _output.println(buf);
                    _output.println("return;");
                    
                    _output.println();
                }
            }
        }
        
        _output.println("default:");
        // TODO: assertion failed
        _output.println("    throw new InternalError();");
        
        _output.println("}");   //end of the switch-case
        _output.println("}");   //end of the function
    }
    
    
	private void writeAttributeHandler() {
		printSection("attribute");
		_output.println("public void processAttribute() throws SAXException");
		_output.println("{");
		_output.println("int ai;");
		if(_Options.debug)
			_output.println("runtime.traceln(\"processAttribute (\" + runtime.getCurrentAttributes().getLength() + \" atts) #\" + _ngcc_current_state);"); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.ENTER_ATTRIBUTE);
		
        Iterator states = _Info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            writeAttributeHandler(bi,st,st);
        }
        
        bi.output(null);
        _output.println("}");   //end of function
    }
    
    private void writeAttributeHandler( SwitchBlockInfo bi, State source, State current ) {
        
        Set attHead = current.attHead();
        for(Iterator jtr=attHead.iterator(); jtr.hasNext(); ) {
            Alphabet a = (Alphabet)jtr.next();
            
            if(a.isRef()) {
                writeAttributeHandler( bi, source,
                    a.asRef().getTargetScope().getInitialState() );
            } else {
                writeAttributeHandlerBlock( bi, source, a.asEnterAttribute() );
            }
        }
    }

    private void writeAttributeHandlerBlock( SwitchBlockInfo bi, State st, 
        Alphabet.EnterAttribute a ) {
        
        NameClass nc = a.getKey();
        if(nc instanceof SimpleNameClass) {
            SimpleNameClass snc = (SimpleNameClass)nc;
            
	        bi.addConditionalCode(st,
	            MessageFormat.format(
	            "(ai = runtime.getAttributeIndex(\"{0}\",\"{1}\"))>=0",
	                new Object[]{
	                    snc.nsUri, snc.localName}),
	            "runtime.consumeAttribute(ai);" + _Options.newline);
        } else {
            // if the name class is complex
            throw new UnsupportedOperationException();
        }
    }
    
    private State appendStateTransition(StringBuffer buf, State deststate ) {
        return appendStateTransition(buf,deststate,null);
    }
    
    /**
     * @param flagVarName
     *      If this parameter is non-null, the processAttribute method
     *      should be called if and only if this variable is true.
     */
	// What's the difference of this method and "buildMoveToStateCode"? - Kohsuke
	private State appendStateTransition(StringBuffer buf, State deststate, String flagVarName)
	{
		if(deststate.getThreadIndex()==-1)
			buf.append("_ngcc_current_state=" + deststate.getIndex());
		else
			buf.append("_ngcc_threaded_state[" + deststate.getThreadIndex() + "]="+ deststate.getIndex());
		buf.append(";");
        if(_Options.debug)
        {
            if(deststate.getThreadIndex()==-1)
                buf.append("runtime.traceln(\" -> #" + deststate.getIndex() + "\");");
            else
                buf.append("runtime.traceln(\" -> #[" + deststate.getThreadIndex() + "]"+ deststate.getIndex() + "\");");
            buf.append(_Options.newline);
        }

        if(!deststate.attHead().isEmpty()) {
            if(flagVarName!=null)
                buf.append("if("+flagVarName+")");
            buf.append("processAttribute();");
        }
		
		if(deststate.getMeetingDestination()!=null)
		{
			buf.append(_Options.newline);
			buf.append("if(");
			boolean first = true;
			Iterator it = deststate.getMeetingDestination().iterateStatesForWait();
			while(it.hasNext())
			{
				State s = (State)it.next();
				if(s==deststate) continue;
				if(!first) buf.append(" && ");
				buf.append("_ngcc_threaded_state[" + s.getThreadIndex() + "]=="+ s.getIndex());
				first = false;
			}
			buf.append(") ");
			return appendStateTransition(buf, deststate.getMeetingDestination());
		}
		else
			return deststate;
	}
	
	private void printSection(String title) {
		_output.println();
		_output.print("/* ------------ " + title + " ------------ */");
		_output.println();
	}
}
