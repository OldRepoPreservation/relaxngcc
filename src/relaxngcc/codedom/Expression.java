package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public abstract class Expression extends CodeDOMRoot {
    /** Invokes a method on this expression. */
    public MethodInvokeExpression invoke( String method ) {
        return new MethodInvokeExpression(this,method);
    }

    /** Prints itself as an expression. */
    protected abstract void express( OutputParameter param, Writer writer) throws IOException;

}