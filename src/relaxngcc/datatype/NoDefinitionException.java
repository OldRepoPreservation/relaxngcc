package relaxngcc.datatype;

import org.xml.sax.Locator;

/**
 * Signals undefined variable in a variable expansion process.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NoDefinitionException extends Exception {
    public final Locator locator;
    
    public NoDefinitionException( String tagName, Locator _loc ) {
        super( "undefined variable name '"+tagName+"'" );
        this.locator = _loc;
    }
}
