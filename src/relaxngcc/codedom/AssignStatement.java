package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class AssignStatement extends CodeDOMRoot implements Statement {

	private Expression _Destination;
	private Expression _Source;

	public AssignStatement(Expression destination, Expression source) {
		_Destination = destination;
		_Source = source;
	}
	

    public void state(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	_Destination.express(param, writer);
    	writer.write(" = ");
    	_Source.express(param, writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }
}
