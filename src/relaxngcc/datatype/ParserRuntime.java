package relaxngcc.datatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import relaxngcc.codedom.CDType;
import relaxngcc.datatype.parser.NGCCRuntime;

/**
 * Parses a datatype definition file.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class ParserRuntime extends NGCCRuntime {

    private final DatatypeLibraryManager owner;
    private DatatypeLibrary library;
    
    /**
     * Map from resource name to {@link Resource} object.
     */
    private final Map resources = new HashMap();
    
    /**
     * Macro definitions for resource files.
     */
    private final Map resourceMacroDefs = new HashMap();
    
    ParserRuntime( DatatypeLibraryManager _owner ) {
        this.owner = _owner; 
        
        // build definition dictionary
        resourceMacroDefs.put("packageDecl","package "+owner.grammar.packageName+";");
    }
    
    public void setURI( String uri ) {
        library = new DatatypeLibrary(uri);
        owner.addLibrary(library);
    }

    /**
     * Creates a new Resource object.
     */
    public void createResource(String name, Macro m) throws SAXException {
        try {
            resources.put( name,
                new Resource( owner, name, m.toString(resourceMacroDefs).getBytes() ));
        } catch (NoDefinitionException e) {
            throw new SAXParseException(e.getMessage(), e.locator );
        }
    }

    /**
     * Obtains a reference to a resource.
     * 
     * @return
     *      non-null valid object.
     */
    public Resource getResource(String resName) throws SAXException {
        if( resources.containsKey(resName) )
            return (Resource)resources.get(resName);
        else {
            // undefined resource
            throw new SAXParseException("undefined resource '"+resName+"'", getLocator() );
        }
    }

    public void createDatatype(
        String name,
        String javaType,
        ArrayList resources,
        Macro m) {
        
        library.addDatatype( name, new Datatype( new CDType(javaType), m,
            (Resource[]) resources.toArray(new Resource[resources.size()]) ));
    }
}
