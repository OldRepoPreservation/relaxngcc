package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 * @author Daisuke OKAJIMA
 * 
 * simple collection of Statement objects
 */
public class StatementVector extends CodeDOMRoot {

	private Vector _Statements;
	
	public StatementVector() {
		_Statements = new Vector();
	}
	public StatementVector(Statement s) {
		_Statements = new Vector();
		_Statements.add(s);
	}
	
	public void addStatement(Statement s) {
		if(s==null) throw new IllegalArgumentException("parameter is null");
		_Statements.add(s);
	}
	public void addStatement(StatementVector sv) {
		_Statements.addAll(sv._Statements);
	}

	public int size() { return _Statements.size(); }
	
	public void writeTo(OutputParameter param, Writer writeTo) throws IOException {
		for(int i=0; i<_Statements.size(); i++) {
			((Statement)_Statements.get(i)).writeTo(param, writeTo);
		}
	}
}
