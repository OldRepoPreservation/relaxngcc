package relaxngcc.grammar;

/**
 * A pattern defined by a &lt;define> and &lt;start>.
 * 
 * A start pattern is represented by <code>name==null</code>.
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Scope {
    public Scope( String _name ) {
        this.name = _name;
    }
    
    /** Name of this pattern. */
    public final String name;
    
    private Pattern p;
    public Pattern getPattern() { return p; }
    
    /** Incorporates the newly discovered &lt;define>. */
    public void append( Pattern pattern, String method ) {
        // TODO
        throw new UnsupportedOperationException();
    }
}
