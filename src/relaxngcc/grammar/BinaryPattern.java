package relaxngcc.grammar;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class BinaryPattern extends Pattern {
    public BinaryPattern( Pattern _p1, Pattern _p2 ) {
        this.p1 = _p1;
        this.p2 = _p2;
    }
    
    public final Pattern p1;
    public final Pattern p2;
}
