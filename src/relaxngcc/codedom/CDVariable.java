package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * CDVariable. Can be used as an CDExpression to refer to this variable.
 * 
 */
public class CDVariable extends CDExpression implements CDStatement {

    private CDLanguageSpecificString _Modifier;
	private CDType _Type;
	private String _Name;
	private CDExpression _InitialValue;

    // not directly creatable. Use appropriate factory methods.
	CDVariable(
        CDLanguageSpecificString modifier,
        CDType type, String name, CDExpression initialvalue) {
            
        _Modifier = modifier;
		_Type = type;
		_Name = name;
		_InitialValue = initialvalue;
	}
	
    public void express( CDFormatter f ) throws IOException {
        // as a reference
        f.p(_Name);
    }

    public void declare( CDFormatter f ) throws IOException {
        // as mod type name [=init]

        if(_Modifier != null)
            f.write(_Modifier);
        
        f.type(_Type).p(_Name);
        
        if(_InitialValue != null)
            f.p('=').express(_InitialValue);
    }

    public void state( CDFormatter f ) throws IOException {
        // as a statement
        declare(f);
        f.eos().nl();
    }
}
