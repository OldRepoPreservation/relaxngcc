package relaxngcc.codedom;

import java.io.IOException;

/**
 * CDVariable. Can be used as an CDExpression to refer to this variable.
 * 
 */
public class CDVariable extends CDExpression implements CDStatement {

    private final CDLanguageSpecificString _modifier;
    private final CDType _type;
    private final String _name;
    private final CDExpression _initialValue;

    // not directly creatable. Use appropriate factory methods.
    CDVariable(
        CDLanguageSpecificString modifier,
        CDType type, String name, CDExpression initialvalue) {
            
        _modifier = modifier;
        _type = type;
        _name = name;
        _initialValue = initialvalue;
    }
    
    public String getName() { return _name; }
    
    public void express( CDFormatter f ) throws IOException {
        // as a reference
        f.p(_name);
    }

    public void declare( CDFormatter f ) throws IOException {
        // as mod type name [=init]

        if(_modifier != null)
            f.write(_modifier);
        
        f.type(_type).p(_name);
        
        if(_initialValue != null)
            f.p('=').express(_initialValue);
    }

    public void state( CDFormatter f ) throws IOException {
        // as a statement
        declare(f);
        f.eos().nl();
    }
}
