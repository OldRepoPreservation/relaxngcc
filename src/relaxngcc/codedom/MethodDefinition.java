package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class MethodDefinition {

	private LanguageSpecificString _ForwardSpecifier;
	private TypeDescriptor _ReturnType;
	private String _Name;
	private LanguageSpecificString _BackwardSpecifier;
    /** Parameters to this method. List of VariableDeclaration. */
    private final ArrayList _Params = new ArrayList();
	
	private final StatementVector _Body = new StatementVector();
	
	public MethodDefinition(LanguageSpecificString forwardspecifier,
        TypeDescriptor returntype, String name,
        LanguageSpecificString backwardspecifier ) {
		
        _ForwardSpecifier = forwardspecifier;
		_ReturnType = returntype;
		_Name = name;
		_BackwardSpecifier = backwardspecifier;
	}
    
    /**
     * Adds a new parameter to this method and returns a reference
     * to it.
     */
    public VariableDeclaration param( TypeDescriptor type, String name ) {
        VariableDeclaration v = new VariableDeclaration(null,type,name,null);
        _Params.add(v);
        return v;
    }
    
    /** Gets a reference to the method body. */
    public StatementVector body() { return _Body; }

    public void writeTo( Formatter f ) throws IOException {

    	if(_ForwardSpecifier!=null)
            f.write(_ForwardSpecifier);
    	
    	if(_ReturnType!=null)
            f.type(_ReturnType);
        
        f.p(_Name).p('(');
        
        boolean first=true;
        for (Iterator itr = _Params.iterator(); itr.hasNext();) {
            if(!first)  f.p(',');
            first = false;
            
            f.declare((VariableDeclaration) itr.next());
        }
        f.p(')');
		
    	if(_BackwardSpecifier!=null)
            f.write(_BackwardSpecifier);
    	
        f.state(_Body).nl();
    	
    }

}
