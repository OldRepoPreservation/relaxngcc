package relaxngcc.datatype;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * "Resource" is a file used by datatype conversion routine.
 * Typically, this is a utility Java class.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class Resource {
    
    private final DatatypeLibraryManager owner;
    
    /**
     * Resource name.
     */
    private final String name;
    
    /**
     * Contents of the resource.
     */
    private final byte[] contents;
    
    /**
     * A flag for avoiding duplicate generation.
     * True if this resource has already been used.
     */
    private boolean used = false;
    
    
    Resource(DatatypeLibraryManager _owner, String _name, byte[] _contents) {
        this.owner = _owner;
        this.name = _name;
        this.contents = _contents;
    }
    
    
    /**
     * Called by the datatype conversion routine to indicate
     * that this resource is in use.
     */
    public void use() throws IOException {
        if( !used ) {
            // generate resource
            FileOutputStream fos = new FileOutputStream(
                new File( owner.options.targetdir, name ) );
            fos.write(contents);
            fos.close();
        }
        used = true;
    }

}
