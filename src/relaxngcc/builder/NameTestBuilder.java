package relaxngcc.builder;

import java.text.MessageFormat;

import relaxngcc.grammar.NameClass;
import relaxngcc.grammar.NameClassFunction;

/**
 * Generates a clause that tests the membership of a NameClass.
 * 
 * <p>
 * This function returns {@link String}.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NameTestBuilder implements NameClassFunction {
    
    public NameTestBuilder( String _uriVar, String _localNameVar ) {
        this.uriVar = _uriVar;
        this.localNameVar = _localNameVar;
    }
    
    private final String uriVar;
    private final String localNameVar;
    
	public Object choice(NameClass nc1, NameClass nc2) {
        return MessageFormat.format("({0})||({1})",
            new Object[]{ nc1.apply(this), nc2.apply(this) });
	}

	public Object nsName(String ns, NameClass except) {
        if(except==null)
            return MessageFormat.format("({0}.equals(\"{1}\")",
                new Object[]{ uriVar, ns });
        else
	        return MessageFormat.format("({0}.equals(\"{1}\")&& !({2})",
	            new Object[]{ uriVar, ns, except.apply(this) });
	}

	public Object anyName(NameClass except) {
        if(except==null)
            return "true";
        else
            return MessageFormat.format("!({0})",
                new Object[]{ except.apply(this) });
	}

	public Object name(String ns, String local) {
        return MessageFormat.format("({0}.equals(\"{1}\") && {2}.equals(\"{3}\"))",
            new Object[]{ uriVar, ns, localNameVar, local });
	}

}

