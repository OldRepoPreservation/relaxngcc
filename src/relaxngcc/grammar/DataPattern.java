package relaxngcc.grammar;

import org.xml.sax.Locator;
import relaxngcc.parser.ParserRuntime;
import relaxngcc.datatype.Datatype;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DataPattern extends Pattern {
    
    public DataPattern(ParserRuntime rt, Locator loc, Datatype type_, String alias_) {
        this.alias = alias_;
        this.type = type_;
        this.locator = loc;
    }
    
    public final Datatype type;
    public final String alias;
    public final Locator locator;
    
    public Object apply( PatternFunction f ) {
        return f.data(this);
    }
}
