package relaxngcc.grammar;

import relaxngcc.MetaDataType;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DataPattern extends Pattern {
    
    public DataPattern( MetaDataType _type, String _alias ) {
        this.alias = _alias;
        this.type = _type;
    }
    
    public final MetaDataType type;
    public final String alias;
    
    public Object apply( PatternFunction f ) {
        return f.data(this);
    }
}
