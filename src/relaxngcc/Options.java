/*
 * Options.java
 *
 * Created on 2001/08/14, 22:14
 */

package relaxngcc;
import java.io.File;

/**
 * RelaxNGCC behavior options
 */
public class Options
{
	public static final int STYLE_MSV = 0;
	public static final int STYLE_TYPED_SAX = 1;
	public static final int STYLE_PLAIN_SAX = 2;
	
	public static final int NORMAL = 0;
	public static final int NONXML = 1;
	
	public String sourcefile;
	public String targetdir;
	public int style;
	public int input;
	public boolean msv_available;
	public boolean from_include;
	public boolean debug;
	public String newline;
	
    /** Print automata. A debug option. */
    public boolean printAutomata;
    
    /** Print FIRST and FOLLOW. A debug option. */
    public boolean printFirstFollow;
    
	public Options(String[] args)
	{
		input = NORMAL;
		newline = System.getProperty("line.separator");
		sourcefile = args[args.length-1]; //last argument
		File src = new File(sourcefile);
		if(src.isAbsolute())
			targetdir = src.getParent();
		else
			targetdir = new File(System.getProperty("user.dir"), sourcefile).getParent();
		style = STYLE_PLAIN_SAX;
		from_include = false;
		
		for(int i=0; i<args.length - 1; i++)
		{
			if(args[i].equals("--target"))
				targetdir = args[++i];
			else if(args[i].equals("--msv"))
				style = STYLE_MSV;
			else if(args[i].equals("--typedsax"))
				style = STYLE_TYPED_SAX;
			else if(args[i].equals("--plainsax"))
				style = STYLE_PLAIN_SAX;
			else if(args[i].equals("--nonxml"))
				input = NONXML;
			else if(args[i].equals("-d"))
				debug = true;
            else if(args[i].equals("--print-automata"))
                printAutomata = true;
            else if(args[i].equals("--print-first-follow"))
                printFirstFollow = true;
			else
				System.err.println("[Warning] Unknown option " + args[i]);
		}
	}
}
