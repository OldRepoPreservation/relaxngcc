package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ReturnStatement extends Statement {

	private Expression _Expression;
	
	public ReturnStatement(Expression expr) {
		_Expression = expr;
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	writer.write("return ");
    	_Expression.writeTo(param, writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }

}
