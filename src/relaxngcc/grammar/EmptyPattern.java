package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class EmptyPattern extends Pattern {
    public EmptyPattern(ParserRuntime rt) {}
    
    public Object apply( PatternFunction f ) {
        return f.empty(this);
    }
}
