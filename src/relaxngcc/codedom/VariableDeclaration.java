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
	
    public void express( Formatter f ) throws IOException {
        // as a reference
        f.p(_Name);
    }

    public void declare( Formatter f ) throws IOException {
        // as mod type name [=init]

        if(_Modifier != null)
            f.write(_Modifier);
        
        f.type(_Type).p(_Name);
        
        if(_InitialValue != null)
            f.p('=').express(_InitialValue);
    }

    public void state( Formatter f ) throws IOException {
        // as a statement
        declare(f);
        f.eos().nl();
    }
}
