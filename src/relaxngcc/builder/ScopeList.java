/*
 * ScopeList.java
 *
 * Created on 2001/09/26, 23:08
 */

package relaxngcc.builder;

/**
 * single link list of ScopeInfo
 */
public class ScopeList
{
	private ScopeInfo _car;
	private ScopeList _cdr;
    public ScopeList(ScopeInfo car)	{ _car=car; }
	
	public ScopeList(ScopeInfo car, ScopeList cdr) { _car=car; _cdr=cdr; }
	
	public boolean contains(ScopeInfo s)
	{
		if(_car==s) return true;
		if(_cdr==null) return false;
		else return _cdr.contains(s);
	}

}
