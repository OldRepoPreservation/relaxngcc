/*
 * State.java
 *
 * Created on 2001/08/04, 22:02
 */

package relaxngcc.automaton;
import java.util.*;
import java.io.PrintStream;

import relaxngcc.NGCCGrammar;
import relaxngcc.dom.NGCCElement;
import relaxngcc.builder.ScopeInfo;

/**
 * A State object has zero or more Transition objects
 */
public class State implements Comparable
{
	public static final int LISTMODE_PRESERVE = 0;
	public static final int LISTMODE_ON = 1;
	public static final int LISTMODE_OFF = 2;

	private Set _AllTransitions;
	private Set _StartElementTransitions; //collection of startElement type transitions
	private Set _EndElementTransitions; //collection of endElement type transitions
	private Set _TextTransitions; //collection of text type transitions
	private Set _AttributeTransitions; //collection of attribute type transitions
	private Set _RefTransitions; //collection of ref type transitions 
	private Set _ReversalTransitions; //collection of transitions that comes to this state from other state
    
	//acceptable or not
	private boolean _Acceptable;
	public void setAcceptable(boolean newvalue) { _Acceptable=newvalue; }
	public boolean isAcceptable() { return _Acceptable; }
	
	private String _ActionOnExit;
	public String getActionOnExit() { return _ActionOnExit; }
	public void addActionOnExit(String code) { _ActionOnExit = (_ActionOnExit==null)? code : code + _ActionOnExit; }
	 
	//for interleave support
	private State _MeetingDestination;
	private Set _StateForWait;

	private ScopeInfo _Container;
	
	//index identifies this state in a scope as an integer
	public int getIndex() { return _Index; }
	private int _Index;
	
	public int getThreadIndex() { return _ThreadIndex; }
	private int _ThreadIndex;

	private NGCCElement _LocationHint;
	
	//about list operation
	private int _ListMode;
	public void setListMode(int n) { _ListMode=n; }
	public int getListMode() { return _ListMode; }
	
	//constructor
    public State(ScopeInfo container, int thread, int index, NGCCElement e)
	{
		_Container = container;
		_LocationHint = e;
		_AllTransitions = new HashSet();
		_StartElementTransitions = new HashSet();
		_EndElementTransitions = new HashSet();
		_TextTransitions = new HashSet();
		_AttributeTransitions = new HashSet();
		_RefTransitions = new HashSet();
        
        _ReversalTransitions = new HashSet();
        
		_Acceptable = false;
		_Index = index;
		_ThreadIndex = thread;
		_ListMode = LISTMODE_PRESERVE;
    }

	public void addTransition(Transition t)
	{
		_AllTransitions.add(t);
		switch(t.getAlphabet().getType())
		{
			case Alphabet.START_ELEMENT:
				addTransitionWithCheck(_StartElementTransitions, t, null);
				break;
			case Alphabet.END_ELEMENT:
				addTransitionWithCheck(_EndElementTransitions, t, null);
				break;
			case Alphabet.START_ATTRIBUTE:
				addTransitionWithCheck(_AttributeTransitions, t, null);
				break;
			case Alphabet.TYPED_VALUE:
			case Alphabet.FIXED_VALUE:
				addTransitionWithCheck(_TextTransitions, t, null);
				break;
			case Alphabet.REF_BLOCK:
				addTransitionWithCheck(_RefTransitions, t, null);
				break;
		}
	}
	
	public boolean hasStartElementTransition() { return !_StartElementTransitions.isEmpty(); }
	public boolean hasEndElementTransition()   { return !_EndElementTransitions.isEmpty(); }
	public boolean hasAttributeTransition()    { return !_AttributeTransitions.isEmpty(); }
	public boolean hasTextTransition()         { return !_TextTransitions.isEmpty(); }
	public boolean hasRefTransition()          { return !_RefTransitions.isEmpty(); }

	public Iterator iterateTransitions()             { return _AllTransitions.iterator(); }
	public Iterator iterateStartElementTransitions() { return _StartElementTransitions.iterator(); }
	public Iterator iterateEndElementTransitions()   { return _EndElementTransitions.iterator(); }
	public Iterator iterateAttributeTransitions()    { return _AttributeTransitions.iterator(); }
	public Iterator iterateTextTransitions()         { return _TextTransitions.iterator(); }
	public Iterator iterateRefTransitions()          { return _RefTransitions.iterator(); }
	public Iterator iterateReversalTransitions()     { return _ReversalTransitions.iterator(); }
    
	public Set firstStartElementAlphabets() { return transitionsToFirstAlphabets(_StartElementTransitions); }
	public Set firstTextAlphabets()         { return transitionsToFirstAlphabets(_TextTransitions); }
	public Set firstAttributeAlphabets()    { return transitionsToFirstAlphabets(_AttributeTransitions); }
	
	public void addReversalTransition(Transition t) { _ReversalTransitions.add(t); }
    
	public void mergeTransitions(State s)
	{
		mergeTransitions(s, null);
	}
	public void mergeTransitions(State s, String action)
	{
		addTransitionsWithCheck(_StartElementTransitions, s._StartElementTransitions, action);
		addTransitionsWithCheck(_EndElementTransitions, s._EndElementTransitions, action);
		addTransitionsWithCheck(_AttributeTransitions, s._AttributeTransitions, action);
		addTransitionsWithCheck(_TextTransitions, s._TextTransitions, action);
		addTransitionsWithCheck(_RefTransitions, s._RefTransitions, action);
		_AllTransitions.addAll(s._AllTransitions);
	}
	
