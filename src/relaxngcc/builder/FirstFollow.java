package relaxngcc.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.Observable;

import relaxngcc.NGCCGrammar;
import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.State;
import relaxngcc.automaton.Transition;

/**
 * Computes FIRST and FOLLOW sets.
 * 
 * The constructor will do all the computation.
 * Use the getFirst/getFollow methods to access the computed values.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class FirstFollow {
    private final Map firsts = new HashMap();
    private final Map follows = new HashMap();
    private final NGCCGrammar grammar;
    
    public Set getFirst( ScopeInfo si ) {
        return first(si).value;
    }
    
    public Set getFollow( ScopeInfo si ) {
        return follow(si).value;
    }
    
    
    private FirstCalculator first( ScopeInfo si ) {
        return (FirstCalculator)firsts.get(si);
    }
    private FirstCalculator first( Alphabet.Ref a ) {
        return first(a.getTargetScope());
    }
//    private FirstCalculator first( Transition tr ) {
//        return first(tr.getAlphabet().asRef());
//    }
    
    private FollowCalculator follow( ScopeInfo si ) {
        return (FollowCalculator)follows.get(si);
    }
    private FollowCalculator follow( Alphabet.Ref a ) {
        return follow(a.getTargetScope());
    }
//    private FollowCalculator follow( Transition tr ) {
//        return follow(tr.getAlphabet().asRef());
//    }
    
    
    public FirstFollow( NGCCGrammar _grammar ) {
        this.grammar = _grammar;
    
        // create calculators for all FIRSTs and FOLLOWs
        Iterator itr = grammar.iterateAllScopeBuilder();
        while(itr.hasNext()) {
            ScopeInfo si = ((ScopeBuilder)itr.next()).getScopeInfo();
            firsts.put( si, new FirstCalculator(si) );
            follows.put( si, new FollowCalculator(si) ); 
        }
        
        // make them subscribe to each other
        itr = firsts.values().iterator();
        while(itr.hasNext())
            ((FirstCalculator)itr.next()).subscribe();
        
        itr = follows.values().iterator();
        while(itr.hasNext())
            ((FollowCalculator)itr.next()).subscribe();
        
        // trigger the flood
        itr = firsts.values().iterator();
        while(itr.hasNext())
            ((FirstCalculator)itr.next()).notifyObservers();
        
        itr = follows.values().iterator();
        while(itr.hasNext())
            ((FollowCalculator)itr.next()).notifyObservers();
    }
    
    
    
    
    private class FirstCalculator extends Calculator {
        FirstCalculator( ScopeInfo _self ) {
            super(_self);
        }
        
        /**
         * Starts observing other FIRST/FOLLOWs whose
         * value we rely upon.
         */
        void subscribe() {
            
            if(self.isNullable())
                first(self).depends(follow(self));
            
            Iterator it = self.getInitialState().iterateTransitions();
            while(it.hasNext()) {
                Transition tr = (Transition)it.next();
                Alphabet a = tr.getAlphabet();
                if(a.isRef())
                    first(self).depends(first(a.asRef()));
                else
                    value.add(a);
            }
        }
    }
    
    private class FollowCalculator extends Calculator {
        FollowCalculator( ScopeInfo _self ) {
            super(_self);
        }
        
        /**
         * Starts observing other FIRST/FOLLOWs whose
         * value we rely upon.
         */
        void subscribe() {
            Iterator refs = self.iterateStatesHaving(Alphabet.REF_BLOCK);
            while(refs.hasNext()) {
                State s = (State)refs.next();
                Iterator trs = s.iterateTransitions(Alphabet.REF_BLOCK);
                while(trs.hasNext()) {
                    Transition tr = (Transition)trs.next();
                    ScopeInfo target = tr.getAlphabet().asRef().getTargetScope();
                    State next = tr.nextState();
                    
                    if(next.isAcceptable()) {
                        // FOLLOW(target) contains FOLLOW(this) if
                        // the transition ref[child] can reach to the final state of 
                        // this automaton.
//                        System.out.println(target.getName()+" depends "+self.getName());
                        follow(target).depends(follow(self));
                    }
                    
                    Iterator next_trs = next.iterateTransitions();
                    while(next_trs.hasNext()) {
                        Alphabet na = ((Transition)next_trs.next()).getAlphabet();
                        
                        if(na.isRef())
                            follow(target).depends(first(na.asRef()));
                        else
                            follow(target).value.add(na);
                    }
                }
            }
        }
    }
    
    private class Calculator extends Observable implements Observer {
        Calculator( ScopeInfo _self ) {
            this.self = _self;
        }
        
        /** we will compute FIRST/FOLLOW for this scope. */
        protected final ScopeInfo self;
        
        /** Current value of FIRST or FOLLOW. */
        protected final Set value = new HashSet();
        
        /** Declares that the value of this calculator depends on
         * that of the given calculator. */
        protected void depends( Calculator rhs ) {
            // watch rhs.
            rhs.addObserver(this);
        }
        
        public void update( Observable src, Object arg ) {
            if(value.addAll(((Calculator)src).value))
                // if this object gets changed, notify observers.
                notifyObservers();
        }
        
        // always return true because we want to update observers manually
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }
    }
}
