package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ValuePattern extends Pattern {
    public ValuePattern( String _value, NGCCCallParam _param ) {
        this.value = _value;
        this.param = _param;
    }
    
    public final String value;
    public final NGCCCallParam param;
    
    public Object apply( PatternFunction f ) {
        return f.value(this);
    }
}
