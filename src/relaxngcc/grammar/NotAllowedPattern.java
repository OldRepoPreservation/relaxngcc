package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NotAllowedPattern extends Pattern {
    private NotAllowedPattern() {}
    
    public Object apply( PatternFunction f ) {
        return f.notAllowed(this);
    }
    
    public final static Pattern theInstance = new NotAllowedPattern();
}
