/*
 * State.java
 *
 * Created on 2001/08/04, 22:02
 */

package relaxngcc.automaton;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.xml.sax.Locator;

import relaxngcc.builder.ScopeInfo;
import relaxngcc.grammar.Pattern;
import relaxngcc.util.SelectiveIterator;
import relaxngcc.codedom.StatementVector;

/**
 * A State object has zero or more Transition objects
 */
public final class State implements Comparable
{
	private Set _AllTransitions;
    
	//acceptable or not
	private boolean _Acceptable;
	public void setAcceptable(boolean newvalue) { _Acceptable=newvalue; }
	public boolean isAcceptable() { return _Acceptable; }
	
    /** Actions that get executed when the execution leaves this state. */
	private final Vector actionsOnExit = new Vector();
    
    public ScopeInfo.Action[] getActionsOnExit() {
        return (ScopeInfo.Action[])actionsOnExit.toArray(new ScopeInfo.Action[0]);
    }
    /** Gets the code to invoke exit-actions. */
	public StatementVector invokeActionsOnExit() {
        StatementVector sv = new StatementVector();
        for( int i=0; i<actionsOnExit.size(); i++ )
            sv.addStatement(((ScopeInfo.Action)actionsOnExit.get(i)).invoke());
        return sv;
    }
    
	public void addActionOnExit(ScopeInfo.Action act) {
       actionsOnExit.add(0,act);
    }
    public void addActionsOnExit(ScopeInfo.Action[] act) {
        for( int i=act.length-1; i>=0; i-- )
            addActionOnExit(act[i]);
    }
	 
	//for interleave support
	private State _MeetingDestination;
	private Set _StateForWait;

    /** ScopeInfo that owns this state. */
	private final ScopeInfo _Container;
    public ScopeInfo getContainer() { return _Container; }
	
	//index identifies this state in a scope as an integer
	public int getIndex() { return _Index; }
	private int _Index;
	
	public int getThreadIndex() { return _ThreadIndex; }
	private int _ThreadIndex;
	
    /** Pattern from which this state was created. */
    public final Pattern locationHint;
    
    /**
     * 
     * @param location
     *      Indicates the pattern object from which this state is created.
     */
    public State(ScopeInfo container, int thread, int index, Pattern location )
	{
		_Container = container;
		_AllTransitions = new HashSet();
        
		_Acceptable = false;
		_Index = index;
		_ThreadIndex = thread;
        this.locationHint = location;
    }

	public void addTransition(Transition t) {
		_AllTransitions.add(t);
	}

    public void removeTransition( Transition t) {
        _AllTransitions.remove(t);
    }
	
    private class TypeIterator extends SelectiveIterator {
        TypeIterator( int _typeMask ) {
            super(_AllTransitions.iterator());
            this.typeMask=_typeMask;
        }
        private final int typeMask;
        protected boolean filter( Object o ) {
            return (((Transition)o).getAlphabet().getType()&typeMask)!=0;
        }
    }

    public Iterator iterateTransitions() { return _AllTransitions.iterator(); }

    /**
     * Checks if this state has transitions with
     * at least one of given types of alphabets.
     * 
     * @param alphabetTypes
     *      OR-ed combination of alphabet types you want to iterate.
     */
    public boolean hasTransition( int alphabetTypes ) {
        return new TypeIterator(alphabetTypes).hasNext();
    }
    
    /**
     * Iterate transitions with specified alphabets.
     * 
     * @param alphabetTypes
     *      OR-ed combination of alphabet types you want to iterate.
     */
    public Iterator iterateTransitions( int alphabetTypes ) {
        return new TypeIterator(alphabetTypes);
    }
        
    public int compareTo(Object obj) {
        if(!(obj instanceof State)) throw new ClassCastException("not State object");
        
        return _Index-((State)obj)._Index;
    }
    
    public void mergeTransitions(State s) {
        if(this==s)
            // this causes ConcurrentModificationException.
            // so we need to treat this as a special case.
            // 
            // merging a state to itself without any action
            // is a no-operation. so we can just return.
            return;
        mergeTransitions(s, null);
    }
    
    /**
     * For all the transitions leaving from the specified state,
     * add it to this state by appending the specified action
     * (possibly null) at the head of its prologue actions.
     */
	public void mergeTransitions(State s, ScopeInfo.Action action) {
        Iterator itr = s.iterateTransitions();
        while(itr.hasNext())
            // TODO: why there needs to be two methods "addTransitionWithCheck" and "addTransition"?
            addTransitionWithCheck( (Transition)itr.next(), action );
	}
	
