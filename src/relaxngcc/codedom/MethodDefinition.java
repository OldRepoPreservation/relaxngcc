package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class MethodDefinition extends CodeDOMRoot {

	private LanguageSpecificString _ForwardSpecifier;
	private TypeDescriptor _ReturnType;
	private String _Name;
	private TypeDescriptor[] _ArgTypes;
	private String[] _Args;
	private LanguageSpecificString _BackwardSpecifier;
	
	private StatementVector _Body;
	
	public MethodDefinition(LanguageSpecificString forwardspecifier, TypeDescriptor returntype, String name, TypeDescriptor[] argtypes, String[] args, LanguageSpecificString backwardspecifier, StatementVector body) {
		_ForwardSpecifier = forwardspecifier;
		_ReturnType = returntype;
		_Name = name;
		_ArgTypes = argtypes;
		_Args = args;
		_BackwardSpecifier = backwardspecifier;
		_Body = body;
		
		/* debugging
		if(_ArgTypes!=null) {
			for(int i=0; i<_ArgTypes.length; i++) {
				if(_ArgTypes[i]==null) throw new IllegalArgumentException("null type is invlalid");
			}
		}
		*/
	}

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
		if(_ArgTypes!=null) {
			for(int i=0; i<_ArgTypes.length; i++) {
				if(i > 0) writer.write(", ");
				_ArgTypes[i].writeTo(param, writer);
				writer.write(" ");
				writer.write(_Args[i]);
			}
		}
		writer.write(")");
		
    	if(_BackwardSpecifier!=null) {
	    	writer.write(" ");
    		_BackwardSpecifier.writeTo(param, writer);
    	}
    	
    	writer.write(" {");
    	writer.write(NEWLINE);
    	
    	param.incrementIndent();
    	if(_Body!=null) {
   			_Body.writeTo(param, writer);
    	}
    	param.decrementIndent();
    	writeIndent(param, writer);
    	writer.write("}");
    	writer.write(NEWLINE);
    	
    }

}
