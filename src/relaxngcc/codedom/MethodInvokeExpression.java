package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class MethodInvokeExpression extends Expression {

	private Expression _Object;
	private String _MethodName;
	private Expression[] _Args;

	public MethodInvokeExpression(Expression obj, String methodname, Expression[] args) {
		_Object = obj;
		_MethodName = methodname;
		_Args = args;
	}
	public MethodInvokeExpression(String methodname, Expression[] args) {
		_Object = null;
		_MethodName = methodname;
		_Args = args;
	}
	

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	
    	if(_Object != null) {
	    	_Object.writeTo(param, writer);
    		writer.write(".");
    	}
    	writer.write(_MethodName);
    	writer.write("(");
    	
    	if(_Args!=null) {
    		for(int i=0; i<_Args.length; i++) {
    			if(i > 0) writer.write(", ");
    			_Args[i].writeTo(param, writer);
    		}
    	}
    	writer.write(")");
    }

}
