package relaxngcc.datatype;

import java.util.HashMap;
import java.util.Map;

import relaxngcc.NGCCGrammar;
import relaxngcc.Options;

/**
 * Maintains datatypes and their converter definitions
 * and allow {@link Datatype} objects to be retrieved.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DatatypeLibraryManager {
    
    /** Name to {@link DatatypeLibrary} map. */
    private final Map libraries = new HashMap();
    
    protected final Options options;
    
    protected final NGCCGrammar grammar;
    
    public DatatypeLibraryManager( Options _opt, NGCCGrammar _grammar ) {
        this.options = _opt;
        this.grammar = _grammar;
    }
   
    protected void addLibrary( DatatypeLibrary lib ) {
        libraries.put( lib.getNamespaceUri(), lib );
    }
    
    /**
     * Obtains a datatype library by its name.
     * 
     * @return
     *      Always return non-null valid object.
     */
    public DatatypeLibrary getLibrary( String name ) {
        if(libraries.containsKey(name))
            return (DatatypeLibrary)libraries.get(name);
        else
            // return a library with no definition
            return new DatatypeLibrary(name);
    }
}
