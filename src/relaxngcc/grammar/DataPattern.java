package relaxngcc.grammar;

import org.xml.sax.Locator;

import relaxngcc.datatype.Datatype;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DataPattern extends Pattern {
    
    public DataPattern( Locator loc, Datatype _type, String _alias ) {
        this.alias = _alias;
        this.type = _type;
        this.locator = loc;
    }
    
    public final Datatype type;
    public final String alias;
    public final Locator locator;
    
    public Object apply( PatternFunction f ) {
        return f.data(this);
    }
}
