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
import relaxngcc.codedom.CDBlock;

/**
 * A Trnasition is a tuple of an Alphabet, a next state, and user-defined action.
 */
public final class Transition
{
	private Alphabet _Alphabet;
	private State _NextState;
    
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
    public void insertEpilogueActions(ScopeInfo.Action[] newActions) {
        for( int i=newActions.length-1; i>=0; i-- )
            insertEpilogueAction(newActions[i]);
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
    public CDBlock invokePrologueActions() {
        return invokeActions(prologueActions);
    }
    /** Gets the code to invoke all the epilogue actions. */
    public CDBlock invokeEpilogueActions() {
        return invokeActions(epilogueActions);
    }
    private static CDBlock invokeActions(Vector vec) {
        CDBlock sv = new CDBlock();
        for( int i=0; i<vec.size(); i++ )
            sv.add(((ScopeInfo.Action)vec.get(i)).invoke());
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
        if(a.isFork()) {
            Alphabet.Fork fork = a.asFork();
            for( int i=0; i<fork._subAutomata.length; i++ )
                fork._subAutomata[i].head( result, false );
            if(fork.isNullable())
                nextState().head( result, includeEE );
        } else
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
