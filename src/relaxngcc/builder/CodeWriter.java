/*
 * CodeWriter.java
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
import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.SimpleNameClass;

import relaxngcc.codedom.*;

/**
 * generates Java code that parses XML data via NGCCHandler interface
 */
public class CodeWriter
{
	//utility classes: for switch-case-if structure of each handler
	private class CodeAboutState
	{
		public StatementVector prologue;
        public StatementVector epilogue;
		public IfStatement conditionalCodes;
		public StatementVector elsecode;
		
		public void addConditionalCode(Expression cond, StatementVector code)
		{
			if(conditionalCodes==null) conditionalCodes = new IfStatement(cond, code);
			else conditionalCodes.addClause(cond, code);
		}
        
        public StatementVector output(Statement errorHandleMethod) {
        	StatementVector sv = new StatementVector();
        	
            if(prologue!=null) sv.add(prologue);
            
            //elsecode, null‚È‚çerrorHandleMethod‚Å•Â‚¶‚é
            
            if(elsecode!=null) {
            	if(conditionalCodes!=null)
	            	conditionalCodes.closeClause(elsecode);
	            else
	            	sv.add(elsecode);
            } else if(errorHandleMethod!=null) {
            	if(conditionalCodes!=null)
	            	conditionalCodes.closeClause(new StatementVector(errorHandleMethod));
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
		public void addConditionalCode(State state, Expression cond, StatementVector code) {
			getCAS(state).addConditionalCode(cond,code);
		}
        
		public void addElseCode(State state, StatementVector code) {
			CodeAboutState cas = getCAS(state);
            
			if(cas.elsecode==null)
				cas.elsecode = code;
			else
				cas.elsecode.add(code);
		}
        
		public void addPrologue(State state, Statement code) {
			CodeAboutState cas = getCAS(state);
			if(cas.prologue==null)
				cas.prologue = new StatementVector(code);
			else
				cas.prologue.add(code);
		}
        
        public void addEpilogue(State state, Statement code) {
            CodeAboutState cas = getCAS(state);
            if(cas.epilogue==null)
                cas.epilogue = new StatementVector(code);
            else
                cas.epilogue.add(code);
        }

        private StatementVector output(Statement errorHandleMethod) {
        	StatementVector sv = new StatementVector();
        	IfStatement ifblock = null;
            Iterator i = state2CodeFragment.entrySet().iterator();
            while(i.hasNext())
            {
                Map.Entry e = (Map.Entry)i.next();
                State st = (State)e.getKey();
                
                Expression condition = null;
                if(st.getThreadIndex()==-1)
                    condition = BinaryOperatorExpression.EQ(new VariableExpression("_ngcc_current_state"), new ConstantExpression(st.getIndex()));
                else
                	condition = BinaryOperatorExpression.EQ(
                        new VariableExpression("_ngcc_threaded_state").arrayRef(new ConstantExpression(st.getThreadIndex())),
                        new ConstantExpression(st.getIndex()));
                    
                StatementVector whentrue = ((CodeAboutState)e.getValue()).output(errorHandleMethod);
                
                if(ifblock==null)
                	ifblock = new IfStatement(condition, whentrue);
                else
                	ifblock.addClause(condition, whentrue);
                
            }
            
            if(errorHandleMethod!=null) {
            	if(ifblock!=null) ifblock.closeClause(new StatementVector(errorHandleMethod));
            }
            
            if(ifblock!=null) sv.add(ifblock);
            return sv;
        }
	}
	
	private ScopeInfo _Info;
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
    



    
    
    
	public ClassDefinition output() throws IOException {
		ClassDefinition classdef = _Info.createClassCode(_Options,_Grammar.globalImportDecls);
        
        classdef.addMember(new LanguageSpecificString("private"), TypeDescriptor.STRING, "uri");
        classdef.addMember(new LanguageSpecificString("private"), TypeDescriptor.STRING, "localName");
        classdef.addMember(new LanguageSpecificString("private"), TypeDescriptor.STRING, "qname");
        
        classdef.addMethod(createAcceptedMethod());
        
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
        
        TypeDescriptor[] types = new TypeDescriptor[] { new TypeDescriptor("Attributes") };
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
            CodeWriter wr = new CodeWriter(_Grammar, (ScopeInfo)lambda_scopes.next(), _Options);
            wr.output(out);
        }
*/
		_Info.printTailSection(classdef, _Grammar.globalBody);
		return classdef;
	}
	
