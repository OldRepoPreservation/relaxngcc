/*
 * Transition.java
 *
 * Created on 2001/08/04, 22:05
 */

package relaxngcc.automaton;

import java.util.Vector;

import relaxngcc.builder.ScopeInfo;

/**
 * A Trnasition is a tuple of an Alphabet, a next state, and user-defined action.
 */
public final class Transition
{
	private Alphabet _Alphabet;
	private State _NextState;
    
	private State _EnableState;
	private State _DisableState;
	
    /** value that uniquely identifies a transition. */
    private final int uniqueId;
    
	/**
	 * creates Transition with no action
	 */
    public Transition(Alphabet a, State n) {
        this(a,n,new Vector(),new Vector());
    }

    private static Vector createVector( ScopeInfo.Action a ) {
        Vector vec = new Vector();
        vec.add(a);
        return vec;
    }
    
    /**
     * creates Transition with user-defined action
     */
//    public Transition(Alphabet a, State n, ScopeInfo.Action act) {
//        this(a,n,createVector(act));
//    }
    
    private Transition(Alphabet a, State n, Vector _pro, Vector _epi) {
        _Alphabet=a;
        _NextState=n;
        prologueActions=_pro;
        epilogueActions=_epi;
        uniqueId=iotaGen++;
    }

    /**
     * Actions to be executed immediately
     * before this transition is performed.
     */
    private final Vector prologueActions;
    
    /**
     * Actions to be executed immediately
     * after this transition is performed.
     * 
     * Note that the difference between prologue
     * and epilogue is significant only for REF-type alphabets.
     */
    private final Vector epilogueActions;
    
	/** Adds a new action at head of the prologue actions. */
	public void insertPrologueAction(ScopeInfo.Action newAction) {
        prologueActions.add(0,newAction);
	}
    /** Adds a new action at head of the epilogue actions. */
    public void insertEpilogueAction(ScopeInfo.Action newAction) {
        epilogueActions.add(0,newAction);
    }
    
    /** Gets all prologue actions. */
    public ScopeInfo.Action[] getPrologueActions() {
        return toActionArray(prologueActions);
    }
    /** Gets all epilogue actions. */
    public ScopeInfo.Action[] getEpilogueActions() {
        return toActionArray(epilogueActions);
    }
    private static ScopeInfo.Action[] toActionArray(Vector vec) {
        return (ScopeInfo.Action[])vec.toArray(new ScopeInfo.Action[vec.size()]);
    }
    
    /** Gets the code to invoke all the prologue actions. */
    public String invokePrologueActions() {
        return invokeActions(prologueActions);
    }
    /** Gets the code to invoke all the epilogue actions. */
    public String invokeEpilogueActions() {
        return invokeActions(epilogueActions);
    }
    private static String invokeActions(Vector vec) {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<vec.size(); i++ )
            buf.append(((ScopeInfo.Action)vec.get(i)).invoke());
        return buf.toString();
    }
    /** Returns true if this transition has any associated action. */
    public boolean hasAction() {
        return !prologueActions.isEmpty() || !epilogueActions.isEmpty();
    }
    
    
	
	public Object clone() {
        return new Transition(_Alphabet, _NextState,
            (Vector)prologueActions.clone(), (Vector)epilogueActions.clone());
	}

	public Alphabet getAlphabet() { return _Alphabet; }
	public State nextState() { return _NextState; }
    
    
    public int getUniqueId() { return uniqueId; }

    public void changeDestination(State s) { _NextState=s; }
	
	public void setEnableState(State s)
	{ _EnableState = s; }
	public void setDisableState(State s)
	{ _DisableState = s; }
	public State getEnableState() { return _EnableState; }
	public State getDisableState() { return _DisableState; }
    
    
    /** used to produce unique IDs. */
    private static int iotaGen=1;
}
