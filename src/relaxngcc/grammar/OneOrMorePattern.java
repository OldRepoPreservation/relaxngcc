package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class OneOrMorePattern extends Pattern {
    public OneOrMorePattern(ParserRuntime rt, Pattern p_) {
        this.p = p_;
    }
    
    public final Pattern p;
    
    public Object apply( PatternFunction f ) {
        return f.oneOrMore(this);
    }
}
