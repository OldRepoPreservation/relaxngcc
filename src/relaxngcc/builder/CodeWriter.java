/*
 * CodeWriter.java
 *
 * Created on 2001/08/05, 14:34
 */

package relaxngcc.builder;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.NGCCGrammar;
import relaxngcc.Options;
import relaxngcc.MetaDataType;

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
		public Vector conditionalCodes;
		public String elsecode;
		
		public void addConditionalCode(String cond, String code)
		{
			if(conditionalCodes==null) conditionalCodes = new Vector();
			conditionalCodes.add(new ConditionalCode(cond, code));
		}
	}
	private class SwitchBlockInfo
	{
		public Map state2CodeFragment;
		
		private int _Type; //one of the constants in Alphabet class
		public int getType() { return _Type; }
		public SwitchBlockInfo(int type)
		{
			_Type=type;
			state2CodeFragment = new TreeMap();
		}
		//if "cond" is "", "code" is put with no if-else clause. this behavior is not smart...
		public void addConditionalCode(State state, String cond, String code)
		{
			Object o = state2CodeFragment.get(state);
			if(o==null)
			{
				CodeAboutState cas = new CodeAboutState();
				cas.addConditionalCode(cond, code);
				state2CodeFragment.put(state, cas);
			}
			else
				((CodeAboutState)o).addConditionalCode(cond, code);
		}
		public void addElseCode(State state, String code)
		{
			CodeAboutState cas = (CodeAboutState)state2CodeFragment.get(state);
			if(cas==null)
			{
				cas = new CodeAboutState();
				cas.elsecode = code;
				state2CodeFragment.put(state, cas);
			}
			else
			{
				if(cas.elsecode==null)
					cas.elsecode = code;
				else
					cas.elsecode.concat(code);
			}
		}
		public void addPrologue(State state, String code)
		{
			CodeAboutState cas = (CodeAboutState)state2CodeFragment.get(state);
			if(cas==null)
			{
				cas = new CodeAboutState();
				cas.prologue = code;
				state2CodeFragment.put(state, cas);
			}
			else
			{
				if(cas.prologue==null)
					cas.prologue = code;
				else
					cas.prologue.concat(code);
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
	public void output(PrintStream out)
	{
		_output = out;
		_Info.printHeaderSection(out, _Options, _Grammar.getGlobalImport());
        
        out.println("private String uri,localName,qname;");
        
        writeAcceptableStates();
        
        writeEventHandler(Alphabet.ENTER_ELEMENT,   "enterElement" );
        writeEventHandler(Alphabet.LEAVE_ELEMENT,     "leaveElement");
        writeEventHandler(Alphabet.ENTER_ATTRIBUTE, "enterAttribute" );
        writeEventHandler(Alphabet.LEAVE_ATTRIBUTE,   "leaveAttribute");
    
		writeTextHandler();
		writeAttributeHandler();
        writeChildCompletedHandler();
        
        Iterator lambda_scopes = _Info.iterateChildScopes();
        while(lambda_scopes.hasNext())
        {
            CodeWriter wr = new CodeWriter(_Grammar, (ScopeInfo)lambda_scopes.next(), _Options);
            wr.output(out);
        }
		_Info.printTailSection(_Grammar.getGlobalBody(), out);
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
	
    
    private void writeEventHandler( int type, String eventName ) {
        
		Iterator states = _Info.iterateStatesHaving(type|Alphabet.REF_BLOCK);// /*HavingStartElementOrRef*/(type);
        
		printSection(eventName);
		_output.println(MessageFormat.format(
            "public void {0}(String uri,String localName,String qname) throws SAXException '{'",
            new Object[]{eventName}));
        // copy them to the instance variables so that they can be 
        // accessed from action functions.
        // we should better not keep them at Runtime, because
        // this makes it impossible to emulate events.
        _output.println("this.uri=uri;");
        _output.println("this.localName=localName;");
        _output.println("this.qname=qname;");
            
		if(_Options.debug)
			_output.println("System.err.println(\""+eventName+" " + _Info.getNameForTargetLang() + ":\" + qname + \",state=\" + _ngcc_current_state);");
		
		SwitchBlockInfo bi = new SwitchBlockInfo(type);
		
		while(states.hasNext()) {
			//normal transitions by startElement type alphabets
			State st = (State)states.next();
			Iterator transitions = st.iterateTransitions(type); // iterateStartElementTransitions();
			
            while(transitions.hasNext()) {
				Transition tr = (Transition)transitions.next();
				Alphabet.Markup a = tr.getAlphabet().asMarkup();
				bi.addConditionalCode(st, 
					a.getKey().createJudgementClause(_Info, "uri", "localName"),
					transitionCode(tr));
			}
			
			//ref elements
			transitions = st.iterateTransitions(Alphabet.REF_BLOCK);
			while(transitions.hasNext())
			{
				Transition ref_tr = (Transition)transitions.next();
				ScopeInfo ref_block = ref_tr.getAlphabet().asRef().getTargetScope();
				boolean first_clause = true;
				String clause = "";
                Iterator first_alphabets = ref_block.iterateFirstAlphabets(type);
				while(first_alphabets.hasNext())
				{
					Alphabet.Markup a = (Alphabet.Markup)first_alphabets.next();
					if(!first_clause) clause += "|| ";
					clause += "(" +  a.getKey().createJudgementClause(_Info, "uri", "localName") + ") ";
					first_clause = false;
				}
				
				if(!first_clause)
					bi.addConditionalCode(st, clause, 
                        buildCodeToSpawnChild(eventName,ref_tr,
                            "uri,localName,qname"));
			}
			
		}

		//end of scope
		states = _Info.iterateAcceptableStates();
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator follows = _Info.iterateFollowAlphabets(type);
			while(follows.hasNext())
			{
				Alphabet.Markup f = (Alphabet.Markup)follows.next();
                String action = MessageFormat.format(
                    "runtime.revertToParentFrom{0}({1},cookie, uri,localName,qname);",
                    new Object[]{
                        capitalize(eventName),
                        _Info.getReturnVariable(),
                    });
                    
                action = st.invokeActionsOnExit()+action;
                
				bi.addConditionalCode(
                    st,
                    f.getKey().createJudgementClause(_Info, "uri", "localName"),
                    action);
			}
		}
		
		outputSwitchBlock(bi,"unexpected"+capitalize(eventName));
		
		_output.println("}");   //end of method
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
                ref_block.getNameForTargetLang(),
                new Integer(ref_tr.getUniqueId()),
                alpha.getParams(),
                _Options.newline}));
            
        if(_Options.debug)
            code.append("System.err.println(\"Change Handler " + ref_block.getNameForTargetLang() + "\");" + _Options.newline);
        
        code.append(MessageFormat.format(
            "runtime.spawnChildFrom{0}(h,{1});\n",
            new Object[]{
                capitalize(eventName),
                eventParams}));
                
        code.append(_Options.newline);
        
        return code.toString();
    }
    
    /** Capitalizes the first character. */
    private String capitalize( String name ) {
        return Character.toUpperCase(name.charAt(0))+name.substring(1);
    }
    
	
	//outputs text consumption handler. this handler branches by output method
	private void writeTextHandler() {
		Iterator states = _Info.iterateStatesHaving(
            Alphabet.VALUE_TEXT|Alphabet.DATA_TEXT|Alphabet.REF_BLOCK ); //Text();
            
		printSection("text");
		_output.println("public void text(String value) throws SAXException");
		_output.println("{");
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.VALUE_TEXT);
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator transitions = st.iterateTransitions(Alphabet.VALUE_TEXT|Alphabet.DATA_TEXT);
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet.Text a = tr.getAlphabet().asText();
				StringBuffer buf = new StringBuffer();
				String alias = a.getAlias();
				if(alias!=null)
					buf.append(alias + "=value;");
				buf.append(transitionCode(tr));
				
				if(a.isValueText())
					bi.addConditionalCode(st, "value.equals(\""
                        + a.asValueText().getValue() + "\")", buf.toString());
				else	
					bi.addElseCode(st, buf.toString());
            }
            
            
            //ref elements
            transitions = st.iterateTransitions(Alphabet.REF_BLOCK);
            while(transitions.hasNext())
            {
                Transition ref_tr = (Transition)transitions.next();
                ScopeInfo ref_block = ref_tr.getAlphabet().asRef().getTargetScope();
                
                if(ref_block.hasFirstAlphabet(Alphabet.DATA_TEXT)
                || ref_block.hasFirstAlphabet(Alphabet.VALUE_TEXT)) {
                    bi.addPrologue(st,
                        buildCodeToSpawnChild("text",ref_tr,
                            "value"));
                    break;
                }
            }
		}
		outputSwitchBlock(bi,null);
		_output.println("}");   //end of function
	}
    
    /**
     * Returns true if the specified state can be reached by
     * a Ref transition.
     */
    private boolean hasReverseRef( State s ) {
        Iterator i = s.iterateReversalTransitions();
        while(i.hasNext()) {
            Transition t = (Transition)i.next();
            if(t.getAlphabet().getType()==Alphabet.REF_BLOCK)
                return true;
        }
        return false;
    }
    
    private void writeChildCompletedHandler() {
        printSection("child completed");
        _output.println("public void onChildCompleted(Object result, int cookie) throws SAXException {");
        if(_Options.debug)
            _output.println("\tSystem.out.println(\"onChildCompleted(\"+cookie+\")\");");
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
                            new Object[]{ alias, childBlock.getReturnType() }));
                    }
                    
                    StringBuffer buf= new StringBuffer();

                    buf.append(tr.invokeEpilogueActions());

                    appendStateTransition(buf, tr.nextState());
                    
                    if(tr.nextState().hasTransition(Alphabet.ENTER_ATTRIBUTE))
                        _output.println("processAttribute();");
                        
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
    
	private void writeAttributeHandler()
	{
		Iterator states = _Info.iterateAllStates();
		printSection("attribute");
		_output.println("public void processAttribute() throws SAXException");
		_output.println("{");
		_output.println("int ai;");
		if(_Options.debug)
			_output.println("System.err.println(\"" + _Info.getNameForTargetLang() + ":processAttribute \" + runtime.getCurrentAttributes().getLength() + \",\" + _ngcc_current_state);"); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.ENTER_ATTRIBUTE);
		
		while(states.hasNext())
		{
			State st = (State)states.next();
			if(!needToCheckAttribute(st)) continue;
			
			Iterator transitions = st.iterateTransitions(Alphabet.ENTER_ATTRIBUTE);
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet.EnterAttribute a = tr.getAlphabet().asEnterAttribute();
				
				
                bi.addConditionalCode(st,
                    MessageFormat.format(
                    "(ai = runtime.getAttributeIndex({0},\"{1}\"))>=0",
                        new Object[]{
                            _Info.getNSStringConstant(a.getKey().getNSURI()),
                            a.getKey().getName()}),
                    "runtime.consumeAttribute(ai);" + _Options.newline);
			}
		}
		
        // TODO: end of scope handling.
        
		outputSwitchBlock(bi,null);
		
		_output.println("}");   //end of function
	}

	
	private String transitionCode(Transition tr/*, boolean output_process_attribute*/)
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
		if(_Options.debug)
		{
			if(result.getThreadIndex()==-1)
				buf.append("System.err.println(\"state " + result.getIndex() + "\");");
			else
				buf.append("System.err.println(\"state [" + result.getThreadIndex() + "]"+ result.getIndex() + "\");");
			buf.append(_Options.newline);
		}
		if(_Options.style!=Options.STYLE_MSV)
		{
			if(result.getListMode()==State.LISTMODE_ON)
				buf.append("runtime.setListMode();"); // _tokenizeText=true;");
//  TODO: why do we need this?
//			else if(result.getListMode()==State.LISTMODE_OFF)
//				buf.append("_tokenizeText=false;");
		}
		
		boolean in_if_block = false;
