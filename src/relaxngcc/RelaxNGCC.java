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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

import com.thaiopensource.relaxng.nonxml.SchemaBuilderImpl;
import com.thaiopensource.relaxng.nonxml.NonXmlSyntax;
import relaxngcc.dom.NGCCElement;
import relaxngcc.dom.W3CDOMElement;
import relaxngcc.dom.NonXmlElement;
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
		if(args.length==0 || args[0].equals("--help") || args[0].equals("-h") || args[0].equals("/?"))
		{ printUsage(); return; }
		
		Options o = new Options(args);
		if(!checkDependencies(o)) return;
		
		_DOMFactory = DocumentBuilderFactory.newInstance();
		_DOMFactory.setNamespaceAware(true);
		_DOMFactory.setValidating(false);
		
		_SAXFactory = SAXParserFactory.newInstance();
		_SAXFactory.setNamespaceAware(true);
		_SAXFactory.setValidating(false);

		NGCCGrammar grm = new NGCCGrammar(o);
        
		grm.buildAutomaton();
		grm.calcFirstAndFollow();
        
		//for debug
		if(o.printFirstFollow)  grm.dump(System.err);
        if(o.printAutomata)     grm.dumpAutomata(new File("."));
            
		grm.output(o.targetdir);
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
	
	public static NGCCElement readNGCCGrammar(Options o, String location) throws NGCCException
	{
		if(o.input==Options.NORMAL)
			return new W3CDOMElement(readGrammar(o, location).getDocumentElement());
		else
		{
			try
			{
				NonXmlSyntax parser = new NonXmlSyntax(new InputStreamReader(new FileInputStream(location)));
				SchemaBuilderImpl sb = new SchemaBuilderImpl();
				parser.Input(sb);
				return NonXmlElement.create(sb.finish(parser.getPreferredNamespace()));
			}
			catch(Exception e)
			{
				e.printStackTrace();
				throw new NGCCException(e);
			}
		}
	}
	
	private static void printUsage()
	{
		PrintStream s = System.err;
		s.println("RELAX NG Compiler Compiler 0.7");
		s.println("   Copyright(c) Daisuke Okajima 2001-2002x");
		s.println();
		s.println("[Usage]");
		s.println("java [java-options] relaxngcc.RelaxNGCC [options] <grammarfile>");
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
		s.println("[Dependency]");
		s.println(" * To use RelaxNGCC, an XML parser must be available via JAXP interface.");
		s.println(" * MSV is not mandatory, but recommended to get detailed error report.");
		s.println();
		s.println(" For more information, see http://homepage2.nifty.com/okajima/relaxngcc/ ");
		s.println();
	}
	
	private static boolean checkDependencies(Options o)
	{
		try
		{
			Class.forName("javax.xml.parsers.DocumentBuilderFactory");
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("[Error] JAXP library is not found. Please check your classpath to use XML parser via JAXP.");
			return false;
		}
		
		try
		{
			Class.forName("com.sun.msv.grammar.Grammar");
			o.msv_available = true;
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("[Warning] MSV(Multi Schema Validator) is not found. If the input RELAX NG grammar is wrong syntactically and MSV is not available, RelaxNGCC terminates with Exception. ");
		}

		return true;
	}
}
