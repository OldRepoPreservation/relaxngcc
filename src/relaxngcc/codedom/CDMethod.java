package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class CDMethod {

	private CDLanguageSpecificString _PreModifier;
	private CDType _ReturnType;
	private String _Name;
	private CDLanguageSpecificString _PostModifier;
    /** Parameters to this method. List of VariableDeclaration. */
    private final ArrayList _Params = new ArrayList();
	
	private final CDBlock _Body = new CDBlock();
	
	public CDMethod(CDLanguageSpecificString forwardspecifier,
        CDType returntype, String name,
        CDLanguageSpecificString backwardspecifier ) {
		
        _PreModifier = forwardspecifier;
		_ReturnType = returntype;
		_Name = name;
		_PostModifier = backwardspecifier;
	}
    
    /**
     * Adds a new parameter to this method and returns a reference
     * to it.
     */
    public CDVariable param( CDType type, String name ) {
        CDVariable v = new CDVariable(null,type,name,null);
        _Params.add(v);
        return v;
    }
    
    /** Gets a reference to the method body. */
    public CDBlock body() { return _Body; }

    public void writeTo( CDFormatter f ) throws IOException {

    	if(_PreModifier!=null)
            f.write(_PreModifier);
    	
    	if(_ReturnType!=null)
            f.type(_ReturnType);
        
        f.p(_Name).p('(');
        
        boolean first=true;
        for (Iterator itr = _Params.iterator(); itr.hasNext();) {
            if(!first)  f.p(',');
            first = false;
            
            f.declare((CDVariable) itr.next());
        }
        f.p(')');
		
    	if(_PostModifier!=null)
            f.write(_PostModifier);
    	
        f.state(_Body).nl();
    	
    }

}
