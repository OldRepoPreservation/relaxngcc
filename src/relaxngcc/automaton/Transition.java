/*
 * Transition.java
 *
 * Created on 2001/08/04, 22:05
 */

package relaxngcc.automaton;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import relaxngcc.builder.ScopeInfo;
import relaxngcc.codedom.StatementVector;

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
    
	/** Creates Transition with no action. */
    public Transition(Alphabet a, State n) {
        this(a,n,new Vector(),new Vector());
    }

    private static Vector createVector( ScopeInfo.Action a ) {
        Vector vec = new Vector();
        vec.add(a);
        return vec;
    }
    
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
    public StatementVector invokePrologueActions() {
        return invokeActions(prologueActions);
    }
    /** Gets the code to invoke all the epilogue actions. */
    public StatementVector invokeEpilogueActions() {
        return invokeActions(epilogueActions);
    }
    private static StatementVector invokeActions(Vector vec) {
        StatementVector sv = new StatementVector();
        for( int i=0; i<vec.size(); i++ )
            sv.addStatement(((ScopeInfo.Action)vec.get(i)).invoke());
        return sv;
    }
    /** Returns true if this transition has any associated action. */
    public boolean hasAction() {
        return !prologueActions.isEmpty() || !epilogueActions.isEmpty();
    }
    
    
	
	public Object clone() {
        return clone(_NextState);
	}
    
    public Transition clone( State next ) {
        return new Transition(_Alphabet, next,
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


    /**
     * Computes HEAD set of this transition.
     * 
     * See {@link Head} for the definition.
     */
    public Set head( boolean includeEE ) {
        Set s = new HashSet();
        head(s,includeEE);
        return s;
    }
    
    /**
     * Internal function to compute HEAD(t)
     * 
     * @param includeEE
     *      If true, the return set will include EVERYTHING_ELSE
     *      when appropriate.
     */
    void head( Set result, boolean includeEE ) {
        Alphabet a = getAlphabet();
        if(!a.isRef()) {
            result.add(a);
        } else {
            ScopeInfo target = a.asRef().getTargetScope();
            target.head( result );
            
            if( target.isNullable() )
                nextState().head( result, includeEE );
        }
    }

    
    
    /** used to produce unique IDs. */
    private static int iotaGen=1;
}
