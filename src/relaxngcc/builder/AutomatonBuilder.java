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
import relaxngcc.grammar.AttributePattern;
import relaxngcc.grammar.ChoicePattern;
import relaxngcc.grammar.DataPattern;
import relaxngcc.grammar.ElementPattern;
import relaxngcc.grammar.EmptyPattern;
import relaxngcc.grammar.GroupPattern;
import relaxngcc.grammar.InterleavePattern;
import relaxngcc.grammar.JavaBlock;
import relaxngcc.grammar.ListPattern;
import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.NotAllowedPattern;
import relaxngcc.grammar.OneOrMorePattern;
import relaxngcc.grammar.Pattern;
import relaxngcc.grammar.PatternFunction;
import relaxngcc.grammar.RefPattern;
import relaxngcc.grammar.Scope;
import relaxngcc.grammar.ValuePattern;

/**
 * Builds an automaton from {@link Scope} object.
 * 
 * <p>
 * This function returns {@link String}.
 * 
 * @author Daisuke Okajima
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AutomatonBuilder implements PatternFunction
{
	private int _ThreadCount;

	class Context {
	    private State _InterleaveBranchRoot;
	    private int _CurrentThreadIndex;
	    
		public Context() {
			_CurrentThreadIndex = -1;
		}
		public Context(Context ctx) {
			_InterleaveBranchRoot = ctx._InterleaveBranchRoot;
			_CurrentThreadIndex = ctx._CurrentThreadIndex;
		}
        
	    public int getCurrentThreadIndex() { return _CurrentThreadIndex; }
	    public void setCurrentThreadIndex(int n) { _CurrentThreadIndex = n; }
	    
	    public State getInterleaveBranchRoot() { return _InterleaveBranchRoot; }
	    public void  setInterleaveBranchRoot(State s) { _InterleaveBranchRoot = s; }
	}

    /**
     * Used to give order numbers to EnterAttribute alphabets.
     */
    private int _OrderCounter;
    
    /** actions are added to this buffer until it is processed */
    private StringBuffer preservedAction = new StringBuffer();

	
	
    /** Builds ScopeInfo. */
    public static void build( NGCCGrammar grammar, ScopeInfo scope ) {
        new AutomatonBuilder(grammar,scope).build();
    }
    
    private AutomatonBuilder( NGCCGrammar grammar, ScopeInfo scope ) {
        this.grammar = grammar;
        this._ScopeInfo = scope;
    }
    
    private final NGCCGrammar grammar;
    private final ScopeInfo _ScopeInfo;
    
	public void build() {
		ctx = new Context();
		//starts from final state
	    destination = createState(null);
		destination.setAcceptable(true);

        State initial = (State)_ScopeInfo.scope.getPattern().apply(this);
        initial = addAction(initial,true);
        		
//		State initial = null;
//        if(_Type==TYPE_NORMAL || _Type==TYPE_ROOT)
//    		initial = traverseNodeList(_Root.getChildNodes(), ctx, finalstate);
//		else
//			initial = processRelaxNGNode(_Root, ctx, finalstate);
		
        _ScopeInfo.setThreadCount(_ThreadCount);
        
		_ScopeInfo.setInitialState(initial);
        
        _ScopeInfo.copyAttributeHandlers();
        
        _ScopeInfo.minimizeStates();
	}
	

    private Context ctx;
    private State destination;
    
    public Object element( ElementPattern pattern ) {
        NameClass nc = pattern.name;
        
        State tail = createState(pattern);
        Transition te = createTransition(
            new Alphabet.LeaveElement(nc,pattern.startLocator), destination);
        addAction(te,false);
        if(ctx.getInterleaveBranchRoot()!=null) te.setEnableState(ctx.getInterleaveBranchRoot());
        tail.addTransition(te);
        
        // start a new context
        Context oldContext = ctx;
        ctx = new Context(ctx);
        
        ctx.setInterleaveBranchRoot(null);

        // process descendants
        destination = tail;
        State middle = (State)pattern.body.apply(this);
        
        oldContext = ctx;
        
        State head = createState(pattern);
        Transition ts = createTransition(
            new Alphabet.EnterElement(nc,pattern.endLocator), middle);
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
        Transition te = createTransition(
            new Alphabet.LeaveAttribute(nc,pattern.startLocator),
            destination /*createState(exp,ctx)*/);
        addAction(te,false);
        tail.addTransition(te);
  
        destination = tail;
        State middle = (State)pattern.body.apply(this);      
//        State middle = traverseNodeList(exp.getChildNodes(), ctx/*newctx*/, tail);
        
        Alphabet.EnterAttribute ea = new Alphabet.EnterAttribute(
            nc,_OrderCounter++,pattern.endLocator);
        ea.workaroundSignificant = pattern.workaroundSignificant;
        
        Transition ts = createTransition(
            ea,
            middle);
        addAction(ts,true);
//??        if(ctx.getInterleaveBranchRoot()!=null) ts.setDisableState(ctx.getInterleaveBranchRoot());
        
        if(!pattern.workaroundSignificant) {
            // always treat attributes as optional,
            orgdest.addTransition(ts);
            return orgdest;
        } else {
            // if a special flag is specified by the user,
            // do NOT treat it as an optional attribute
            State head = createState(pattern);
            head.addTransition(ts);
            return head;
        }
    }

    public Object data( DataPattern pattern ) {
        State result = createState(pattern);
        if(pattern.alias!=null) _ScopeInfo.addAlias(pattern.alias,"String");
        Transition t = createTransition(
            new Alphabet.DataText(pattern.type,pattern.alias,pattern.locator),
            destination);
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
        if(pattern.alias!=null) _ScopeInfo.addAlias(pattern.alias,"String");
        
        State result = createState(pattern);
        Transition t = createTransition(
            new Alphabet.ValueText(pattern.value, pattern.alias, pattern.locator),
            destination);
        addAction(t,false);
        result.addTransition(t);
        return result;
    }


    public Object list( ListPattern pattern ) {
        if(pattern.alias!=null) {
            // don't treat this list as a structured text.
	        _ScopeInfo.addAlias(pattern.alias,"String");
	        
            State result = createState(pattern);
	        Transition t = createTransition(
                new Alphabet.DataText(
                    new MetaDataType("string"),
                    pattern.alias,pattern.locator),
                destination);
	        addAction(t,false);
	        result.addTransition(t);
	        return result;
        } else {
            // treat this list as a structured text
            State head = (State)pattern.p.apply(this);
            
            // then append the "header" transition that tokenizes the text.
            _ScopeInfo.addAlias("__text","String");
            Transition tr = new Transition(
                new Alphabet.DataText(
                    new MetaDataType("string"), "__text", pattern.locator ),
                head );
            tr.insertEpilogueAction(_ScopeInfo.createAction(
                "runtime.processList(__text);"));
            addAction(tr,false);
            // add user-defined action before the processList method,
            // so that those are executed before <list> is processed.
            
            State top = createState(pattern);
            top.addTransition(tr);
            
            return top;
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
        
        State dest = addAction(destination,true);
        
        
        // a branch could be empty, in that case head could be returned
        // as the head of a branch. This would cause a weird effect.
        // so we should better create a new state.
        State head = createState(pattern);
        
        destination = dest;
        processChoiceBranch(head,pattern.p2);
        destination = dest;
        processChoiceBranch(head,pattern.p1);
        
        return head;
    }
    
    private void processChoiceBranch( State head, Pattern pattern ) {
        
        State member = (State)pattern.apply(this);
        member = addAction(member,true);
        
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
            head.addActionsOnExit(member.getActionsOnExit());
        }
    }
	

    public Object interleave( InterleavePattern pattern ) {
        Context oldContext = ctx;
        
        State tail = addAction(destination,true);
        
        State head = createState(pattern);
        ctx = new Context(ctx);
        ctx.setInterleaveBranchRoot(head);

        tail.addStateForWait(processInterleaveBranch(pattern.p2,head));
        tail.addStateForWait(processInterleaveBranch(pattern.p1,head));
        
        head = addAction(head,true);
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
        destination = addAction(destination,true);
        
        State tail = destination;   // remember the current destination
        
        State head = (State)pattern.p.apply(this);
        head = addAction(head,true); //addAction must be before mergeTransition
        
        tail.mergeTransitions(head);
        return head;
    }

    public Object ref( RefPattern pattern ) {
//      ScopeInfo.Action action = null;
//        if(preservedAction.length()!=0)
//            action = _ScopeInfo.createAction(preservedAction);
        
        State head = createState(pattern);
        
        ScopeInfo targetScope = grammar.getScopeInfo(pattern.target);
        
        String alias = pattern.param.getAlias();
        if(alias!=null)
            _ScopeInfo.addAlias(alias,
                targetScope.scope.getParam().returnType);
        
        Transition t = createTransition(new Alphabet.Ref(
            targetScope, alias,
            pattern.param.getWithParams(), _OrderCounter++,
            pattern.locator),
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
	
    
	private State createState(Pattern source) {
		State s = new State(_ScopeInfo, ctx.getCurrentThreadIndex(), _ScopeInfo.getStateCount(), source);
		_ScopeInfo.addState(s);
		return s;
	}
    
	private Transition createTransition(Alphabet key, State destination) {
		Transition t = new Transition(key, destination);
		return t;
	}
    
	private void addAction(Transition t,boolean prologue) {
		if(preservedAction.length()==0)   return;
        
        ScopeInfo.Action action = _ScopeInfo.createAction(preservedAction);
        preservedAction = new StringBuffer();
        
        if(prologue)    t.insertPrologueAction(action);
        else            t.insertEpilogueAction(action);
	}
    
    /**
     * Adds the specified action as a prologue/epilogue action
     * to all the transitions that leave the given state.
     * 
     * <p>
     * To avoid causing unexpected modification, a State object
     * will be copied and the new state will be returned
     * 
     * <p>
     * Consider a process of building (A|(B,cc:java)) where
     * A and B are elements and cc:java is an associated java action,
     * if we don't copy the state before adding actions to it, then
     * we end up creating the following automaton:
     * 
     * <pre><xmp>
     * s1 --- A ---> s2+action (final state)
     *  |             ^
     *  +---- B -----+
     * </xmp></pre>
     * 
     * which is incorrect, because we don't want cc:java to be executed
     * when we see A.
     * 
     * <p>
     * Copying a state will prevent this side-effect.
     */
	private State addAction(State s,boolean prologue) {
        if(preservedAction.length()==0) return s;
        
        ScopeInfo.Action act = _ScopeInfo.createAction(preservedAction);
        preservedAction = new StringBuffer();
        
        State ss = createState(s.locationHint);
        ss.mergeTransitions(s);
        if(s.isAcceptable()) {
            ss.setAcceptable(true);
            ss.addActionsOnExit(s.getActionsOnExit());
        }
        
		Iterator it = ss.iterateTransitions();
		while(it.hasNext()) {
            Transition t = (Transition)it.next();
            if(prologue)    t.insertPrologueAction(act);
            else            t.insertEpilogueAction(act);
        }
        				
		ss.addActionOnExit(act);
        return ss;
	}
}
