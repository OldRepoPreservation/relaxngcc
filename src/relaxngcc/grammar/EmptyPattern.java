package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class EmptyPattern extends Pattern {
    private EmptyPattern() {}
    
    public Object apply( PatternFunction f ) {
        return f.empty(this);
    }
    
    public static final Pattern theInstance = new EmptyPattern();
}
