/*
 * ScopeBuilder.java
 *
 * Created on 2001/08/04, 22:15
 */

package relaxngcc.builder;
import java.util.Iterator;

import relaxngcc.MetaDataType;
import relaxngcc.NGCCGrammar;
import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;
import relaxngcc.grammar.*;

/**
 * A ScopeBuilder constructs an automaton from a given root Element of scope in target grammar. 
 */
public class ScopeBuilder implements PatternFunction
{
	private int _ThreadCount;

    /**
     * Used to give order numbers to EnterAttribute alphabets.
     */
    private int _OrderCounter;
    
    /** actions are added to this buffer until it is processed */
    private StringBuffer preservedAction = new StringBuffer();

	
	
    /** Builds ScopeInfo. */
    public static void build( NGCCGrammar grammar, ScopeInfo scope ) {
        new ScopeBuilder(grammar,scope).build();
    }
    
    private ScopeBuilder( NGCCGrammar grammar, ScopeInfo scope ) {
        this.grammar = grammar;
        this._ScopeInfo = scope;
    }
    
    private final NGCCGrammar grammar;
    private final ScopeInfo _ScopeInfo;
    
	public void build() {
		ctx = new ScopeBuildingContext();
		//starts from final state
	    destination = createState(null);
		destination.setAcceptable(true);

        State initial = (State)_ScopeInfo.scope.getPattern().apply(this);		
//		State initial = null;
//        if(_Type==TYPE_NORMAL || _Type==TYPE_ROOT)
//    		initial = traverseNodeList(_Root.getChildNodes(), ctx, finalstate);
//		else
//			initial = processRelaxNGNode(_Root, ctx, finalstate);
		
        _ScopeInfo.setThreadCount(_ThreadCount);
        // TODO: don't we need to reset the preservedAction variable? - Kohsuke
		_ScopeInfo.setInitialState(initial,
            (preservedAction.length()!=0)?
                _ScopeInfo.createAction(preservedAction):
                null);
        
        _ScopeInfo.copyAttributeHandlers();
        
        _ScopeInfo.minimizeStates();
	}
	

    private ScopeBuildingContext ctx;
    private State destination;
    
    public Object element( ElementPattern pattern ) {
        NameClass nc = pattern.name;
        
        State tail = createState(pattern);
        Transition te = createTransition(new Alphabet.LeaveElement(nc), destination);
        addAction(te,true);
        if(ctx.getInterleaveBranchRoot()!=null) te.setEnableState(ctx.getInterleaveBranchRoot());
        tail.addTransition(te);
        
        // start a new context
        ScopeBuildingContext oldContext = ctx;
        ctx = new ScopeBuildingContext(ctx);
        
        ctx.setInterleaveBranchRoot(null);

        // process descendants
        destination = tail;
        State middle = (State)pattern.body.apply(this);
        
        oldContext = ctx;
        
        State head = createState(pattern);
        Transition ts = createTransition(new Alphabet.EnterElement(nc), middle);
        addAction(ts,true);
        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());
        head.addTransition(ts);
        
