package relaxngcc.ant;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;

import org.xml.sax.XMLReader;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.ParserAdapter;
import relaxngcc.Options;
import relaxngcc.RelaxNGCC;


/**
 * Ant task that invokes RelaxNGCC.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class RelaxNGCCTask extends Task {

    /** Source RELAX NG grammar. */
	private File source;
    /** Target directory. */
	private File target;
    /** Directory to output automata gif files. */
    private File automataDir;

	public void execute() throws BuildException {
		if( source==null )
			throw new BuildException("The source attribute is required",location);
		if( target==null )
            throw new BuildException("The targetdir attribute is required",location);

//		ErrorHandlerImpl eh =
//			new ErrorHandlerImpl(
				new PrintStream(new LogOutputStream(this, Project.MSG_WARN)); // );

		boolean hadError = false;
        
        Options opt = new Options();
        opt.sourcefile = source;
        opt.targetdir = target;
        opt.smartOverwrite = true;
        
        opt.printAutomata = automataDir;
        
        try {
            RelaxNGCC.run(opt);
        } catch( Exception e ) {
            e.printStackTrace();
            throw new BuildException(
                "Validation failed, messages should have been provided.",
                e, location);
        }
	}


	public void setSource(String rngFile) {
		source = project.resolveFile(rngFile);
	}

	public void setTargetdir(File file) {
		this.target = file;
	}

    public void setAutomata( String dir ) {
        automataDir = project.resolveFile(dir);
    }

}
