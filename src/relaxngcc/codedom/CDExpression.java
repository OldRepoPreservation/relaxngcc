package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public abstract class CDExpression {
    
    /** Invokes a method on this expression. */
    public CDMethodInvokeExpression invoke( String method ) {
        return new CDMethodInvokeExpression(this,method);
    }
    
    /** Refers to an item of the array */
    public CDExpression arrayRef( final CDExpression index ) {
        return new CDExpression() {
            public void express( CDFormatter f) throws IOException {
                f.express(CDExpression.this).p('[').express(index).p(']');
            }
        };
    }
    
    public CDExpression arrayRef( int index ) {
        return arrayRef( new CDConstant(index) );
    }
    
    /** Refers to a property of this expression. */
    public CDExpression prop( final String name ) {
        return new CDExpression() {
            public void express(CDFormatter f) throws IOException {
                f.express(CDExpression.this).p('.').p(name);
            }
        };
    }
    
    /** Creates !x */
    public CDExpression not() {
        return CDOp.not(this);
    }

    /** Prints itself as an expression. */
    protected abstract void express( CDFormatter f ) throws IOException;

}
