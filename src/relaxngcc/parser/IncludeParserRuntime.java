package relaxngcc.parser;

import relaxngcc.parser.state.IncludedGrammarState;
import relaxngcc.grammar.Grammar;

/**
 * Used to parse grammar included by &lt;include> elements.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class IncludeParserRuntime extends ParserRuntime {

    /**
     * @param
     *      The parent runtime object that created this runtime.
     */
    public IncludeParserRuntime( ParserRuntime parent ) {
        _start = new IncludedGrammarState(this);
        setRootHandler(_start);
        
        // inherit context from the parent
        grammar = parent.grammar;
        _parent = parent;
        _nsStack.add(parent.getTargetNamespace()); // inherit the ns attribute
    }
    
    /** The root state object that we use to parse the RELAX NG grammar. */
    private final IncludedGrammarState _start;
    
    /** Parent runtime object. */
    private final ParserRuntime _parent;

    public RootParserRuntime getRootRuntime() {
        return _parent.getRootRuntime();
    }

    protected void checkLastModifiedTime( long time ) {
        _parent.checkLastModifiedTime(time);
    }
}

