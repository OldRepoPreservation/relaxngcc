package relaxngcc.grammar;

/**
 * NGCC Parameter for scope definitions.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCDefineParam {
    
    public NGCCDefineParam( String _className, String _access,
        String _returnType, String _returnValue, String _params ) {
        
        if(_returnType==null)   _returnType=_className;
        if(_returnValue==null)  _returnValue="this";
        if(_access==null)       _access="";
        if(_className==null)    _className = "RelaxNGCC_Result";
        
        this.className = _className;
        this.access = _access;
        this.returnType = _returnType;
        this.returnValue = _returnValue;
        this.params = _params;
    }
    
    /** Class name to generate. */
    public final String className;
    
    /** Access modifiers. */
    public final String access;
    
    /** Return-type from this state. */
    public final String returnType;
    
    /** Return-value from this state. */
    public final String returnValue;
    
    /** Additional parameters to this state */
    public final String params;
}

