package relaxngcc.codedom;

import java.io.IOException;

/**
 * CDExpression as a statement.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class CDExpressionStatement implements CDStatement {
    CDExpressionStatement( CDExpression exp ) {
        this._exp = exp;
    }
    
    private final CDExpression _exp;
    
    public void state(CDFormatter f) throws IOException {
        f.express(_exp).eos().nl();
    }

}
