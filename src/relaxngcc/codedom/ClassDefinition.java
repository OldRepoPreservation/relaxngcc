package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class ClassDefinition {

	private LanguageSpecificString[] _PrecedingDeclarations;
	private LanguageSpecificString _PreModifier;
    private LanguageSpecificString _PostModifier;
	private String _ClassName;
    
	private final Vector _Members = new Vector();
	private final Vector _Methods = new Vector();
	
    private final Vector _AdditionalBody = new Vector();

	public ClassDefinition(LanguageSpecificString[] declarations, LanguageSpecificString fs, String name, LanguageSpecificString bs) {
		_PrecedingDeclarations = declarations;
		_PreModifier = fs;
		_ClassName = name;
		_PostModifier = bs;
	}

    /** Adds a new member declaration. */
	public Variable addMember(
        LanguageSpecificString modifier,
        TypeDescriptor type, String name, Expression initialValue) {
            
        Variable var = new Variable(modifier,type,name,initialValue);
		_Members.add(var);
        return var;
	}

    public Variable addMember(
        LanguageSpecificString modifier, TypeDescriptor type, String name) {
        
        return addMember(modifier,type,name,null);
    }
    
	public void addMethod(MethodDefinition methoddef) {
		_Methods.add(methoddef);
	}
	public void addLanguageSpecificString(LanguageSpecificString content) {
		_AdditionalBody.add(content);
	}

    public void writeTo( Formatter f ) throws IOException {

    	if(_PrecedingDeclarations!=null) {
	    	for(int i=0; i<_PrecedingDeclarations.length; i++)
	    		f.write(_PrecedingDeclarations[i]).nl();
    	}
    	if(_PreModifier!=null)
            f.write(_PreModifier);
    	
        f.p("class").p(_ClassName);

    	if(_PostModifier!=null)
            f.write(_PostModifier);
    	
        f.p('{');
        f.in();
        f.nl();
    	
		for(int i=0; i<_Members.size(); i++)
			((Variable)_Members.get(i)).state(f);
            
    	f.nl();
        
		for(int i=0; i<_Methods.size(); i++)
			((MethodDefinition)_Methods.get(i)).writeTo(f);
    	
		for(int i=0; i<_AdditionalBody.size(); i++)
			f.write((LanguageSpecificString)_AdditionalBody.get(i));
    	
    	f.out();
        f.nl().p('}');
    }

}
