package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

public class CDReturnStatement implements CDStatement {

	private CDExpression _Expression;
	
    /** use CDBlock to create an instance. */
	CDReturnStatement(CDExpression expr) {
		_Expression = expr;
	}

    public void state(CDFormatter f) throws IOException {
        f.p("return").express(_Expression).eos().nl();
    }

}
