package relaxngcc.grammar;

import org.xml.sax.Locator;
import relaxngcc.parser.ParserRuntime;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttributePattern extends MarkupPattern {
    public AttributePattern(ParserRuntime rt, Locator sloc, NameClass name, Pattern body) {
        
        super(sloc, rt.createLocator(), name, body);
    }
    
    public Object apply( PatternFunction f ) {
        return f.attribute(this);
    }
}
