package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class MethodDefinition extends CodeDOMRoot {

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

    public void writeTo(OutputParameter param, Writer writer) throws IOException {

    	writeIndent(param, writer);

    	if(_ForwardSpecifier!=null) {
    		_ForwardSpecifier.writeTo(param, writer);
	    	writer.write(" ");
    	}
    	
    	if(_ReturnType!=null) {
    		_ReturnType.writeTo(param, writer);
	    	writer.write(" ");
    	}
    		
    	writer.write(_Name);
		writer.write("(");
        
        boolean first=true;
        for (Iterator itr = _Params.iterator(); itr.hasNext();) {
            if(!first)  writer.write(", ");
            first = false;
            VariableDeclaration v = (VariableDeclaration) itr.next();
            v.declare(param,writer);
        }
		writer.write(")");
		
    	if(_BackwardSpecifier!=null) {
	    	writer.write(" ");
    		_BackwardSpecifier.writeTo(param, writer);
    	}
    	
    	writer.write(" {");
    	writer.write(NEWLINE);
    	
    	param.incrementIndent();
        _Body.writeTo(param, writer);
    	param.decrementIndent();
        
    	writeIndent(param, writer);
    	writer.write("}");
    	writer.write(NEWLINE);
    	
    }

}
