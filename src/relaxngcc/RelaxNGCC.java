/*
 * RelaxNGCC.java
 *
 * Created on 2001/08/06, 22:32
 */

package relaxngcc;
import java.io.File;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.text.ParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import com.sun.corba.se.internal.core.OSFCodeSetRegistry;
import com.thaiopensource.relaxng.nonxml.SchemaBuilderImpl;
import com.thaiopensource.relaxng.nonxml.NonXmlSyntax;
import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.W3CDOMElement;
import relaxngcc.dom.NonXmlElement;
import relaxngcc.grammar.Grammar;
import relaxngcc.grammar.Pattern;
import relaxngcc.grammar.RefPattern;
import relaxngcc.parser.ParserRuntime;
import relaxngcc.parser.state.Start;
import relaxngcc.builder.ScopeBuilder;
import relaxngcc.builder.CodeWriter;


/**
 * main class
 */
public class RelaxNGCC
{
	private static DocumentBuilderFactory _DOMFactory;
	private static SAXParserFactory _SAXFactory;
	
	public static void main(String[] args) throws Exception
	{
        Options o;
        try {
            o = new Options(args);
        } catch( CommandLineException e ) {
            printUsage(e.getMessage(),System.err);
            return;
        }
		
        if(!checkDependencies(o)) return;
		
		_DOMFactory = DocumentBuilderFactory.newInstance();
		_DOMFactory.setNamespaceAware(true);
		_DOMFactory.setValidating(false);
		
		_SAXFactory = SAXParserFactory.newInstance();
		_SAXFactory.setNamespaceAware(true);
		_SAXFactory.setValidating(false);
        
        if(o.input==o.NEWPARSER) {
            // TODO: this code should be moved to somewhere else.
            try {// debug
	            ParserRuntime runtime = new ParserRuntime();
	            Start s = new Start(runtime);
	            runtime.pushHandler(s);
	            
	            XMLReader reader = _SAXFactory.newSAXParser().getXMLReader();
	            reader.setContentHandler(runtime);
	            reader.parse(o.sourcefile);
                
                Grammar grammar;
                Pattern p = s.getResult();
                if(p instanceof RefPattern) {
                    grammar = (Grammar)((RefPattern)p).target;
                } else {
                    // if the parsed tree doesn't have the enclosing &lt;grammar>, add one.
                    grammar = new Grammar(null);
                    grammar.append(p,null);
                }
                
                // set breakpoint here and debug
                return;
            } catch( SAXException e ) {
                if(e.getException()!=null)
                    throw e.getException();
                throw e;
            }
        }
		NGCCGrammar grm = new NGCCGrammar(o);
        
		grm.buildAutomaton();
        
		// process debug options
		if(o.printFirstFollow)    grm.dump(System.err);
        if(o.printAutomata!=null) grm.dumpAutomata(o.printAutomata);
        
        if(!o.noCodeGeneration) grm.output();
	}

	
	//returns a DOM document after checking validity as a RelaxNGCC grammar
	public static Document readGrammar(Options o, String location) throws NGCCException
	{
		try
		{
			//returns the document as a DOM document
			DocumentBuilder docbuilder = _DOMFactory.newDocumentBuilder();
			Document grm = docbuilder.parse(location);
			String rootnsuri = grm.getDocumentElement().getNamespaceURI();
			if(!rootnsuri.equals(NGCCGrammar.RELAXNG_NSURI_09) && !rootnsuri.equals(NGCCGrammar.RELAXNG_NSURI_10))
				throw new NGCCException("The namespace URI of the root element is not correct as RELAX NG");
			
			NGCCGrammar.RELAXNG_NSURI = rootnsuri;
			
			// load a schema. GrammarLoader will detect the schema language automatically.
			if(o.msv_available && !o.from_include)
			{
				if(GrammarChecker.check(location, _SAXFactory)==null) throw new NGCCException("failed to load grammar [" + location + "]");
			}
			
			return grm;
		}
		catch(Exception e) //IOException, SAXException
		{ throw new NGCCException(e); }
	}
	
	public static NGCCElement readNGCCGrammar(Options o, String location)
		throws NGCCException {
		if (o.input == Options.NORMAL)
			return new W3CDOMElement(
				readGrammar(o, location).getDocumentElement());
		else {
			try {
				NonXmlSyntax parser =
					new NonXmlSyntax(
						new InputStreamReader(new FileInputStream(location)));
				SchemaBuilderImpl sb = new SchemaBuilderImpl();
				parser.Input(sb);
				return NonXmlElement.create(
					sb.finish(parser.getPreferredNamespace()));
			} catch (Exception e) {
				e.printStackTrace();
				throw new NGCCException(e);
			}
		}
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
			o.msv_available = true;
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
        
        s.println("RELAX NG Compiler Compiler 0.8");
        s.println("   Copyright(c) Daisuke Okajima, RelaxNGCC SourceForge Project 2001-2002");
        s.println();
        s.println("[Usage]");
        s.println("relaxngcc.jar [options] <grammarfile>");
        s.println();
        s.println("[Options]");
        s.println(" --msv");
        s.println("   generates code based on TypedContentHandler interface of MSV.");
        s.println(" --typedsax");
        s.println("   generates code that depends on only XML Schema Datatype Library, not MSV.");
        s.println(" --nonxml");
        s.println("   declares that the input grammar is written in the non-XML syntax.");
        s.println(" --plainsax(default)");
        s.println("   generates code that depends on only SAX2 parser. This is the most simple case but no datatypes are supported.");
        s.println(" --target <dir>");
        s.println("   specifies the source code output location.");
        s.println();
        s.println(" For more information, see http://www.relaxngcc.sourceforge.net/ ");
        s.println();
    }
}
