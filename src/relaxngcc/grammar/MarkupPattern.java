package relaxngcc.grammar;

/**
 * Base class of ElementPattern and AttributePattern
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class MarkupPattern extends Pattern {
    public MarkupPattern( NameClass _name, Pattern _body ) {
        this.name = _name;
        this.body = _body;
        
        if(_name==null || _body==null)
            throw new IllegalArgumentException();
    }
    
    public final NameClass name;
    public final Pattern body;
}
