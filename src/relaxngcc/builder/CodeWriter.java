/*
 * CodeWriter.java
 *
 * Created on 2001/08/05, 14:34
 */

package relaxngcc.builder;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map;
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
        writeAcceptableStates();
		writeStartElementHandler();
		writeEndElementHandler();
		writeTextHandler();
		writeAttributeHandler();
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
        _output.println(";");
        _output.println("}"); //end of method
    }
	
	private void writeStartElementHandler()
	{
		Iterator states = _Info.iterateStatesHavingStartElementOrRef();
		printSection("enterElement");
		_output.println("public void enterElement(String uri,String localname,String qname) throws SAXException");
		_output.println("{");
		if(_Options.debug)
			_output.println("System.err.println(\"enterElement " + _Info.getNameForTargetLang() + ":\" + qname + \",state=\" + _ngcc_current_state);");
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.START_ELEMENT);
		
		while(states.hasNext())
		{
			//normal transitions by startElement type alphabets
			State st = (State)states.next();
			Iterator transitions = st.iterateStartElementTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				bi.addConditionalCode(st, 
					a.getKey().createJudgementClause(_Info, "uri", "localname"),
					transitionCode(tr));
			}
			
			//ref elements
			transitions = st.iterateRefTransitions();
			while(transitions.hasNext())
			{
				Transition ref_tr = (Transition)transitions.next();
				ScopeInfo ref_block = _Grammar.getScopeInfoByName(ref_tr.getAlphabet().getValue());
				Iterator first_alphabets = ref_block.iterateFirstAlphabets();
				boolean first_clause = true;
				String clause = "";
				while(first_alphabets.hasNext())
				{
					Alphabet a = (Alphabet)first_alphabets.next();
					if(a.getType()!=Alphabet.START_ELEMENT) continue;
					if(!first_clause) clause += "|| ";
					clause += "(" +  a.getKey().createJudgementClause(_Info, "uri", "localname") + ") ";
					first_clause = false;
				}
				
				if(!first_clause)
				{
					StringBuffer code = new StringBuffer();
					if(ref_tr.getAction()!=null) code.append(ref_tr.getAction());
					appendStateTransition(code, ref_tr.nextState());
					String handler_name = ref_tr.getAlphabet().getAlias();
					if(handler_name==null)
					{
						String typename = (_Options.style==Options.STYLE_MSV? "NGCCTypedContentHandler" : "NGCCPlainHandler");
						code.append(typename + " h = new " + ref_block.getNameForTargetLang() + "(_ngcc_reader, this);" + _Options.newline);
						handler_name = "h";
					}
					else
						code.append(handler_name + " = new " + ref_block.getNameForTargetLang() + "(_ngcc_reader, this);" + _Options.newline);
						
					if(_Options.debug)
						code.append("System.err.println(\"Change Handler " + ref_block.getNameForTargetLang() + "\");" + _Options.newline);
					
					code.append("setupNewHandler(" + handler_name + ", uri, localname, qname);" + _Options.newline);
					code.append(_Options.newline);
					if(ref_tr.nextState().hasAttributeTransition())
						code.append("processAttribute();");
					
					bi.addConditionalCode(st, clause, code.toString());
				}
			}
			
		}

		//end of scope
		states = _Info.iterateAcceptableStates();
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator follows = _Info.iterateFollowAlphabets();
			while(follows.hasNext())
			{
				Alphabet f = (Alphabet)follows.next();
				if(f.getType()==Alphabet.START_ELEMENT)
				{
					String action = "resetHandlerByStart(uri,localname,qname);";
					if(st.getActionOnExit()!=null) action = st.getActionOnExit() + action;
					bi.addConditionalCode(st, f.getKey().createJudgementClause(_Info, "uri", "localname"), action);
				}
			}
		}
		
		outputSwitchBlock(bi);
		
		_output.println("}");   //end of method
	}
	private void writeEndElementHandler()
	{
		Iterator states = _Info.iterateStatesHavingEndElement();
		printSection("leaveElement");
		_output.println("public void leaveElement(String uri,String localname,String qname) throws SAXException");
		_output.println("{");
		if(_Options.debug)
			_output.println("System.err.println(\"" + _Info.getNameForTargetLang() + ":leaveElement \" + qname + \",state=\" + _ngcc_current_state);"); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.END_ELEMENT);
		
		//normal transitions by endElement type transitions
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator transitions = st.iterateEndElementTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				bi.addConditionalCode(st, a.getKey().createJudgementClause(_Info, "uri", "localname"), transitionCode(tr));
			}
		}
		
		//end of scope
		states = _Info.iterateAcceptableStates();
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator follows = _Info.iterateFollowAlphabets();
			while(follows.hasNext())
			{
				Alphabet f = (Alphabet)follows.next();
				if(f.getType()==Alphabet.END_ELEMENT)
				{
					String action = "resetHandlerByEnd(uri,localname,qname);";
					if(_Options.debug) action = "System.out.println(\"reset handler\"); " + action;
					if(st.getActionOnExit()!=null) action = st.getActionOnExit() + action;
					bi.addConditionalCode(st, f.getKey().createJudgementClause(_Info, "uri", "localname"), action);
				}
			}
		}
		
		outputSwitchBlock(bi);
		
		_output.println("}");   //end of function
	}
	
	//outputs text consumption handler. this handler branches by output method
	private void writeTextHandler()
	{
		switch(_Options.style)
		{
			case Options.STYLE_MSV:
				writeMSVTextHandler();
				break;
			case Options.STYLE_TYPED_SAX:
				writeTypedSAXTextHandler();
				break;
			case Options.STYLE_PLAIN_SAX:
				writePlainSAXTextHandler();
				break;
		}
	}
	private void writeTypedSAXTextHandler()
	{
		Iterator states = _Info.iterateStatesHavingText();
		printSection("text");
		_output.println("public void text(String value) throws SAXException");
		_output.println("{");
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.TYPED_VALUE);
		
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator transitions = st.iterateTextTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				StringBuffer buf = new StringBuffer();
				String alias = tr.getAlphabet().getAlias();
				if(a.getType()==Alphabet.FIXED_VALUE)
				{
					if(alias!=null)
						buf.append(alias + "=value;");
					buf.append(transitionCode(tr));
					bi.addConditionalCode(st, "value.equals(\"" + a.getValue() + "\")", buf.toString());
				}
				else	
				{
					MetaDataType mdt = tr.getAlphabet().getMetaDataType();
					if(mdt==MetaDataType.STRING) // case of string type
					{
						if(alias!=null)
							buf.append(alias + "=value;");
						buf.append(transitionCode(tr));
						bi.addElseCode(st, buf.toString());
					}
					else
					{
						int typeindex = mdt.getIndex();
						if(alias!=null)
							buf.append(alias + "=(" + mdt.getJavaTypeName() + ")DataTypes.dt[" + typeindex + "].createJavaObject(value,null);");
						buf.append(transitionCode(tr));
						bi.addConditionalCode(st, "DataTypes.dt[" + typeindex + "].isValid(value,null)", buf.toString());
					}
				}
			}
		}
		outputSwitchBlock(bi);
		_output.println("}");   //end of function
	}
	private void writeMSVTextHandler()
	{
		Iterator states = _Info.iterateStatesHavingText();
		printSection("text");
		_output.println("public void text(String value, XSDatatype type) throws SAXException");
		_output.println("{");
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.TYPED_VALUE);
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator transitions = st.iterateTextTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				StringBuffer buf = new StringBuffer();
				String alias = tr.getAlphabet().getAlias();
				
				if(a.getType()==Alphabet.FIXED_VALUE)
				{
					if(alias!=null)
						buf.append(alias + "=" + a.getKey() + ";");
					buf.append(transitionCode(tr));
					bi.addConditionalCode(st, "value.equals(\"" + a.getValue() + "\")", buf.toString());
				}
				else	
				{
					MetaDataType mdt = tr.getAlphabet().getMetaDataType();
					if(alias!=null)
						buf.append(alias + "=(" + mdt.getJavaTypeName() + ")type.createJavaObject(value,null);");
					buf.append(transitionCode(tr));
					bi.addElseCode(st, buf.toString());
				}
			}
		}
		outputSwitchBlock(bi);
		_output.println("}");   //end of function
	}
	private void writePlainSAXTextHandler()
	{
		Iterator states = _Info.iterateStatesHavingText();
		printSection("text");
		_output.println("public void text(String value) throws SAXException");
		_output.println("{");
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.TYPED_VALUE);
		while(states.hasNext())
		{
			State st = (State)states.next();
			Iterator transitions = st.iterateTextTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				StringBuffer buf = new StringBuffer();
				String alias = tr.getAlphabet().getAlias();
				if(alias!=null)
					buf.append(alias + "=value;");
				buf.append(transitionCode(tr));
				
				if(a.getType()==Alphabet.FIXED_VALUE)
					bi.addConditionalCode(st, "value.equals(\"" + a.getValue() + "\")", buf.toString());
				else	
					bi.addElseCode(st, buf.toString());
			}
		}
		outputSwitchBlock(bi);
		_output.println("}");   //end of function
	}
	private void writeAttributeHandler()
	{
		Iterator states = _Info.iterateAllStates();
		printSection("attribute");
		_output.println("public void processAttribute() throws SAXException");
		_output.println("{");
		_output.println("int __attr_index;");
		if(_Options.debug)
			_output.println("System.err.println(\"" + _Info.getNameForTargetLang() + ":processAttribute \" + currentAttrs().getLength() + \",\" + _ngcc_current_state);"); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.START_ATTRIBUTE);
		
		while(states.hasNext())
		{
			State st = (State)states.next();
			if(!needToCheckAttribute(st)) continue;
			
			Iterator transitions = st.iterateAttributeTransitions();
			while(transitions.hasNext())
			{
				Transition tr = (Transition)transitions.next();
				Alphabet a = tr.getAlphabet();
				
				StringBuffer buf = new StringBuffer();
				bi.addPrologue(st, "__attr_index = getAttributeIndex("+ _Info.getNSStringConstant(a.getKey().getNSURI()) +", \"" + a.getKey().getName() + "\");" + _Options.newline);
				buf.append(transitionCode(tr));
				buf.append("consumeAttribute(__attr_index);" + _Options.newline);
				bi.addConditionalCode(st, "__attr_index>=0", buf.toString());
			}
			
			//ref elements
			transitions = st.iterateRefTransitions();
			while(transitions.hasNext())
			{
				Transition ref_tr = (Transition)transitions.next();
				ScopeInfo destscope = _Grammar.getScopeInfoByName(ref_tr.getAlphabet().getValue());
				if(destscope.containsFirstAttributeAlphabet())
				{
					StringBuffer clause = new StringBuffer();
					StringBuffer action = new StringBuffer();
					writeHavingAttributeCheckCode(destscope, destscope.iterateFirstAlphabets(), clause);
					if(_Options.debug)
						action.append("System.err.println(\"Change Handler " + destscope.getNameForTargetLang() + "\");" + _Options.newline);
					
					appendStateTransition(action, ref_tr.nextState());
					String handler_name = ref_tr.getAlphabet().getAlias();
					if(handler_name==null)
					{
						String typename = (_Options.style==Options.STYLE_MSV? "NGCCTypedContentHandler" : "NGCCPlainHandler");
						action.append(typename + " h = new " + destscope.getNameForTargetLang() + "(_ngcc_reader, this);" + _Options.newline);
						handler_name = "h";
					}
					else
						action.append(handler_name + " = new " + destscope.getNameForTargetLang() + "(_ngcc_reader, this);" + _Options.newline);
						
					action.append("setupNewHandler(" + handler_name + ");" + _Options.newline);
					bi.addConditionalCode(st, clause.toString(), action.toString());
				}
			}
		}
		
		outputSwitchBlock(bi);
		
		_output.println("}");   //end of function
	}
	
	private String transitionCode(Transition tr/*, boolean output_process_attribute*/)
	{
		StringBuffer buf = new StringBuffer();
		
		if(tr.getAction()!=null)
			buf.append(tr.getAction());
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
				buf.append("_tokenizeText=true;");
			else if(result.getListMode()==State.LISTMODE_OFF)
				buf.append("_tokenizeText=false;");
		}
		
		boolean in_if_block = false;
		//in accept state, try unwinding
		if(result.isAcceptable() && _Info.containsFollowAttributeAlphabet())
		{
			buf.append("if(");
			writeHavingAttributeCheckCode(_Info, _Info.iterateFollowAlphabets(), buf);
			buf.append(") resetHandlerByAttr();");
			buf.append(_Options.newline);
			in_if_block = true;
		}
		
		if(needToCheckAttribute(nextstate))
		{
			if(in_if_block) buf.append("else ");
			buf.append("processAttribute();" + _Options.newline);
		}
								
		return buf.toString();
	}
    private boolean needToCheckAttribute(State s)
    {
    	if(s.hasAttributeTransition()) return true;
    	Iterator it = s.iterateRefTransitions();
    	while(it.hasNext())
    	{
    		Transition tr = (Transition)it.next();
			ScopeInfo sc = _Grammar.getScopeInfoByName(tr.getAlphabet().getValue());
			if(sc.containsFirstAttributeAlphabet()) return true;
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
	
	private void outputSwitchBlock(SwitchBlockInfo bi)
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
			}
			
			if(flag && (bi.getType()==Alphabet.START_ELEMENT || bi.getType()==Alphabet.END_ELEMENT))
				_output.println("else this.throwUnexpectedElementException(qname);");
			_output.println("}");
			_output.println();
			first = false;
		}
		
		if(bi.getType()==Alphabet.START_ELEMENT || bi.getType()==Alphabet.END_ELEMENT)
		{
			if(first)
				_output.println("this.throwUnexpectedElementException(qname);");
			else
				_output.println("else this.throwUnexpectedElementException(qname);");
		}
	}
	
	private static void writeHavingAttributeCheckCode(ScopeInfo sci, Iterator alphabets, StringBuffer code)
	{
		boolean first = true;
		while(alphabets.hasNext())
		{
			Alphabet a = (Alphabet)alphabets.next();
			if(a.getType()!=Alphabet.START_ATTRIBUTE) continue;
			
			if(!first) code.append(" || ");
			code.append("getAttributeIndex(");
			code.append(sci.getNSStringConstant(a.getKey().getNSURI()));
			code.append(", \"");
			code.append(a.getKey().getName());
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
