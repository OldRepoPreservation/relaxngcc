package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class PropertyReferenceExpression extends Expression {

	private Expression _Object;
	private String _PropertyName;

	public PropertyReferenceExpression(Expression obj, String propertyname) {
		_Object = obj;
		_PropertyName = propertyname;
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	_Object.writeTo(param, writer);
    	writer.write(".");
    	writer.write(_PropertyName);
    }

}
