package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class CDMethodInvokeExpression extends CDExpression {

	private final CDExpression _Object;
	private final String _MethodName;
    private final ArrayList _Args = new ArrayList();

    // use the invoke method on CDExpression.
	protected CDMethodInvokeExpression(CDExpression obj, String methodname) {
		_Object = obj;
		_MethodName = methodname;
	}
	public CDMethodInvokeExpression(String methodname) {
		_Object = null;
		_MethodName = methodname;
	}
    
    /** Adds an argument to this invocation. */
    public CDMethodInvokeExpression arg( CDExpression arg ) {
        _Args.add(arg);
        return this;
    }
    /** Adds arguments to this invocation. */
    public CDMethodInvokeExpression args( CDExpression[] args ) {
        for( int i=0; i<args.length; i++ )
            arg( args[i] );
        return this;
    }
    
    public CDStatement asStatement() {
        return new CDExpressionStatement(this);
    }
	
    public void express(CDFormatter f) throws IOException {
    	
    	if(_Object != null) {
            f.express(_Object).p('.');
    	}
        f.p(_MethodName).p('(');
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            
            if(!first)  f.p(',');
            first = false;
            
            f.express( (CDExpression) itr.next() );
		}
        
        f.p(')');
    }

}