    private MethodDefinition createAcceptedMethod()
    {
        Iterator states = _Info.iterateAcceptableStates();
        Expression statecheckexpression = null;
		while(states.hasNext())
		{
            Expression temp = null;
			State s = (State)states.next();
			if(s.getThreadIndex()==-1)
				temp = BinaryOperatorExpression.EQ(new VariableExpression("_ngcc_current_state"), new ConstantExpression(s.getIndex()));
			else
				temp = BinaryOperatorExpression.EQ(
                    new VariableExpression("_ngcc_threaded_state")
                        .arrayRef( new ConstantExpression(s.getThreadIndex())),
                    new ConstantExpression(s.getIndex()));
			
			statecheckexpression = (statecheckexpression==null)? temp : BinaryOperatorExpression.OR(temp, statecheckexpression);
        }
        
        if(statecheckexpression==null) statecheckexpression = new ConstantExpression(false);
        
        MethodDefinition m = new MethodDefinition(new LanguageSpecificString("public"), TypeDescriptor.BOOLEAN, "accepted",null);
        m.body()._return(statecheckexpression);
        return m;
    }

    private MethodDefinition writeEventHandler( TransitionTable table, int type, String eventName ) {
        return writeEventHandler(table,type,eventName,new TypeDescriptor[0],new String[0]);
    }
	
