package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

public class ExpressionStatement extends Statement {

	private Expression _Expression;
	
	public ExpressionStatement(Expression e) {
		_Expression = e;
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	_Expression.writeTo(param, writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }

}
