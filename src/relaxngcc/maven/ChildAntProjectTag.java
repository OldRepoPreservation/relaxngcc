package relaxngcc.maven;

import java.io.File;

import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.TagSupport;
import org.apache.commons.jelly.XMLOutput;
import org.apache.tools.ant.Project;

/**
 * Creates a new Ant project with a different base directory.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ChildAntProjectTag extends TagSupport {
    
    private File baseDir;
    
    public void doTag(XMLOutput output) throws MissingAttributeException, JellyTagException {
        // I tried to set a different Project object but it didn't work very well.
        Project oldProject =
            (Project)getContext().findVariable("org.apache.commons.jelly.ant.Project");
        
        File oldBaseFile = oldProject.getBaseDir();
        oldProject.setBaseDir(baseDir);
//        System.out.println("replaced project");
        
        invokeBody(output);

        System.out.println("replaced project done");
        oldProject.setBaseDir(oldBaseFile);
    }
    
    public void setBase( File f ) {
        baseDir = f.getParentFile();
    }
}
