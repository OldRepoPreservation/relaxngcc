package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

public class ReturnStatement implements Statement {

	private Expression _Expression;
	
    /** use StatementVector to create an instance. */
	ReturnStatement(Expression expr) {
		_Expression = expr;
	}

    public void state(Formatter f) throws IOException {
        f.p("return").express(_Expression).eos().nl();
    }

}
