package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
/**
 * Parameters attached to a &lt;ref> pattern.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCCallParam {
    public NGCCCallParam(ParserRuntime rt, String withParams_, String alias_) {
        this.withParams = withParams_;
        this.alias = alias_;
    }
    
    private final String alias;
    
    private String withParams;
    
    public String getAlias() {
        return alias;
    }

    public String getWithParams() {
        return withParams;
    }

    public void setWithParams(String withParams) {
        this.withParams = withParams;
    }
}

