package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DataPattern extends Pattern {
    
    public DataPattern( NGCCCallParam _param ) {
        this.param = _param;
    }
    
    public final NGCCCallParam param;
    
    public Object apply( PatternFunction f ) {
        return f.data(this);
    }
}
