package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class MethodInvokeExpression extends Expression implements Statement {

	private final Expression _Object;
	private final String _MethodName;
    private final ArrayList _Args = new ArrayList();

    // use the invoke method on Expression.
	protected MethodInvokeExpression(Expression obj, String methodname) {
		_Object = obj;
		_MethodName = methodname;
	}
	public MethodInvokeExpression(String methodname) {
		_Object = null;
		_MethodName = methodname;
	}
    
    /** Adds an argument to this invocation. */
    public MethodInvokeExpression arg( Expression arg ) {
        _Args.add(arg);
        return this;
    }
    /** Adds arguments to this invocation. */
    public MethodInvokeExpression args( Expression[] args ) {
        for( int i=0; i<args.length; i++ )
            arg( args[i] );
        return this;
    }
	
    public void state(Formatter f) throws IOException {
        express(f);
        f.eos().nl();
    }

    public void express(Formatter f) throws IOException {
    	
    	if(_Object != null) {
            f.express(_Object).p('.');
    	}
        f.p(_MethodName).p('(');
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            
            if(!first)  f.p(',');
            first = false;
            
            f.express( (Expression) itr.next() );
		}
        
        f.p(')');
    }

}
