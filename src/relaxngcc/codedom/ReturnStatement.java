package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

public class ReturnStatement extends CodeDOMRoot implements Statement {

	private Expression _Expression;
	
    /** use StatementVector to create an instance. */
	ReturnStatement(Expression expr) {
		_Expression = expr;
	}

    public void state(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	writer.write("return ");
    	_Expression.express(param, writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }

}
