package relaxngcc.parser;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import relaxngcc.grammar.Grammar;
import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.SimpleNameClass;
import relaxngcc.parser.state.*;

/**
 * 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ParserRuntime extends NGCCRuntime {
    
    /** Parses a QName into a SimpleNameClass. */
    public NameClass parseSimpleName( String qname, boolean attributeMode ) {
        String uri,local;
        
        int idx = qname.indexOf(':');
        
        if(idx<0) {
            if(attributeMode) {
                uri="";
                local=qname;
            } else {
                uri=resolveNamespacePrefix("");
                local=qname;
            }
        } else {
            String prefix = qname.substring(0,idx);
            uri = resolveNamespacePrefix(prefix);
            if(uri==null) {
                // TODO: undeclared prefix
                throw new UnsupportedOperationException();
//                uri = "";
            }
            local = qname.substring(idx+1);
        }
        
        return new SimpleNameClass(uri,local);
    }
    
    /** Grammar object that we are currently building. */
    public Grammar grammar;
    
    /** Processes the &lt;include> element. */
    public void processInclude( String href ) {
        throw new UnsupportedOperationException();
    }
    
    /** Any global-scope &lt;cc:java-import> will be reported here. */
    public void appendGlobalImports( String code ) {
        System.out.println("\nglobal import:\n"+code+"\n");
        // TODO
    }
    
    
    
    /** Keeps track of values of the ns attribute. */
    private final Stack nsStack = new Stack();
    {// register the default binding
        nsStack.push("");
    }
    
    /** Gets the value of the current "ns". */
    public String getTargetNamespace() {
        return (String)nsStack.peek();
    }
    
    
    // override start/endElement to handle the ns attribute
    // TODO: handle datatypeLibrary attribute
    public void startElement( String uri, String local, String qname, Attributes atts )
        throws SAXException {
            
        String ns = atts.getValue("ns");
        if(ns==null)    ns = getTargetNamespace();
        nsStack.push(ns);
        
        super.startElement(uri,local,qname,atts);
    }
    
    public void endElement( String uri, String local, String qname )
        throws SAXException {
            
        super.endElement(uri,local,qname);
        nsStack.pop();
    }
}

