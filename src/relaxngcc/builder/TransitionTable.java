package relaxngcc.builder;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Comparator;
import java.text.MessageFormat;

import relaxngcc.automaton.State;
import relaxngcc.automaton.Alphabet;
import relaxngcc.automaton.Transition;
import relaxngcc.automaton.WithOrder;

public class TransitionTable {
    private final Map table = new HashMap();
    
    public void add( State s, Alphabet alphabet, Transition action ) {
        Map m = (Map)table.get(s);
        if(m==null)
            table.put(s,m=new HashMap());
        
        if(m.containsKey(alphabet)) {
            // TODO: proper error report
            String scopename = s.getContainer()._scope.name;
            if(scopename==null) scopename = "<start>"; //the ScopeInfo for <start> block has a null as its name
            System.err.println(MessageFormat.format(
                "State #{0}  of \"{1}\" has a conflict by {2}",
                new Object[]{
                    Integer.toString(s.getIndex()),
                    scopename,
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
     * the specified state in terms of TrnasitionTable.Entry.
     * The resulting array is sorted in the order of Transition.
     */
    public Map.Entry[] list( State s ) {
        Map m = (Map)table.get(s);
        if(m==null)
            return new Map.Entry[0];
            /*
            return new Iterator() {
                public boolean hasNext() { return false; }
                public Object next() { return null; }
                public void remove() { throw new UnsupportedOperationException(); }
            };
            */
        else {
            Map.Entry[] a = (Map.Entry[])m.entrySet().toArray(new Map.Entry[m.size()]);
            Arrays.sort(a, new Comparator() {
                public int compare(Object t1, Object t2) {
                    Object o1 = ((Map.Entry)t1).getValue();
                    Object o2 = ((Map.Entry)t2).getValue();
                    return ((WithOrder)o2).getOrder()-((WithOrder)o1).getOrder();
                }
            } );
            return a;
            
        }
    }

}
