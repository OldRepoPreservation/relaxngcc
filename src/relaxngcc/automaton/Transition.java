/*
 * Transition.java
 *
 * Created on 2001/08/04, 22:05
 */

package relaxngcc.automaton;

/**
 * A Trnasition is a tuple of an Alphabet, a next state, and user-defined action.
 */
public class Transition
{
	private Alphabet _Alphabet;
	private State _NextState;
	private String _Action;
	
	private State _EnableState;
	private State _DisableState;
	
	/**
	 * creates Transition with no action
	 */
    public Transition(Alphabet a, State n)
	{ _Alphabet=a; _NextState=n; }

	/**
	 * add a new action at head of current action.
	 */
	public void appendActionAtHead(String action)
	{
		if(_Action==null)
			_Action = action;
		else
			_Action = action + _Action;
		
		_Action += System.getProperty("line.separator");
	}
	/**
	 * creates Transition with user-defined action
	 */
    public Transition(Alphabet a, State n, String act)
	{ _Alphabet=a; _NextState=n; _Action=act; }
	
	public Object clone()
	{
		Transition t = new Transition(_Alphabet, _NextState);
		t._Action = _Action;
		return t;
	}

	public Alphabet getAlphabet() { return _Alphabet; }
	public State nextState() { return _NextState; }
	public String getAction() { return _Action; }

    public void changeDestination(State s) { _NextState=s; }
	
	public void setEnableState(State s)
	{ _EnableState = s; }
	public void setDisableState(State s)
	{ _DisableState = s; }
	public State getEnableState() { return _EnableState; }
	public State getDisableState() { return _DisableState; }
}
