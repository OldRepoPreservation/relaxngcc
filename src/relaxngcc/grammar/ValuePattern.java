package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ValuePattern extends Pattern {
    public ValuePattern( String _value, String _alias ) {
        this.value = _value;
        this.alias = _alias;
    }
    
    public final String value;
    public final String alias;
    
    public Object apply( PatternFunction f ) {
        return f.value(this);
    }
}
