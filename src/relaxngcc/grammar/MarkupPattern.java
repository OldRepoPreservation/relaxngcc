package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class MarkupPattern extends Pattern {
    public MarkupPattern( NameClass _name, Pattern _body ) {
        this.name = _name;
        this.body = _body;
    }
    
    public final NameClass name;
    public final Pattern body;
}
