/*
 * ScopeBuildingContext.java
 *
 * Created on 2002/01/02, 15:56
 */

package relaxngcc.builder;
import relaxngcc.automaton.State;

public class ScopeBuildingContext
{
	private State _InterleaveBranchRoot;
    private int _CurrentThreadIndex;
    
	public ScopeBuildingContext()
	{
		_CurrentThreadIndex = -1;
	}
	public ScopeBuildingContext(ScopeBuildingContext ctx)
	{
		_InterleaveBranchRoot = ctx._InterleaveBranchRoot;
		_CurrentThreadIndex = ctx._CurrentThreadIndex;
    }
	public int getCurrentThreadIndex() { return _CurrentThreadIndex; }
	public void setCurrentThreadIndex(int n) { _CurrentThreadIndex = n; }
	
	public State getInterleaveBranchRoot() { return _InterleaveBranchRoot; }
	public void  setInterleaveBranchRoot(State s) { _InterleaveBranchRoot = s; }
}
