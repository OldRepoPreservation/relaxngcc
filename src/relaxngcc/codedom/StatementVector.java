package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 * @author Daisuke OKAJIMA
 * 
 * simple collection of Statement objects
 */
public class StatementVector implements Statement {

	private final Vector _Statements = new Vector();
	
	public StatementVector() {}
    public StatementVector(Statement s) {
        this();
        add(s);
    }
	
	public void add(Statement s) {
		if(s==null) throw new IllegalArgumentException("parameter is null");
		_Statements.add(s);
	}
	public void add(StatementVector sv) {
		_Statements.addAll(sv._Statements);
	}
    
    /** Adds a new method invocation. */
    public MethodInvokeExpression invoke( Expression obj, String method ) {
        MethodInvokeExpression e = new MethodInvokeExpression(obj,method);
        add(e);
        return e;
    }
    public MethodInvokeExpression invoke( String method ) {
        MethodInvokeExpression e = new MethodInvokeExpression(method);
        add(e);
        return e;
    }
    /** Adds a new variable declaration. */
    public Variable decl(TypeDescriptor type, String name) {
        Variable d = new Variable(null,type,name,null);
        add(d);
        return d;
    }
    public Variable decl(TypeDescriptor type, String name, Expression init ) {
        Variable d = new Variable(null,type,name,init);
        add(d);
        return d;
    }
    /** Adds a new assignment. */
    public AssignStatement assign( Expression lhs, Expression rhs ) {
        AssignStatement a = new AssignStatement(lhs,rhs);
        add(a);
        return a;
    }
    /** Adds a new return statement. */
    public void _return( Expression val ) {
        add(new ReturnStatement(val));
    }
    

	public int size() { return _Statements.size(); }
	
	public void state(Formatter f) throws IOException {
        f.p('{').nl().in();
        
		for(int i=0; i<_Statements.size(); i++)
            f.state( (Statement)_Statements.get(i) );
        
        f.out().p('}').nl();
	}
}
