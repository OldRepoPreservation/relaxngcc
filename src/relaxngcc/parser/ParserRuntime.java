package relaxngcc.parser;

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
    
    /** Gets the value of the current "ns". */
    public String getTargetNamespace() {
        throw new UnsupportedOperationException();
    }
}

