package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
import org.xml.sax.Locator;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ElementPattern extends MarkupPattern {
    public ElementPattern(ParserRuntime rt, Locator sloc, NameClass name, Pattern body) {
        super(sloc, rt.createLocator(), name, body);
    }
    public Object apply( PatternFunction f ) {
        return f.element(this);
    }
}
