package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class MethodDefinition {

	private LanguageSpecificString _PreModifier;
	private TypeDescriptor _ReturnType;
	private String _Name;
	private LanguageSpecificString _PostModifier;
    /** Parameters to this method. List of VariableDeclaration. */
    private final ArrayList _Params = new ArrayList();
	
	private final StatementVector _Body = new StatementVector();
	
	public MethodDefinition(LanguageSpecificString forwardspecifier,
        TypeDescriptor returntype, String name,
        LanguageSpecificString backwardspecifier ) {
		
        _PreModifier = forwardspecifier;
		_ReturnType = returntype;
		_Name = name;
		_PostModifier = backwardspecifier;
	}
    
    /**
     * Adds a new parameter to this method and returns a reference
     * to it.
     */
    public Variable param( TypeDescriptor type, String name ) {
        Variable v = new Variable(null,type,name,null);
        _Params.add(v);
        return v;
    }
    
    /** Gets a reference to the method body. */
    public StatementVector body() { return _Body; }

    public void writeTo( Formatter f ) throws IOException {

    	if(_PreModifier!=null)
            f.write(_PreModifier);
    	
    	if(_ReturnType!=null)
            f.type(_ReturnType);
        
        f.p(_Name).p('(');
        
        boolean first=true;
        for (Iterator itr = _Params.iterator(); itr.hasNext();) {
            if(!first)  f.p(',');
            first = false;
            
            f.declare((Variable) itr.next());
        }
        f.p(')');
		
    	if(_PostModifier!=null)
            f.write(_PostModifier);
    	
        f.state(_Body).nl();
    	
    }

}