	//reports if this state has ambiguous transitions. [target] is a set of Transitions.
    /**
     * Adds the specified transition to this state,
     * and reports any ambiguity error if detected.
     */
	private void addTransitionWithCheck(
        Transition newtransition, ScopeInfo.Action action)
	{
		Alphabet a = newtransition.getAlphabet();
        
		Iterator it = iterateTransitions();
		while(it.hasNext()) {
			Transition tr = (Transition)it.next();
			Alphabet existing_alphabet = tr.getAlphabet();
            
            // I don't see why this constitutes ambiguity -kk
//            if(a.isText())
//            	printAmbiguousTransitionsWarning(tr, newtransition);
//			else

            if(existing_alphabet.equals(a)) {
                if(tr.nextState()==newtransition.nextState()) {
                    if(action==null)
                        return; // trying to add the same transition. no-op.
                    else
                        // the same transition is being added but with an action.
                        // I guess this is ambiguous, but not sure. - Kohsuke
                        printAmbiguousTransitionsWarning(tr, newtransition);
                } else {
	                if(!newtransition.hasAction() && !tr.hasAction()) {
	                    // only if both of them have no action, we can merge them.
	                    tr.nextState().mergeTransitions(newtransition.nextState());
						return; //ignores newtransition
	                } else
	            		printAmbiguousTransitionsWarning(tr, newtransition);
                }
            }
		}
		
        // always make a copy, because we might modify actions later.
        // in general, it is dangerous to share transitions.
        newtransition = (Transition)newtransition.clone();
        
		if(action!=null)
			newtransition.insertPrologueAction(action);
		
        _AllTransitions.add(newtransition);
	}

	private void printAmbiguousTransitionsWarning(Transition a, Transition b)
	{
		PrintStream s = System.err;
		printStateWarningHeader(s);
		s.print(" has ambiguous transitions: ");
		s.print(a.getAlphabet().toString());
		s.print("(to #");
		s.print(a.nextState().getIndex());
		s.print(") and ");
		s.print(b.getAlphabet().toString());
		s.print("(to #");
		s.print(b.nextState().getIndex());
		s.println(".)");
        a.getAlphabet().printLocator(s);
        b.getAlphabet().printLocator(s);
	}
    
	private void printStateWarningHeader(PrintStream s) {
		s.print("[Warning] ");

		s.print("State #");
		s.print(_Index);
		s.print(" of ");
        s.print(_Container.scope.name);
        // TODO: location
//		s.print(_Container.getLocation());
	}


	//for interleave support
	public void setMeetingDestination(State s) { _MeetingDestination=s; }
	public State getMeetingDestination() { return _MeetingDestination; }
	public void addStateForWait(State s) {
		if(_StateForWait==null) _StateForWait = new HashSet();
		_StateForWait.add(s);
	}
	public Iterator iterateStatesForWait() {
        if(_StateForWait==null)     return emptyIterator;
        else                        return _StateForWait.iterator();
    }
    
    private static final Iterator emptyIterator = new Iterator() {
        public void remove() { throw new UnsupportedOperationException(); }
        public boolean hasNext() { return false; }
        public Object next() { throw new NoSuchElementException(); }
    };
    
    
    /**
     * Computes HEAD set of this state.
     * 
     * See {@link Head} for the definition.
     */
    public Set head( boolean includeEE ) {
        Set s = new HashSet();
        head(s,includeEE);
        return s;
    }
    
    /**
     * Internal function to compute HEAD.
     */
    void head( Set result, boolean includeEE ) {
        
        if(isAcceptable() && includeEE )
            result.add(Head.EVERYTHING_ELSE);
        
        Iterator itr = iterateTransitions();
        while(itr.hasNext()) {
            Transition t = (Transition)itr.next();
            t.head( result, includeEE );
        }
    }
    
    /**
     * Computes ATTHEAD set of this state and returns them
     * in a sorted order.
     * 
     * See {@link HEAD} for the definition.
     */
    public Set attHead() {
        Set r = new TreeSet(Alphabet.orderComparator);
        attHead(r);
        return r;
    }
    
    // internal-version
    private void attHead( Set result ) {
        Iterator itr = iterateTransitions();
        while(itr.hasNext()) {
            Transition t = (Transition)itr.next();
            Alphabet a = t.getAlphabet();
            
            if(a.isEnterAttribute())
                result.add(a);
            else
            if(a.isRef()) {
                // ref[X] itself will be included in ATTHEAD
                result.add(a);
                if(a.asRef().getTargetScope().isNullable())
                    t.nextState().attHead(result);
            }
        }
    }
}
