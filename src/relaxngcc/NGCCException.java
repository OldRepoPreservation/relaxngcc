/*
 * NGCCException.java
 *
 * Created on 2001/08/11, 11:13
 */

package relaxngcc;

/**
 * generic exception for RelaxNGCC
 */
public class NGCCException extends Exception
{
    public NGCCException(String msg)
	{ super(msg); }
	public NGCCException(Exception parent) { super(parent.getMessage()); }
}
