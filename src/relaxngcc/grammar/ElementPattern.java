package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ElementPattern extends MarkupPattern {
    public ElementPattern(NameClass _name, Pattern _body) {
        super(_name, _body);
    }
    public Object apply( PatternFunction f ) {
        return f.element(this);
    }
}
