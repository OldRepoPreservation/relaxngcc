package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Variable. Can be used as an Expression to refer to this variable.
 * 
 */
public class VariableDeclaration extends Expression implements Statement {

    private LanguageSpecificString _Modifier;
	private TypeDescriptor _Type;
	private String _Name;
	private Expression _InitialValue;

    // not directly creatable. Use appropriate factory methods.
	VariableDeclaration(
        LanguageSpecificString modifier,
        TypeDescriptor type, String name, Expression initialvalue) {
            
        _Modifier = modifier;
		_Type = type;
		_Name = name;
		_InitialValue = initialvalue;
	}
	
    public void express(OutputParameter param, Writer writer) throws IOException {
        // as a reference
        writer.write(_Name);
    }

    public void declare(OutputParameter param, Writer writer) throws IOException {
        // as mod type name [=init]
        writeIndent(param, writer);

        if(_Modifier != null) {
            _Modifier.writeTo(param, writer);
            writer.write(" ");
        }
        _Type.writeTo(param, writer);
        writer.write(" ");
        writer.write(_Name);
        if(_InitialValue != null) {
            writer.write(" = ");
            _InitialValue.express(param, writer);
        }
    }

    public void state(OutputParameter param, Writer writer) throws IOException {
        // as a statement
        declare(param,writer);
    	writer.write(";");
    	writer.write(NEWLINE);
    }
}
