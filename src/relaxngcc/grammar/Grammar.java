package relaxngcc.grammar;

import java.util.Hashtable;
import java.util.Map;

/**
 * Set of {@link Scope}s.
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Grammar {
    
    /** from pattern name to Scope. */
    private final Map patterns = new Hashtable();
    
    /** Gets the Scope object or return null. */
    public Scope get( String name ) {
        return (Scope)patterns.get(name);
    }
    
    public Scope getOrCreate( String name ) {
        Scope s = get(name);
        if(s==null)
            patterns.put(name,s=new Scope(name));
        return s;
    }
    
    public final Scope start = new Scope(null);
}
