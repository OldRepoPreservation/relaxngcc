package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttributePattern extends MarkupPattern {
    public AttributePattern(NameClass _name, Pattern _body) {
        super(_name, _body);
    }
    
    public Object apply( PatternFunction f ) {
        return f.attribute(this);
    }
}
