package relaxngcc.parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import relaxngcc.NGCCGrammar;
import relaxngcc.Options;
import relaxngcc.datatype.DatatypeLibraryManager;
import relaxngcc.grammar.Grammar;
import relaxngcc.grammar.NGCCDefineParam;
import relaxngcc.grammar.Pattern;
import relaxngcc.grammar.RefPattern;
import relaxngcc.parser.state.Start;

/**
 * {@link ParserRuntime} that parses grammars as the root definition.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class RootParserRuntime extends ParserRuntime {

    public RootParserRuntime( Options _options ) {
        this.options = _options;
        setRootHandler(start=new Start(this));
        this.datatypeManager = new DatatypeLibraryManager(options);
    }
    
    /** The root state object that we use to parse the RELAX NG grammar. */
    private final Start start;
    
    /** The value specified via the cc:package attribute. */
    private String packageName = "";
    
    /** The value specified via the cc:runtime-type attribute. */
    private String runtimeType = null;
    
    /** Provides the datatype conversions rountines. */
    protected final DatatypeLibraryManager datatypeManager;
    
    private final Options options;

    private String globalImportDecls = "";
    public void appendGlobalImport( String code ) {
        globalImportDecls += code;
    }

    private String globalBody = "";
    public void appendGlobalBody( String code ) {
        globalBody += code;
    }
    
    /**
     * Timestamp of the source grammar file.
     * If other files are included via some mechanism, those are
     * also incorporated.
     */
    private long grammarTimestamp = -1;
    protected void checkLastModifiedTime( long time ) {
        grammarTimestamp = Math.max( grammarTimestamp, time );
    }
    public long getGrammarTimestamp() { return grammarTimestamp; }


    public RootParserRuntime getRootRuntime() {
        return this;
    }
    
    /** Gets the parsed result, or null if there was any error. */
    public NGCCGrammar getResult() {
        Grammar grammar;
        Pattern p = start.getResult();
        if(p instanceof RefPattern) {
            grammar = (Grammar)((RefPattern)p).target;
        } else {
            // if the parsed tree doesn't have the enclosing &lt;grammar>, add one.
            grammar = new Grammar(null);
            grammar.setParam(new NGCCDefineParam(this, "RelaxNGCC_Result",null,null,null,null));
            grammar.append(p,null);
        }
        
        if(runtimeType==null) {
            // if none is specified, defaults to the NGCCRuntime in the target package.
            runtimeType = packageName+".NGCCRuntime";
            if(runtimeType.charAt(0)=='.')
                runtimeType = runtimeType.substring(1);
        }
        
        return new NGCCGrammar(
            grammar,packageName,runtimeType,globalImportDecls,globalBody);
    }

    public void startElement( String uri, String local, String qname, Attributes atts )
        throws SAXException {
            
        String v;
        v = atts.getValue(Const.NGCC_URI,"package");
        if(v!=null) packageName = v;
        
        v = atts.getValue(Const.NGCC_URI,"runtime-type");
        if(v!=null) runtimeType = v;

        
        v = atts.getValue(Const.NGCC_URI,"datatype-defs");
        if(v!=null)
            processDatatypeDefs(v);
        
        super.startElement(uri,local,qname,atts);
    }
    
    /**
     * Parses <code>cc:datatype-defs</code> attributes.
     */
    private void processDatatypeDefs( String list ) throws SAXException {
        StringTokenizer tokens = new StringTokenizer(list);
        while( tokens.hasMoreTokens() ) {
            String uri = tokens.nextToken();
            if( uri.startsWith("builtin://") ) {
                // handle "builtin://XXXX" specially and resolve
                // them to our built-in datatype definition files.
                String body = uri.substring(10);
                URL url = this.getClass().getClassLoader().getResource(
                    "relaxngcc/datatype/builtin/"+body);
                if( url==null )
                    throw new SAXParseException("undefined built-in datatype definition: "+body,getLocator());
                uri = url.toExternalForm();
            }
            
            try {
                datatypeManager.parse(new InputSource(absolutize(uri)));
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }
    }
    
    /**
     * Absolutizes an URI.
     */
    private String absolutize( String uri ) {
        try {
            // try URI first
            return new URI(getLocator().getSystemId()).resolve(uri).toString();
        } catch( Throwable t ) {
            // but URI is since JDK1.4 so it might not be available.
            // if so, try URL.
            try {
                return new URL( new URL(getLocator().getSystemId()), uri ).toExternalForm();
            } catch( MalformedURLException e ) {
                // everything else fails
                return uri;
            }
        }
    }

}

