package relaxngcc.datatype;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
    
    public DatatypeLibraryManager( Options _opt ) {
        this.options = _opt;
    }
    
    protected DatatypeLibrary getOrCreateLibrary( String name ) {
        if(libraries.containsKey(name))
            return (DatatypeLibrary)libraries.get(name);
        else {
            DatatypeLibrary lib = new DatatypeLibrary(name);
            libraries.put(name,lib);
            return lib;
        }
    }
    
    /**
     * Parses a datatype definition XML file and adds it to
     * this manager.
     * 
     * @exception
     *      if an error happens.
     */
    public void parse( InputSource source ) throws SAXException, IOException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            XMLReader reader = spf.newSAXParser().getXMLReader();
            reader.setContentHandler(new ParserRuntime(this));
            reader.parse(source);
        } catch( ParserConfigurationException e ) {
            // can't happen
            e.printStackTrace();
            throw new InternalError();
        }
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