	public int compareTo(Object obj)
	{
		if(!(obj instanceof State)) throw new ClassCastException("not State object");
		
		return _Index-((State)obj)._Index;
	}
	
	//reports if this state has ambiguous transitions. [target] is a set of Transitions.
	private void addTransitionWithCheck(Set currentTransitions, Transition newtransition, String action)
	{
		Alphabet a = newtransition.getAlphabet();
		Iterator it = currentTransitions.iterator();
		while(it.hasNext())
		{
			Transition tr = (Transition)it.next();
			if(tr==newtransition) continue;
			Alphabet existing_alphabet = tr.getAlphabet();
            
            if(a.getType()==Alphabet.TYPED_VALUE && existing_alphabet.getType()==Alphabet.TYPED_VALUE)
            	printAmbiguousTransitionsWarning(tr, newtransition);
			else if(existing_alphabet.equals(a) && tr.nextState()!=newtransition.nextState())
            {
                if(newtransition.getAction()==null && tr.getAction()==null) //if both of them have no action, merge is possible
                {
                    tr.nextState().mergeTransitions(newtransition.nextState());
                    Iterator r = newtransition.nextState().iterateReversalTransitions();
                    while(r.hasNext())
                        ((Transition)r.next()).changeDestination(tr.nextState());
					return; //ignores newtransition
                }
                else
            		printAmbiguousTransitionsWarning(tr, newtransition);
            }
		}
		
		if(action!=null)
		{
			newtransition = (Transition)newtransition.clone();
			newtransition.appendActionAtHead(action);
		}
		
        currentTransitions.add(newtransition);
        newtransition.nextState().addReversalTransition(newtransition);
	}
	private void addTransitionsWithCheck(Set target, Set newtransitions, String action)
	{
		Iterator it = newtransitions.iterator();
		while(it.hasNext()) addTransitionWithCheck(target, (Transition)it.next(), action);
	}
	
	private static Set transitionsToFirstAlphabets(Set transitions)
	{
		Set result = new HashSet();
		Iterator it = transitions.iterator();
		while(it.hasNext())
		{
			Transition t = (Transition)it.next();
			result.add(t.getAlphabet());
		}
		return result;
	}
	
	public void checkFirstAlphabetAmbiguousity()
	{
		TreeSet alphabets = new TreeSet();
		Iterator trs = _AllTransitions.iterator();
		while(trs.hasNext())
		{
			Transition tr = (Transition)trs.next();
			if(tr.getAlphabet().getType()!=Alphabet.REF_BLOCK) alphabets.add(tr.getAlphabet());
		}
		
		NGCCGrammar gr = _Container.getGrammar();
		Iterator refs = iterateRefTransitions();
		while(refs.hasNext())
		{
			Transition ref = (Transition)refs.next();
			ScopeInfo sci = gr.getScopeInfoByName(ref.getAlphabet().getValue());
			Iterator as = alphabets.iterator();
			while(as.hasNext())
			{
				Alphabet a = (Alphabet)as.next();
				if(sci.isFirstAlphabet(a))
				{
					printAmbiguousTransitionAndFirstWarning(ref);
					break;
				}
			}
		}
	}
	
	public void checkFollowAlphabetAmbiguousity()
	{
		if(!_Acceptable) return;
	
		Iterator it = _AllTransitions.iterator();
		while(it.hasNext())
		{
			Transition t = (Transition)it.next();
			Alphabet a = t.getAlphabet();
			if(_Container.isFollowAlphabet(a))
				printAmbiguousTransitionAndFollowWarning(t);
		}
			
	}
	
	private void printAmbiguousTransitionsWarning(Transition a, Transition b)
	{
		PrintStream s = System.err;
		printStateWarningHeader(s);
		s.print(" has ambiguous transitions about following alphabets, ");
		s.print(a.getAlphabet().toString());
		s.print("(to state<");
		s.print(a.nextState().getIndex());
		s.print(">) and ");
		s.print(b.getAlphabet().toString());
		s.print("(to state<");
		s.print(b.nextState().getIndex());
		s.println(">).");
		
	}
	private void printAmbiguousTransitionAndFirstWarning(Transition t)
	{
		PrintStream s = System.err;
		printStateWarningHeader(s);
		s.print(" has ambiguous transitions about FIRST alphabets, ");
		s.print(t.getAlphabet().toString());
		s.print("(to state<");
		s.print(t.nextState().getIndex());
		s.println(">).");
	}
	private void printAmbiguousTransitionAndFollowWarning(Transition t)
	{
		PrintStream s = System.err;
		printStateWarningHeader(s);
		s.print(" has ambiguous transitions about FOLLOW alphabets, ");
		s.print(t.getAlphabet().toString());
		s.print("(to state<");
		s.print(t.nextState().getIndex());
		s.println(">).");
	}
	private void printStateWarningHeader(PrintStream s)
	{
		s.print("[Warning] ");
		String path = null;
		try
		{
			if(_LocationHint!=null) path = _LocationHint.getPath();
		}
		catch(UnsupportedOperationException e) {}
		
		s.print("The state <");
		s.print(_Index);
		if(path!=null)
		{
			s.print("> generated at or near ");
			s.print(path);
		}
		else
		{
			s.print("> whose path information is not available");
		}
		
		s.print(" in ");
		s.print(_Container.getLocation());
	}
	//for interleave support
	public void setMeetingDestination(State s) { _MeetingDestination=s; }
	public State getMeetingDestination() { return _MeetingDestination; }
	public void addStateForWait(State s)
	{
		if(_StateForWait==null) _StateForWait = new TreeSet();
		_StateForWait.add(s);
	}
	public Iterator iterateStatesForWait() { return _StateForWait.iterator(); }
}
