package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class ClassDefinition extends CodeDOMRoot {

	private LanguageSpecificString[] _PrecedingDeclarations;
	private LanguageSpecificString _ForwardSpecifier;
	private String _ClassName;
	private LanguageSpecificString _BackwardSpecifier;
	private Vector _Members;
	private Vector _Methods;
	private Vector _LanguageSpecificStrings;

	public ClassDefinition(LanguageSpecificString[] declarations, LanguageSpecificString fs, String name, LanguageSpecificString bs) {
		_PrecedingDeclarations = declarations;
		_ForwardSpecifier = fs;
		_ClassName = name;
		_BackwardSpecifier = bs;
		_Members = new Vector();
		_Methods = new Vector();
		_LanguageSpecificStrings = new Vector();
	}

	public void addMember(MemberDefinition memberdef) {
		_Members.add(memberdef);
	}
	public void addMethod(MethodDefinition methoddef) {
		_Methods.add(methoddef);
	}
	public void addLanguageSpecificString(LanguageSpecificString content) {
		_LanguageSpecificStrings.add(content);
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {

    	if(_PrecedingDeclarations!=null) {
	    	for(int i=0; i<_PrecedingDeclarations.length; i++) {
	    		_PrecedingDeclarations[i].writeTo(param, writer);
	    		writer.write(NEWLINE);
	    	}
    	}
    	if(_ForwardSpecifier!=null) {
    		_ForwardSpecifier.writeTo(param, writer);
	    	writer.write(" ");
    	}
    	
    	writer.write("class ");
    	writer.write(_ClassName);

    	if(_BackwardSpecifier!=null) {
	    	writer.write(" ");
    		_BackwardSpecifier.writeTo(param, writer);
    	}
    	
    	writer.write(" {");
    	writer.write(NEWLINE);
    	param.incrementIndent();
    	
    	if(_Members!=null) {
    		for(int i=0; i<_Members.size(); i++) {
    			((MemberDefinition)_Members.get(i)).writeTo(param, writer);
    		}
    	}
    	writer.write(NEWLINE);
    	if(_Methods!=null) {
    		for(int i=0; i<_Methods.size(); i++) {
    			((MethodDefinition)_Methods.get(i)).writeTo(param, writer);
    		}
    	}
    	
    	if(_LanguageSpecificStrings!=null) {
    		for(int i=0; i<_LanguageSpecificStrings.size(); i++) {
    			((LanguageSpecificString)_LanguageSpecificStrings.get(i)).writeTo(param, writer);
    		}
    	}
    	
    	
    	param.decrementIndent();
    	writer.write("}");
    	writer.write(NEWLINE);
    }

}
