package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class MemberDefinition extends CodeDOMRoot {
	
	private LanguageSpecificString _AccessSpecifier;
	private TypeDescriptor _Type;
	private String _Name;
	private Expression _Initializer;
	
	public MemberDefinition(LanguageSpecificString accessspecifier, TypeDescriptor type, String name) {
		_AccessSpecifier = accessspecifier;
		_Type = type;
		_Name = name;
		_Initializer = null;
	}
	public MemberDefinition(LanguageSpecificString accessspecifier, TypeDescriptor type, String name, Expression initializer) {
		_AccessSpecifier = accessspecifier;
		_Type = type;
		_Name = name;
		_Initializer = initializer;
	}
	

    public void writeTo(OutputParameter param, Writer writer) throws IOException {

    	writeIndent(param, writer);
    	if(_AccessSpecifier != null) {
    		_AccessSpecifier.writeTo(param, writer);
	    	writer.write(" ");
    	}
    	_Type.writeTo(param, writer);
    	writer.write(" ");
    	writer.write(_Name);
    	
    	if(_Initializer!=null) {
    		writer.write(" = ");
    		_Initializer.express(param, writer);
    	}
    	writer.write(";");
    	writer.write(NEWLINE);
    }

}
