package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class AssignStatement extends Statement {

	private Expression _Destination;
	private Expression _Source;

	public AssignStatement(Expression destination, Expression source) {
		_Destination = destination;
		_Source = source;
	}
	

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	_Destination.writeTo(param, writer);
    	writer.write(" = ");
    	_Source.writeTo(param, writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }
}
