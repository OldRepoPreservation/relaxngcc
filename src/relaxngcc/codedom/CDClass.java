package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class CDClass {

	private CDLanguageSpecificString[] _PrecedingDeclarations;
	private CDLanguageSpecificString _PreModifier;
    private CDLanguageSpecificString _PostModifier;
	private String _ClassName;
    
	private final Vector _Members = new Vector();
	private final Vector _Methods = new Vector();
	
    private final Vector _AdditionalBody = new Vector();

	public CDClass(CDLanguageSpecificString[] declarations, CDLanguageSpecificString fs, String name, CDLanguageSpecificString bs) {
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
    	
    	f.out();
        f.nl().p('}');
    }

}