        return head;
    }


    public Object attribute( AttributePattern pattern ) {
        NameClass nc = pattern.name;
        State orgdest = destination;

        // I think I broke the code related to interleave handling.
        // I just don't know how to fix them.  - Kohsuke
        
        State tail = createState(pattern);
        Transition te = createTransition(new Alphabet.LeaveAttribute(nc),
            destination /*createState(exp,ctx)*/);
        addAction(te,true);
        tail.addTransition(te);
  
        destination = tail;
        State middle = (State)pattern.body.apply(this);      
//        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*/, tail);
        
        State head = createState(pattern);
        Transition ts = createTransition(
            new Alphabet.EnterAttribute(nc,_OrderCounter++),
            middle);
        addAction(ts,true);
//??        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());

        // always treat attributes as optional,
        orgdest.addTransition(ts);
        return orgdest;
    }

    public Object data( DataPattern pattern ) {
        State result = createState(pattern);
        if(pattern.alias!=null) _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
        Transition t = createTransition(
            new Alphabet.DataText(pattern.type,pattern.alias), destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }
    
    public Object empty( EmptyPattern pattern ) {
        return destination;
    }
    public Object notAllowed( NotAllowedPattern pattern ) {
        // return a non-reachable state
        return createState(pattern);
    }

    public Object value( ValuePattern pattern ) {
        if(pattern.alias!=null) _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
        
        State result = createState(pattern);
        Transition t = createTransition(
            new Alphabet.ValueText(pattern.value, pattern.alias), destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }


    public Object list( ListPattern pattern ) {
        if(pattern.alias!=null) {
            // don't treat this list as a structured text.
	        _ScopeInfo.addUserDefinedAlias(pattern.alias,"String");
	        
            State result = createState(pattern);
	        Transition t = createTransition(
                new Alphabet.DataText(
                    new MetaDataType("string"),
                    pattern.alias), destination);
	        addAction(t,false);
	        result.addTransition(t);
	        return result;
        } else {
            // treat this list as a structured text
            destination.setListMode(State.LISTMODE_OFF);
            State head = (State)pattern.p.apply(this);
            head.setListMode(State.LISTMODE_ON);
            return head;
        }
    }
    

    public Object javaBlock( JavaBlock block ) {
        preservedAction.append(block.code);
        return destination;
    }
    
    public Object group( GroupPattern pattern ) {
        // build automaton in a reverse order.
        // TODO: how about actions?
        destination = (State)pattern.p2.apply(this);
        return               pattern.p1.apply(this);
    }
    
    public Object choice( ChoicePattern pattern ) {
        
        State dest = destination;
        addAction(destination,true);
        
        State head = (State)pattern.p1.apply(this);
        
        destination = dest;
        State member = (State)pattern.p2.apply(this);
        
        head.mergeTransitions(member);
        
        if(member.isAcceptable()) {
            // TODO: we need to copy exit actions from the member state
            // to the head state, but what if there already are some exit
            // actions?
            // this would happen for cases like
            // <choice>
            //   <group>
            //     <optional>...</optoinal>
            //     <cc:java> AAA </cc:java>
            //   </group>
            //   <group>
            //     <optional>...</optoinal>
            //     <cc:java> BBB </cc:java>
            //   </group>
            // </choice>
            //
            // this is a variation of ambiguity which we need to
            // detect.
            head.setAcceptable(true);
        }
        
        return head;
    }
	

    public Object interleave( InterleavePattern pattern ) {
        State tail = destination;
        ScopeBuildingContext oldContext = ctx;
        
        addAction(destination,true);
        
        State head = createState(pattern);
        ctx = new ScopeBuildingContext(ctx);
        ctx.setInterleaveBranchRoot(head);

        tail.addStateForWait(processInterleaveBranch(pattern.p1,head));
        tail.addStateForWait(processInterleaveBranch(pattern.p2,head));
        
        addAction(head,true);
        ctx = oldContext;
        return head;
    }

    private State processInterleaveBranch( Pattern child, State head ) {
        ctx.setCurrentThreadIndex(_ThreadCount++);
        State meetingspot = createState(child);
        meetingspot.setMeetingDestination(destination);
        
        destination = meetingspot;
        head.mergeTransitions( (State)child.apply(this) );
        
        return meetingspot;
    }


    public Object oneOrMore(OneOrMorePattern pattern) {
        State tail = destination;
        addAction(destination,true);
        State head = (State)pattern.p.apply(this);
        addAction(head,true); //addAction must be before mergeTransition
        tail.mergeTransitions(head);
        return head;
    }

    public Object ref( RefPattern pattern ) {
//      ScopeInfo.Action action = null;
//        if(preservedAction.length()!=0)
//            action = _ScopeInfo.createAction(preservedAction);
        
        State head = createState(pattern);
        
        ScopeInfo targetScope = grammar.getScopeInfo(pattern.target);
        
        String alias = pattern.param.alias;
        if(alias!=null)
            _ScopeInfo.addUserDefinedAlias(alias,
                targetScope.scope.getParam().returnType);
        
        Transition t = createTransition(new Alphabet.Ref(
            targetScope, alias, pattern.param.withParams, _OrderCounter++),
            destination);
        head.addTransition(t);

        // add action as epilogue because code should be executed
        // *after* the transition is performed.
        addAction(t,false);
        
        return head;
    }
    
    public Object scope( Scope scope ) {
        // we don't cross <ref> boundary, so this shouldn't be executed at all.
        throw new InternalError();
    }
	
    
	private State createState(Pattern source/*read from field --, ScopeBuildingContext ctx*/)
	{
		State s = new State(_ScopeInfo, ctx.getCurrentThreadIndex(), _ScopeInfo.getStateCount(), source);
		_ScopeInfo.addState(s);
		return s;
	}
	private Transition createTransition(Alphabet key, State destination)
	{
		Transition t = new Transition(key, destination);
		return t;
	}
	private void addAction(Transition t,boolean prologue)
	{
		if(preservedAction.length()!=0)
		{
            ScopeInfo.Action action = _ScopeInfo.createAction(preservedAction);
            preservedAction = new StringBuffer();
            
            if(prologue)    t.insertPrologueAction(action);
            else            t.insertEpilogueAction(action);
		}
	}
    /**
     * Adds the specified action as a prologue/epilogue action
     * to all the transitions that leave the given state.
     */
	private void addAction(State s,boolean prologue)
	{
        if(preservedAction.length()!=0) {
            ScopeInfo.Action act = _ScopeInfo.createAction(preservedAction);
            preservedAction = new StringBuffer();
            
			Iterator it = s.iterateTransitions();
			while(it.hasNext()) {
                Transition t = (Transition)it.next();
                if(prologue)    t.insertPrologueAction(act);
                else            t.insertEpilogueAction(act);
            }
            				
			s.addActionOnExit(act);
		}
	}
}
