package relaxngcc.parser;

import relaxngcc.parser.state.IncludedGrammarState;

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
        start=new IncludedGrammarState(this);
        setRootHandler(start);
        
        // inherit context from the parent
        this.grammar = parent.grammar;
        this.parent = parent;
        this.nsStack.add(parent.getCurrentAttributes()); // inherit the ns attribute
    }
    
    /** The root state object that we use to parse the RELAX NG grammar. */
    private final IncludedGrammarState start;
    
    /** Parent runtime object. */
    private final ParserRuntime parent;
    
    public void appendGlobalImport( String code ) {
        parent.appendGlobalImport(code);
    }
    
    public void appendGlobalBody( String code ) {
        parent.appendGlobalBody(code);
    }

    protected void checkLastModifiedTime( long time ) {
        parent.checkLastModifiedTime(time);
    }
}

