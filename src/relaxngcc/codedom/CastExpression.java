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
public class CastExpression extends Expression {

	private TypeDescriptor _Type;
	private Expression _Expression;

	public CastExpression(TypeDescriptor type, Expression expr) {
		_Type = type;
		_Expression = expr;
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	writer.write("((");
    	_Type.writeTo(param, writer);
    	writer.write(")");
    	_Expression.writeTo(param, writer);
        writer.write(")");
    }

}
