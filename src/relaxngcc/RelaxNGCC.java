/*
 * RelaxNGCC.java
 *
 * Created on 2001/08/06, 22:32
 */

package relaxngcc;
import java.io.File;
import java.io.PrintStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import relaxngcc.parser.RootParserRuntime;


/**
 * main class
 */
public class RelaxNGCC
{
	private static final DocumentBuilderFactory _domFactory;
	private static final SAXParserFactory _saxFactory;
    
    static {
        _domFactory = DocumentBuilderFactory.newInstance();
        _domFactory.setNamespaceAware(true);
        _domFactory.setValidating(false);
        
        _saxFactory = SAXParserFactory.newInstance();
        _saxFactory.setNamespaceAware(true);
        _saxFactory.setValidating(false);
    }	
    
	public static void main(String[] args) throws Exception {
        Options o;
        try {
            o = new Options(args);
        } catch( CommandLineException e ) {
            printUsage(e.getMessage(),System.err);
            return;
        }
		
        if(!checkDependencies(o)) return;
		
        run(o);
    }
    
    /**
     * Executes RelaxNGCC with the specified options.
     */
    public static void run( Options o ) throws Exception {
        
        if(o._purifiedSchema!=null) {
            purify( o.sourcefile, o._purifiedSchema );
            return;
        }
        
        // TODO: this code should be moved to somewhere else.
        try {// debug
            RootParserRuntime parser = new RootParserRuntime();
            parser.parse(o.sourcefile.getPath());
            NGCCGrammar grammar = parser.getResult();
            
            grammar.buildAutomaton();
            
            
            boolean uptodate=false; // set to true if there is no need for processing
            
            // generate code first.
            if(!o.noCodeGeneration) {
                uptodate = !grammar.output(o,parser.getGrammarTimestamp());
                if(uptodate)
                    System.out.println("files are up-to-date.");
            }
            
            // process debug options
            if(!uptodate) {
                if(o.printFirstFollow)    grammar.dump(System.err);
                if(o.printAutomata!=null) grammar.dumpAutomata(o.printAutomata);
            }
        } catch( SAXException e ) {
            if( e instanceof SAXParseException )
                System.err.println(((SAXParseException)e).getSystemId());
            if(e.getException()!=null)
                throw e.getException();
            throw e;
        }
	}

    /**
     * Removes RelaxNGCC annotations from the source schema
     * and write to the specified file.
     */
    public static void purify( File in, File out ) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(
            RelaxNGCC.class.getClassLoader().getResourceAsStream("purify.xsl")));
            
        transformer.transform(new StreamSource(in),new StreamResult(out));
    }    
    
    
    
    /**
     * Checks the existance of libraries that are necessary to run RelaxNGCC.
     */
	private static boolean checkDependencies(Options o) {
		try {
			Class.forName("javax.xml.parsers.DocumentBuilderFactory");
		} catch (ClassNotFoundException e) {
			System.err.println("[Error] JAXP is not in your classpath.");
			return false;
		}

		try {
			Class.forName("com.sun.msv.grammar.Grammar");
//			o.msv_available = true;
		} catch (ClassNotFoundException e) {
			System.err.println(
				"[Warning] MSV(Multi Schema Validator) is not found. If the input RELAX NG grammar is wrong syntactically and MSV is not available, RelaxNGCC terminates with Exception. ");
		}

		return true;
	}

    /**
     * Prints the usage screen.
     */
    private static void printUsage( String msg, PrintStream s ) {
        
        if(msg==null)       s.println(msg);
        
        s.println("RELAX NG Compiler Compiler 1.0");
        s.println("   Copyright(c) Daisuke Okajima and Kohsuke Kawaguchi 2001-2002");
        s.println();
        s.println("[Usage]");
        s.println("relaxngcc.jar [options] <grammarfile>");
        s.println();
        s.println("[Options]");
// --plainsax mode is the only mode that works right now.
//        s.println(" --msv");
//        s.println("   generates code based on TypedContentHandler interface of MSV.");
//        s.println(" --typedsax");
//        s.println("   generates code that depends on only XML Schema Datatype Library, not MSV.");
//        s.println(" --nonxml");
//        s.println("   declares that the input grammar is written in the non-XML syntax.");
//        s.println(" --plainsax(default)");
//        s.println("   generates code that depends on only SAX2 parser. This is the most simple case but no datatypes are supported.");
        s.println(" --target <dir>");
        s.println("   specifies the source code output location.");
        s.println(" --purify <outFileName>");
        s.println("   removes RelaxNGCC annotation from the grammar file and write to this file.");
        s.println(" --uptodatecheck");
        s.println("   don't generate files if they are up-to-date.");
        s.println(" --debug");
        s.println("   emit a lot of debug codes in the generated code");
        s.println();
        s.println(" For more information, see http://www.relaxngcc.sourceforge.net/ ");
        s.println();
    }
}
