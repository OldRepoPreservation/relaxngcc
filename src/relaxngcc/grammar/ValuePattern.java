package relaxngcc.grammar;

import org.xml.sax.Locator;

import relaxngcc.datatype.Datatype;
import relaxngcc.parser.ParserRuntime;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ValuePattern extends Pattern {
    public ValuePattern(ParserRuntime rt, Locator loc, Datatype type_, String value_, String alias_ ) {
        this.type = type_;
        this.value = value_;
        this.alias = alias_;
        this.locator = loc;
    }
    
    public final Datatype type;
    public final String value;
    public final String alias;
    public final Locator locator;
    
    public Object apply( PatternFunction f ) {
        return f.value(this);
    }
}
