package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public abstract class Expression {
    
    /** Invokes a method on this expression. */
    public MethodInvokeExpression invoke( String method ) {
        return new MethodInvokeExpression(this,method);
    }
    
    /** Refers to an item of the array */
    public Expression arrayRef( final Expression index ) {
        return new Expression() {
            public void express( Formatter f) throws IOException {
                f.express(Expression.this).p('[').express(index).p(']');
            }
        };
    }
    
    public Expression arrayRef( int index ) {
        return arrayRef( new ConstantExpression(index) );
    }
    
    /** Refers to a property of this expression. */
    public Expression prop( final String name ) {
        return new Expression() {
            public void express(Formatter f) throws IOException {
                f.express(Expression.this).p('.').p(name);
            }
        };
    }

    /** Prints itself as an expression. */
    protected abstract void express( Formatter f ) throws IOException;

}
