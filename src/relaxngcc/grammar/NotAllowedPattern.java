package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NotAllowedPattern extends Pattern {
    public NotAllowedPattern(ParserRuntime rt) {}
    
    public Object apply( PatternFunction f ) {
        return f.notAllowed(this);
    }
}
