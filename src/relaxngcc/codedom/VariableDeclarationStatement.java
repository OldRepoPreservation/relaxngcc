package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class VariableDeclarationStatement extends CodeDOMRoot implements Statement {

	private TypeDescriptor _Type;
	private String _Name;
	private Expression _InitialValue;

	public VariableDeclarationStatement(TypeDescriptor type, String name) {
		_Type = type;
		_Name = name;
		_InitialValue = null;
	}
	public VariableDeclarationStatement(TypeDescriptor type, String name, Expression initialvalue) {
		_Type = type;
		_Name = name;
		_InitialValue = initialvalue;
	}
	

    public void state(OutputParameter param, Writer writer) throws IOException {
    	writeIndent(param, writer);
    	_Type.writeTo(param, writer);
    	writer.write(" ");
    	writer.write(_Name);
    	if(_InitialValue != null) {
	    	writer.write(" = ");
    		_InitialValue.express(param, writer);
    	}
    	writer.write(";");
    	writer.write(NEWLINE);
    }
}
