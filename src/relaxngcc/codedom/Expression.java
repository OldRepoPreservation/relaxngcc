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
    public Expression arrayRef( Expression index ) {
        return new ArrayElementReferenceExpression(this,index);
    }

    /** Prints itself as an expression. */
    protected abstract void express( Formatter f ) throws IOException;

}
