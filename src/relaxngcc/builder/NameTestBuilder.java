package relaxngcc.builder;

import java.text.MessageFormat;

import relaxngcc.codedom.Op;
import relaxngcc.codedom.ConstantExpression;
import relaxngcc.codedom.Expression;
import relaxngcc.codedom.Variable;
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
    
    public NameTestBuilder( Variable _uriVar, Variable _localNameVar ) {
        this.$uriVar = _uriVar;
        this.$localNameVar = _localNameVar;
    }
    
    private final Variable $uriVar;
    private final Variable $localNameVar;
    
	public Object choice(NameClass nc1, NameClass nc2) {
        return Op.OR(
            (Expression)nc1.apply(this),
            (Expression)nc2.apply(this));
	}

	public Object nsName(String ns, NameClass except) {
        Expression exp = $uriVar.invoke("equals").arg(new ConstantExpression(ns));
        
        if(except!=null)
            exp = Op.AND( exp,
                ((Expression)except.apply(this)).not() );
        
        return exp;
	}

	public Object anyName(NameClass except) {
        if(except==null)
            return new ConstantExpression(true);
        else
            return ((Expression)except.apply(this)).not();
	}

	public Object name(String ns, String local) {
        return Op.AND(
            Op.STREQ( $uriVar, new ConstantExpression(ns) ),
            Op.STREQ( $localNameVar, new ConstantExpression(local)) );
	}

}

