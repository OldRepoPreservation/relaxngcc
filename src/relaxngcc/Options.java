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

	public String sourcefile;
	public String targetdir;
	public boolean msv_available;
	public boolean from_include;
	public boolean debug;
	public String newline;
	
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
    
    
	public Options(String[] args) throws CommandLineException {
        
//		input = NORMAL;
		newline = System.getProperty("line.separator");
//		style = STYLE_PLAIN_SAX;
		from_include = false;
		
		for(int i=0; i<args.length; i++)
		{
            if(args[i].charAt(0)=='-') {
    			if(args[i].equals("--target"))
    				targetdir = args[++i];
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
    			else
                    throw new CommandLineException(
                        "[Warning] Unknown option "+args[i]);
            } else {
                if(sourcefile!=null)
                    throw new CommandLineException(
                        "[Warning] Two source files are specified "+args[i]);
                sourcefile = args[i];
            }
		}
        
        if(sourcefile==null)
            throw new CommandLineException("grammar file is missing");
        
        if(targetdir==null) {
            // compute the default target directory
            File src = new File(sourcefile);
            if(src.isAbsolute())
                targetdir = src.getParent();
            else
                targetdir = ".";
        }
	}
}
