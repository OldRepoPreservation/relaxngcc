package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class VariableExpression extends Expression {
    
    private String _Name;
    
    public VariableExpression(String name) {
    	_Name = name;
    }
    
    public void express( Formatter f ) throws IOException {
    	f.p(_Name);
    }

}
