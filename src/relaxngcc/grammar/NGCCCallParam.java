package relaxngcc.grammar;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCCallParam {
    public NGCCCallParam( String _withParams, String _alias ) {
        this.withParams = _withParams;
        this.alias = _alias;
    }
    
    public final String alias;
    public final String withParams;
}

