package relaxngcc.datatype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import relaxngcc.codedom.CDType;
import relaxngcc.datatype.parser.NGCCRuntime;
import relaxngcc.datatype.parser.Start;

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
    
    ParserRuntime( DatatypeLibraryManager _owner ) {
        this.owner = _owner; 
        
        // set the root handler
        setRootHandler(new Start(this));
    }
    
    
    
    
    
    
    public void setURI( String uri ) {
        library = owner.getOrCreateLibrary(uri);
    }

    /**
     * Creates a new Resource object.
     */
    public void createResource(String name, Macro m) throws SAXException {
        resources.put( name,
            new Resource( owner, name, m ) );
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
        Macro m) throws SAXException {
        
        boolean r = library.addDatatype( name, new Datatype(
            name,
            new CDType(javaType),
            m,
            (Resource[]) resources.toArray(new Resource[resources.size()]) ));
        
        if(r)
            throw new SAXParseException("duplicate definition of datatype '"+name+"'", getLocator() );
    }
}