/*		//in accept state, try unwinding
		if(result.isAcceptable() && _Info.containsFollowAttributeAlphabet())
		{
			buf.append("if(");
			writeHavingAttributeCheckCode(_Info, _Info.iterateFollowAlphabets(), buf);
			buf.append(") resetHandlerByAttr();");
			buf.append(_Options.newline);
			in_if_block = true;
		}
*/		
		if(needToCheckAttribute(nextstate))
		{
			if(in_if_block) buf.append("else ");
			buf.append("processAttribute();" + _Options.newline);
		}
								
		return buf.toString();
	}
    private boolean needToCheckAttribute(State s)
    {
    	if(s.hasTransition(Alphabet.ENTER_ATTRIBUTE)) return true;
    	Iterator it = s.iterateTransitions(Alphabet.REF_BLOCK);
    	while(it.hasNext())
    	{
    		Transition tr = (Transition)it.next();
			ScopeInfo sc = tr.getAlphabet().asRef().getTargetScope();
            if(sc.hasFirstAlphabet(Alphabet.LEAVE_ATTRIBUTE)) return true;
    	}
    	return false;
    }
    
	
	private State appendStateTransition(StringBuffer buf, State deststate)
	{
		if(deststate.getThreadIndex()==-1)
			buf.append("_ngcc_current_state=" + deststate.getIndex());
		else
			buf.append("_ngcc_threaded_state[" + deststate.getThreadIndex() + "]="+ deststate.getIndex());
		buf.append(";");
		
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
	
	private void outputSwitchBlock(SwitchBlockInfo bi, String errorHandleMethod)
	{
		boolean first = true;
		Iterator i = bi.state2CodeFragment.entrySet().iterator();
		while(i.hasNext())
		{
			Map.Entry e = (Map.Entry)i.next();
			State st = (State)e.getKey();
			if(!first) _output.print("else ");
			if(st.getThreadIndex()==-1)
				_output.println("if(_ngcc_current_state==" + st.getIndex()+") {");
			else
				_output.println("if(_ngcc_threaded_state[" + st.getThreadIndex() + "]=="+ st.getIndex()+") {");
				
			CodeAboutState cas = (CodeAboutState)e.getValue();
			if(cas.prologue!=null) _output.println(cas.prologue);
			
			boolean flag = false;
			if(cas.conditionalCodes!=null)
			{
				Iterator ccs = cas.conditionalCodes.iterator();
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
			
			if(cas.elsecode!=null)
			{
				if(flag) _output.println("else {"); else _output.println("{");
				//_output.println(_Options.newline);
				_output.println(cas.elsecode);
				//_output.println(_Options.newline);
				_output.println("}");
                flag=true;
			}
			
            if(flag && errorHandleMethod!=null) {
                 _output.print("else ");
                _output.println(errorHandleMethod+"(qname);");
            }
            
			_output.println("}");
			_output.println();
			first = false;
		}
		
        if(errorHandleMethod!=null) {
    		if(!first)    _output.print("else ");
    		_output.println(errorHandleMethod+"(qname);");
        }
	}
	
	private static void writeHavingAttributeCheckCode(ScopeInfo sci, Iterator alphabets, StringBuffer code)
	{
		boolean first = true;
		while(alphabets.hasNext())
		{
			Alphabet _a = (Alphabet)alphabets.next();
            if(!_a.isEnterAttribute())   continue;
            
            NameClass name = _a.asEnterAttribute().getKey();
			
			if(!first) code.append(" || ");
			code.append("runtime.getAttributeIndex(");
			code.append(sci.getNSStringConstant(name.getNSURI()));
			code.append(", \"");
			code.append(name.getName());
			code.append("\")!=-1");
			first = false;
		}
	}
	
	private void printSection(String title)
	{
		_output.println();
		_output.print("/* ------------ " + title + " ------------ */");
		_output.println();
	}
}
