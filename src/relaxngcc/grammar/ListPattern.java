package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ListPattern extends Pattern {
    public ListPattern( Pattern _p ) {
        this.p = _p;
    }
    
    public final Pattern p;

    public Object apply( PatternFunction f ) {
        return f.list(this);
    }
}
