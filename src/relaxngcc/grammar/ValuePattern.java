package relaxngcc.grammar;

import org.xml.sax.Locator;

import relaxngcc.datatype.Datatype;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ValuePattern extends Pattern {
    public ValuePattern( Locator loc, Datatype _type, String _value, String _alias ) {
        this.type = _type;
        this.value = _value;
        this.alias = _alias;
        this.locator = loc;
    }
    
    public final Datatype type;
    public final String value;
    public final String alias;
    public final Locator locator;
    
    public Object apply( PatternFunction f ) {
        return f.value(this);
    }
}
