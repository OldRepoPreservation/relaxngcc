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
    
    /**
     * Array of Actions to be executed
     * when this transition is performed.
     */
	private final Vector actions;
	
	private State _EnableState;
	private State _DisableState;
	
    /** value that uniquely identifies a transition. */
    private final int uniqueId;
    
	/**
	 * creates Transition with no action
	 */
    public Transition(Alphabet a, State n) {
        this(a,n,new Vector());
    }

    private static Vector createVector( ScopeInfo.Action a ) {
        Vector vec = new Vector();
        vec.add(a);
        return vec;
    }
    
    /**
     * creates Transition with user-defined action
     */
    public Transition(Alphabet a, State n, ScopeInfo.Action act) {
        this(a,n,createVector(act));
    }
    
    private Transition(Alphabet a, State n, Vector _actions) {
        _Alphabet=a;
        _NextState=n;
        actions=_actions;
        uniqueId=iotaGen++;
    }

	/**
	 * add a new action at head of current action.
	 */
	public void appendActionAtHead(ScopeInfo.Action newAction) {
        actions.add(0,newAction);
	}
	
	public Object clone() {
        return new Transition(_Alphabet, _NextState, (Vector)actions.clone());
	}

	public Alphabet getAlphabet() { return _Alphabet; }
	public State nextState() { return _NextState; }
	public ScopeInfo.Action[] getActions() {
        return (ScopeInfo.Action[])actions.toArray(new ScopeInfo.Action[0]);
    }
    /** Gets the code to invoke all the associated actions. */
    public String invokeActions() {
        StringBuffer buf = new StringBuffer();
        for( int i=0; i<actions.size(); i++ )
            buf.append(((ScopeInfo.Action)actions.get(i)).invoke());
        return buf.toString();
    }
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
