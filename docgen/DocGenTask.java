import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;


/**
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DocGenTask extends Task {
    private String language;
    private final Set files = new HashSet();
    
    public void setLanguage( String lang ) {
        this.language = lang;
    }
    
    public void addConfiguredSrc( FileSet fs ) {
        DirectoryScanner ds = fs.getDirectoryScanner(project);
        String[] includedFiles = ds.getIncludedFiles();
        File baseDir = ds.getBasedir();
        
        for (int j = 0; j < includedFiles.length; ++j) {
            files.add(new File(baseDir, includedFiles[j]));
        }
    }
    
    public void execute() {
        for (Iterator itr = files.iterator(); itr.hasNext();) {
            File src = (File) itr.next();
            File dst = new File(new File(src.getParentFile(), language), src.getName());
            try {
                DocGen.convert(src, dst, language);
            } catch (IOException e) {
                throw new BuildException(e);
            }
        }
    }
}
