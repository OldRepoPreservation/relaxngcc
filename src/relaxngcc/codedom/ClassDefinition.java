package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class ClassDefinition {

	private LanguageSpecificString[] _PrecedingDeclarations;
	private LanguageSpecificString _ForwardSpecifier;
	private String _ClassName;
	private LanguageSpecificString _BackwardSpecifier;
	private final Vector _Members = new Vector();
	private final Vector _Methods = new Vector();
	private Vector _LanguageSpecificStrings;

	public ClassDefinition(LanguageSpecificString[] declarations, LanguageSpecificString fs, String name, LanguageSpecificString bs) {
		_PrecedingDeclarations = declarations;
		_ForwardSpecifier = fs;
		_ClassName = name;
		_BackwardSpecifier = bs;
		_LanguageSpecificStrings = new Vector();
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
		_LanguageSpecificStrings.add(content);
	}

    public void writeTo( Formatter f ) throws IOException {

    	if(_PrecedingDeclarations!=null) {
	    	for(int i=0; i<_PrecedingDeclarations.length; i++)
	    		f.write(_PrecedingDeclarations[i]).nl();
    	}
    	if(_ForwardSpecifier!=null)
            f.write(_ForwardSpecifier);
    	
        f.p("class").p(_ClassName);

    	if(_BackwardSpecifier!=null)
            f.write(_BackwardSpecifier);
    	
        f.p('{');
        f.in();
        f.nl();
    	
		for(int i=0; i<_Members.size(); i++)
			((Variable)_Members.get(i)).state(f);
            
    	f.nl();
        
		for(int i=0; i<_Methods.size(); i++)
			((MethodDefinition)_Methods.get(i)).writeTo(f);
    	
    	if(_LanguageSpecificStrings!=null) {
    		for(int i=0; i<_LanguageSpecificStrings.size(); i++)
    			f.write((LanguageSpecificString)_LanguageSpecificStrings.get(i));
    	}
    	
    	f.out();
        f.nl().p('}');
    }

}