    /**
     * Writes event handlers for (enter|leave)(Attribute|Element) methods.
     */
    private MethodDefinition writeEventHandler( TransitionTable table, int type, String eventName,
        TypeDescriptor[] additionalTypes, String[] additionalArgs ) {

        MethodDefinition method = new MethodDefinition(
            new LanguageSpecificString("public"),
            TypeDescriptor.VOID, eventName,
            new LanguageSpecificString("throws SAXException") );
        
        method.param( TypeDescriptor.STRING, "uri" );
        method.param( TypeDescriptor.STRING, "local" );
        method.param( TypeDescriptor.STRING, "qname" );
        for( int i=0; i<additionalTypes.length; i++ )
            method.param( additionalTypes[i], additionalArgs[i] );
        
        StatementVector sv = method.body();
		//printSection(eventName);
            
        // QUICK HACK
        // copy them to the instance variables so that they can be 
        // accessed from action functions.
        // we should better not keep them at Runtime, because
        // this makes it impossible to emulate events.
        sv.assign(new PropertyReferenceExpression(new VariableExpression("this"), "uri"), new VariableExpression("uri"));
        sv.assign(new PropertyReferenceExpression(new VariableExpression("this"), "localName"), new VariableExpression("localName"));
        sv.assign(new PropertyReferenceExpression(new VariableExpression("this"), "qname"), new VariableExpression("qname"));
            
		if(_Options.debug) {
			sv.invoke(
                new VariableExpression("runtime"), "traceln")
                    .arg(new LanguageSpecificExpression(
                        new LanguageSpecificString("\""+eventName + " \"+qname+\" #\" + _ngcc_current_state")));
        }
        
		SwitchBlockInfo bi = new SwitchBlockInfo(type);
		Expression[] arguments = new Expression[3 + additionalArgs.length];
		arguments[0] = new VariableExpression("uri");
		arguments[1] = new VariableExpression("localName");
		arguments[2] = new VariableExpression("qname");
		for(int i=0; i<additionalArgs.length; i++)
			arguments[3+i] = new VariableExpression(additionalArgs[i]);
		
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
                    new LanguageSpecificExpression((String)a.asMarkup().getKey().apply(new NameTestBuilder("uri","localName"))),
					buildTransitionCode(st,tr,eventName,arguments));
			}

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null)
                bi.addElseCode(st,
                    buildTransitionCode(st,tr,eventName,arguments));
		}
        
        Statement eh = new MethodInvokeExpression("unexpected"+capitalize(eventName))
                .arg(new VariableExpression("qname"));
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
    private StatementVector buildTransitionCode( State current, Transition tr, String eventName, Expression[] additionalparams ) {
	    
	    if(tr==REVERT_TO_PARENT) {
            
            String retValue = _Info.scope.getParam().returnValue;
            String retType  = _Info.scope.getParam().returnType;
            String boxType = getJavaBoxType(retType);
            
            Expression r = new VariableExpression(_Info.scope.getParam().returnValue);
            if(boxType!=null)
                r = new ObjectCreateExpression( new TypeDescriptor(boxType) ).arg(r);
            
	    	StatementVector sv = current.invokeActionsOnExit();
	    	sv.invoke(
	    		new VariableExpression("runtime"),
	    		"revertToParentFrom"+capitalize(eventName))
                    .arg(r)
                    .arg(new VariableExpression("cookie"))
                    .args(additionalparams);
	    	return sv;
        }
        
        if(tr.getAlphabet().isEnterElement()) {
        	StatementVector sv = new StatementVector();
            sv.invoke( new VariableExpression("runtime"), "pushAttributes")
                    .arg(new VariableExpression("attrs"));
            sv.add(buildMoveToStateCode(tr));
            
            return sv;
        }
        
        if(tr.getAlphabet().isText()) {
            StatementVector sv = new StatementVector();
            Alphabet.Text ta = tr.getAlphabet().asText();
            String alias = ta.getAlias();
            if(alias!=null)
                sv.assign(new VariableExpression(alias), new VariableExpression("___$value"));
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
    private StatementVector buildCodeToSpawnChild(String eventName,Transition ref_tr, Expression[] eventParams) {
        
        StatementVector sv = new StatementVector();
        Alphabet.Ref alpha = ref_tr.getAlphabet().asRef();
        ScopeInfo ref_block = alpha.getTargetScope();
        
        sv.add(ref_tr.invokePrologueActions());
        
        /* Caution
         *  alpha.getParams() may return more than one argument concatinated by ','.
         *  But I give it away because separating the string into Expression[] is hard.
         */
        String extraarg = alpha.getParams();
        
        ObjectCreateExpression oe = 
            new ObjectCreateExpression(new TypeDescriptor(ref_block.getClassName()))
                .arg(new VariableExpression("this")) //TODO: literal 'this' is specific to programming language 
                .arg(new VariableExpression("runtime"))
                .arg(new ConstantExpression(ref_tr.getUniqueId()));
        if(extraarg.length()>0)
            oe.arg(new LanguageSpecificExpression(extraarg.substring(1)));
            
        sv.decl(new TypeDescriptor("NGCCHandler"), "h", oe );
        
        
        if(_Options.debug) {
        	Expression msg = new ConstantExpression(MessageFormat.format(
                "Change Handler to {0} (will back to:#{1})",
                new Object[]{
                    ref_block.getClassName(),
                    new Integer(ref_tr.nextState().getIndex())
                }));
                
        	sv.invoke(new VariableExpression("runtime"), "traceln").arg(msg);
        }
        
        sv.invoke( new VariableExpression("runtime"), "spawnChildFrom"+capitalize(eventName))
            .arg(new VariableExpression("h"))
            .args(eventParams);
                
        return sv;
    }

    
    /**
     * Creates code that changes the current state to the nextState
     * of the transition.
     */
    private StatementVector buildMoveToStateCode(Transition tr)
    {
        StatementVector sv = new StatementVector();
        
        sv.add(tr.invokePrologueActions());
        State nextstate = tr.nextState();
        
        if(tr.getDisableState()!=null) {
        	Expression dest;
            if(tr.getDisableState().getThreadIndex()==-1)
                dest = new VariableExpression("_ngcc_current_state");
            else
                dest = new VariableExpression("_ngcc_threaded_state").arrayRef(
                    new ConstantExpression(tr.getDisableState().getThreadIndex()));
            sv.assign(dest, new ConstantExpression(-1));
        }
        
        if(tr.getEnableState()!=null) {
        	Expression dest;
            if(tr.getEnableState().getThreadIndex()==-1)
                dest = new VariableExpression("_ngcc_current_state");
            else
                dest = new VariableExpression("_ngcc_threaded_state").arrayRef(
                    new ConstantExpression(tr.getEnableState().getThreadIndex()));
        
            sv.assign(dest, new ConstantExpression(tr.getEnableState().getIndex()));
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
	private MethodDefinition writeTextHandler(TransitionTable table) {
		//printSection("text");

        MethodDefinition method = new MethodDefinition(
            new LanguageSpecificString("public"),
            TypeDescriptor.VOID,
            "text",
            new LanguageSpecificString("throws SAXException"));
		
        method.param( TypeDescriptor.STRING, "___$value" );
        
		StatementVector sv = method.body();
		
        if(_Options.debug) {
        	sv.invoke( new VariableExpression("runtime"), "trace")
                .arg(new LanguageSpecificExpression(new LanguageSpecificString("\"text '\"+___$value.trim()+\"' #\" + _ngcc_current_state")));
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
                
                StatementVector code = buildTransitionCode(st,tr,"text",new Expression[]{ new VariableExpression("___$value") });
                if(a.isValueText())
                    bi.addConditionalCode(st, 
                        new VariableExpression("___$value").invoke("equals")
                            .arg( new ConstantExpression(a.asValueText().getValue())), code);
                else {
                    dataPresent = true;
                    bi.addElseCode(st, code);
                }
            }

            // if there is EVERYTHING_ELSE transition, add an else clause.
            Transition tr = table.getEverythingElse(st);
            if(tr!=null && !dataPresent)
                bi.addElseCode(st, buildTransitionCode(st,tr,"text",new Expression[]{ new VariableExpression("___$value") }));
        }
        
        Statement errorHandler = null;
        if(_Options.debug)
            errorHandler = new VariableExpression("runtime").invoke("traceln")
                    .arg(new ConstantExpression("ignored"));
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
    
    private String getJavaBoxType( String name ) {
        for( int i=0; i<boxTypes.length; i+=2 )
            if( name.equals(boxTypes[i]) )
                return boxTypes[i+1];
        return null;
    }
    
    private MethodDefinition writeChildCompletedHandler() {
        //printSection("child completed");
        MethodDefinition method = new MethodDefinition(
            new LanguageSpecificString("public"),
            TypeDescriptor.VOID,
            "onChildCompleted",
            new LanguageSpecificString("throws SAXException") );

        method.param( new TypeDescriptor("Object"), "result" );
        method.param( TypeDescriptor.INTEGER, "cookie" );
        method.param( TypeDescriptor.BOOLEAN, "needAttCheck" );
        
        StatementVector sv = method.body();
        
        if(_Options.debug) {
        	sv.invoke(new VariableExpression("runtime"), "traceln" )
                .arg( new LanguageSpecificExpression("\"onChildCompleted(\"+cookie+\") back to "+_Info.getClassName()+"\""));
        }
        
        SwitchStatement switchstatement = new SwitchStatement(new VariableExpression("cookie"));
        
        Set processedTransitions = new HashSet();
        
        Iterator states = _Info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            
            Iterator trans =st.iterateTransitions(Alphabet.REF_BLOCK);
            while(trans.hasNext()) {
                Transition tr = (Transition)trans.next();
                Alphabet.Ref a = tr.getAlphabet().asRef();
                
                if(processedTransitions.add(tr)) {
                    
                    StatementVector block = new StatementVector();
                    // if there is an alias, assign to that variable
                    String alias = a.getAlias();
                    if(alias!=null) {
                        ScopeInfo childBlock = a.getTargetScope();
                        String returnType = childBlock.scope.getParam().returnType;
                        
                        String boxType = getJavaBoxType(returnType);
                        Expression rhs;
                        if(boxType==null)
                            rhs = new CastExpression(
                                new TypeDescriptor(returnType), new VariableExpression("result"));
                        else
                            rhs = new CastExpression( new TypeDescriptor(boxType),
                                new VariableExpression("result")).invoke(returnType+"Value");
                            
                        block.assign(
                        	new PropertyReferenceExpression(new VariableExpression("this"), alias),
                        	rhs);
                                
                    }
                    
                    block.add(tr.invokeEpilogueActions());

                    appendStateTransition(block, tr.nextState(), "needAttCheck");
                        
                    switchstatement.addCase(new ConstantExpression(tr.getUniqueId()), block);
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
    
    
	private MethodDefinition writeAttributeHandler() {
		//printSection("attribute");
		//_output.println("public void processAttribute() throws SAXException");

        MethodDefinition method = new MethodDefinition(
            new LanguageSpecificString("public"),
            TypeDescriptor.VOID,
            "processAttribute",
            new LanguageSpecificString("throws SAXException") );
		
		StatementVector sv = method.body();
        
		sv.decl(TypeDescriptor.INTEGER, "ai");
		if(_Options.debug)
			sv.invoke(new VariableExpression("runtime"), "traceln")
                .arg( new LanguageSpecificExpression("\"processAttribute (\" + runtime.getCurrentAttributes().getLength() + \" atts) #\" + _ngcc_current_state")); 
		
		SwitchBlockInfo bi = new SwitchBlockInfo(Alphabet.ENTER_ATTRIBUTE);
        
        Iterator states = _Info.iterateAllStates();
        while(states.hasNext()) {
            State st = (State)states.next();
            writeAttributeHandler(bi,st,st);
        }
        
        sv.add(bi.output(null));
        
        return method;
    }
    
    private void writeAttributeHandler( SwitchBlockInfo bi, State source, State current ) {
        
        Set attHead = current.attHead();
        for(Iterator jtr=attHead.iterator(); jtr.hasNext(); ) {
            Alphabet a = (Alphabet)jtr.next();
            
            if(a.isRef()) {
                writeAttributeHandler( bi, source, a.asRef().getTargetScope().getInitialState() );
            } else {
                writeAttributeHandlerBlock( bi, source, a.asEnterAttribute() );
            }
        }
    }

    private void writeAttributeHandlerBlock( SwitchBlockInfo bi, State st, Alphabet.EnterAttribute a ) {
        
        NameClass nc = a.getKey();
        if(nc instanceof SimpleNameClass) {
            SimpleNameClass snc = (SimpleNameClass)nc;
            
            Expression condition = new LanguageSpecificExpression(MessageFormat.format(
	            "(ai = runtime.getAttributeIndex(\"{0}\",\"{1}\"))>=0",
	                new Object[]{
	                    snc.nsUri, snc.localName})); //chotto sabori gimi
	       
            StatementVector sv = new StatementVector();
            sv.invoke(new VariableExpression("runtime"), "consumeAttribute")
                .arg(new VariableExpression("ai"));
	        	
	        bi.addConditionalCode(st, condition, sv);
        } else {
            // if the name class is complex
            throw new UnsupportedOperationException(
                "attribute with a complex name class is not supported yet  name class:"+nc.toString());
        }
    }
    
    private State appendStateTransition(StatementVector sv, State deststate ) {
        return appendStateTransition(sv,deststate,null);
    }
    
    /**
     * @param flagVarName
     *      If this parameter is non-null, the processAttribute method
     *      should be called if and only if this variable is true.
     */
	// What's the difference of this method and "buildMoveToStateCode"? - Kohsuke
	private State appendStateTransition(StatementVector sv, State deststate, String flagVarName)
	{
		
		Expression statevariable = null;
		if(deststate.getThreadIndex()==-1)
			statevariable = new VariableExpression("_ngcc_current_state");
		else
			statevariable = new VariableExpression("_ngcc_threaded_state")
                .arrayRef( new ConstantExpression(deststate.getThreadIndex()));
		
		sv.assign(statevariable, new ConstantExpression(deststate.getIndex()));
		
		if(_Options.debug)
        {
        	String trace;	
            if(deststate.getThreadIndex()==-1)
                trace = "-> #" + deststate.getIndex();
            else
                trace = "-> #[" + deststate.getThreadIndex() + "]"+ deststate.getIndex();

        	sv.invoke( new VariableExpression("runtime"), "traceln" )
                .arg( new ConstantExpression(trace));
               
        }

        if(!deststate.attHead().isEmpty()) {
        	
        	Statement processAttribute = new MethodInvokeExpression("processAttribute");
            if(flagVarName!=null)
                sv.add(new IfStatement(new VariableExpression(flagVarName), new StatementVector(processAttribute)));
            else
	            sv.add(processAttribute);
        }
		
		if(deststate.getMeetingDestination()!=null)
		{
			Expression condition = null;
			Iterator it = deststate.getMeetingDestination().iterateStatesForWait();
			while(it.hasNext())
			{
				State s = (State)it.next();
				if(s==deststate) continue;
				
				Expression t = BinaryOperatorExpression.EQ(
					new VariableExpression("_ngcc_threaded_state")
                        .arrayRef( new ConstantExpression(s.getThreadIndex())),
					new ConstantExpression(s.getIndex()));
				
				condition = condition==null? t : BinaryOperatorExpression.AND(condition, t);
			}
			
			StatementVector whentrue = new StatementVector();
			State t = appendStateTransition(whentrue, deststate.getMeetingDestination());
			if(condition==null)
				sv.add(whentrue);
			else
				sv.add(new IfStatement(condition, whentrue));
			return t;
		}
		else
			return deststate;
	}
}
