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
	
    public void state(OutputParameter param, Writer writer) throws IOException {
        express(param,writer);
        writer.write(';');
    }

    public void express(OutputParameter param, Writer writer) throws IOException {
    	
    	if(_Object != null) {
	    	_Object.express(param, writer);
    		writer.write(".");
    	}
    	writer.write(_MethodName);
    	writer.write("(");
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            
            if(!first)  writer.write(",");
            first = false;
            
            Expression arg = (Expression) itr.next();
			arg.express(param, writer);
		}
        
    	writer.write(")");
    }

}
