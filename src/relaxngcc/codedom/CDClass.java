package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 */
public class CDClass extends CDType {

	private CDLanguageSpecificString[] _PrecedingDeclarations;
	private CDLanguageSpecificString _PreModifier;
    private CDLanguageSpecificString _PostModifier;
	private String _ClassName;
    
	private final ArrayList _Members = new ArrayList();
	private final ArrayList _Methods = new ArrayList();
	
    private final ArrayList _AdditionalBody = new ArrayList();
    
    private final ArrayList _innerClasses = new ArrayList();

    public CDClass( String className ) {
        this(null,null,className,null);
    }

	public CDClass(
        CDLanguageSpecificString[] declarations, CDLanguageSpecificString fs,
        String name, CDLanguageSpecificString bs) {
		
        super(name);
        _PrecedingDeclarations = declarations;
		_PreModifier = fs;
		_ClassName = name;
		_PostModifier = bs;
	}
    

    /** Adds a new member declaration. */
	public CDVariable addMember(
        CDLanguageSpecificString modifier,
        CDType type, String name, CDExpression initialValue) {
            
        CDVariable var = new CDVariable(modifier,type,name,initialValue);
		_Members.add(var);
        return var;
	}

    public CDVariable addMember(
        CDLanguageSpecificString modifier, CDType type, String name) {
        
        return addMember(modifier,type,name,null);
    }
    
	public void addMethod(CDMethod methoddef) {
		_Methods.add(methoddef);
	}
	public void addLanguageSpecificString(CDLanguageSpecificString content) {
		_AdditionalBody.add(content);
	}
    
    /**
     * Adds a new inner class.
     */
    public void addInnerClass( CDClass innerClass ) {
        _innerClasses.add(innerClass);
    }

    public void writeType( CDFormatter f ) throws IOException {
        f.p(_ClassName);
    }

    public void writeTo( CDFormatter f ) throws IOException {

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
			((CDVariable)_Members.get(i)).state(f);
            
    	f.nl();
        
		for(int i=0; i<_Methods.size(); i++)
			((CDMethod)_Methods.get(i)).writeTo(f);
    	
		for(int i=0; i<_AdditionalBody.size(); i++)
			f.write((CDLanguageSpecificString)_AdditionalBody.get(i));
    	
        for(int i=0; i<_innerClasses.size(); i++) {
            f.nl();
            ((CDClass)_innerClasses.get(i)).writeTo(f);
            f.nl();
        }
        
    	f.out();
        f.nl().p('}').nl().nl();
    }

}
