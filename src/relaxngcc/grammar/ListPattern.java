package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ListPattern extends Pattern {
    public ListPattern( Pattern _p, String _alias ) {
        this.p = _p;
        this.alias = _alias;
    }
    
    public final Pattern p;
    public final String alias;
    
    public Object apply( PatternFunction f ) {
        return f.list(this);
    }
}
