package relaxngcc.grammar;

import relaxngcc.BuildError;
import relaxngcc.codedom.CDExpression;
import relaxngcc.codedom.CDLanguageSpecificString;
import relaxngcc.codedom.CDType;

import relaxngcc.parser.ParserRuntime;

/**
 * NGCC Parameter for scope definitions.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NGCCDefineParam {
    
    public NGCCDefineParam(ParserRuntime rt, String _className, String _access,
        String _returnType, String _returnValue, String _params ) {
        
        if(_returnType==null && _returnValue!=null)
            rt.addError(new BuildError(BuildError.ERROR, rt.createLocator(), "@return-type is required when @return-value is specified."));
        else if(_returnType!=null && _returnValue==null)            
            rt.addError(new BuildError(BuildError.ERROR, rt.createLocator(), "@return-value is required when @return-type is specified"));
            
        if(_returnType==null)   _returnType=_className;
        if(_returnValue==null)  _returnValue="this";
        if(_access==null)       _access="";
        if(_className==null) {
            Grammar g = rt.grammar;
            int i = 0;
            while(g!=null) {
                i++;
                g = g.parent;
            }
        
            if(i>1)
                _className = "RelaxNGCC_Result" + Integer.toString(i);
            else
                _className = "RelaxNGCC_Result";
        }
        
        this.className = _className;
        this.access = _access;
        this.returnType = new CDType(_returnType);
        this.returnValue = new CDLanguageSpecificString(_returnValue);
        this.params = _params;
    }
    
    /** Class name to generate. */
    public final String className;
    
    /** Access modifiers. */
    public final String access;
    
    /** Return-type from this state. */
    public final CDType returnType;
    
    /** Return-value from this state. */
    public final CDExpression returnValue;
    
    /** Additional parameters to this state */
    public final String params;
}

