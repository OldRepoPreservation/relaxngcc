/*
 * Options.java
 *
 * Created on 2001/08/14, 22:14
 */

package relaxngcc;
import java.io.File;
import java.io.PrintStream;
import java.text.ParseException;

/**
 * RelaxNGCC behavior options
 */
public class Options
{
/*
	public static final int STYLE_MSV = 0;
	public static final int STYLE_TYPED_SAX = 1;
	public static final int STYLE_PLAIN_SAX = 2;
    public int style;
*/	

	public File sourcefile;
	public File targetdir;
//	public boolean msv_available;
	public boolean debug       = false;
	public String newline      = System.getProperty("line.separator");
	
    /**
     * Directory to write automata gif files. A debug option.
     * If null, we won't generate automata dumps.
     */
    public File printAutomata;
    
    /** Print FIRST and FOLLOW. A debug option. */
    public boolean printFirstFollow;
    
    /** Do not generate source code. */
    public boolean noCodeGeneration;
    
    /** Uses a private copy of runtime code. */
    public boolean usePrivateRuntime=true;
    
    /** Don't overwrite files when the Java files are up-to-date. */
    public boolean smartOverwrite=false;
    
    /** Creates Options filled by the default values. */
    public Options() {}
    
    /** Parses option list. */
	public Options(String[] args) throws CommandLineException {
        
//		input = NORMAL;
//		style = STYLE_PLAIN_SAX;
		
		for(int i=0; i<args.length; i++)
		{
            if(args[i].charAt(0)=='-') {
    			if(args[i].equals("--target"))
    				targetdir = new File(args[++i]);
//    			else if(args[i].equals("--msv"))
//    				style = STYLE_MSV;
//    			else if(args[i].equals("--typedsax"))
//    				style = STYLE_TYPED_SAX;
//    			else if(args[i].equals("--plainsax"))
//    				style = STYLE_PLAIN_SAX;
    			else if(args[i].equals("-d"))
    				debug = true;
                else if(args[i].equals("--debug"))
                    debug = true;
                else if(args[i].equals("--print-automata"))
                    printAutomata = new File(args[++i]);
                else if(args[i].equals("--print-first-follow"))
                    printFirstFollow = true;
                else if(args[i].equals("--no-code"))
                    noCodeGeneration = true;
                else if(args[i].equals("--uptodatecheck"))
                    smartOverwrite = true;
    			else
                    throw new CommandLineException(
                        "[Warning] Unknown option "+args[i]);
            } else {
                if(sourcefile!=null)
                    throw new CommandLineException(
                        "[Warning] Two source files are specified "+args[i]);
                sourcefile = new File(args[i]);
            }
		}
        
        if(sourcefile==null)
            throw new CommandLineException("grammar file is missing");
        
        if(targetdir==null) {
            // compute the default target directory
            targetdir = sourcefile.getParentFile();
        }
	}
}
