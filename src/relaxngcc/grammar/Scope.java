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
    
    /** Body of this definition. */
    private Pattern p;
    public Pattern getPattern() { return p; }
    
    /** Code specified via &lt;cc:java-import> statements. */
    private String imports;
    public void appendImport( String code ) {
        if(imports==null)   imports=code;
        else                imports+=code;
    }

    /** Code specified via &lt;cc:java-body> statements. */
    private String body;
    public void appendBody( String code ) {
        if(body==null)   body=code;
        else                body+=code;
    }
    
    /** Incorporates the newly discovered &lt;define>. */
    public void append( Pattern pattern, String method ) {
        // TODO: handle @combine
        if(method!=null)
            throw new UnsupportedOperationException();
        this.p = pattern;
    }
}
