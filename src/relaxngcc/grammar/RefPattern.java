package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class RefPattern extends Pattern {
    public RefPattern( Scope _target, NGCCCallParam _param ) {
        this.target = _target;
        this.param = _param;
    }
    
    public final Scope target;
    public final NGCCCallParam param;

    public Object apply( PatternFunction f ) {
        return f.ref(this);
    }
}
