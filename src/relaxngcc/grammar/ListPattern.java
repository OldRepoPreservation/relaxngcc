package relaxngcc.grammar;

import org.xml.sax.Locator;
import relaxngcc.parser.ParserRuntime;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ListPattern extends Pattern {
    public ListPattern(ParserRuntime rt, Locator loc, Pattern p_, String alias_ ) {
        this.p = p_;
        this.alias = alias_;
        this.locator = loc;
    }
    
    public final Pattern p;
    public final String alias;
    public final Locator locator;
    
    public Object apply( PatternFunction f ) {
        return f.list(this);
    }
}
