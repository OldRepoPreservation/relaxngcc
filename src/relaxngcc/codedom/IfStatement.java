package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class IfStatement implements Statement {
	
	private class Block {
		Expression _Expr;
		StatementVector _Statements;
		
		Block(Expression e, StatementVector s) {
			_Expr = e;
			_Statements = s;
		}
	}
	
	private Vector _Blocks;
	private StatementVector _ElseBlock;
	
	public IfStatement(Expression expr, StatementVector true_case) {
		_Blocks = new Vector();
		if(expr==null) throw new IllegalArgumentException("expr is null");
		_Blocks.add(new Block(expr, true_case));
		_ElseBlock = null;
	}
	public void addClause(Expression expr, StatementVector statements) {
		if(expr==null) throw new IllegalArgumentException("expr is null");
		if(_ElseBlock!=null) throw new IllegalStateException("this IfStatement is closed already");
		_Blocks.add(new Block(expr, statements));
	}
	public void closeClause(StatementVector statements) {
		if(_ElseBlock!=null) throw new IllegalStateException("this IfStatement is closed already");
		_ElseBlock = statements;
	}
	
	
    public void state( Formatter f ) throws IOException {

    	for(int i=0; i<_Blocks.size(); i++) {
            final Block block = (Block)_Blocks.get(i);
            
            if(i!=0)    f.p("else");
            f.p("if").p('(').express(block._Expr).p(')');
            
            // not writing '{' causes ambiguity if the only statement
            // inside it is another IfStatement.
            // consider
            // if(x) if(y) a; else b;
            
            // Is this
            // if(x) { if(y) a; } else b;
            // or
            // if(x) { if(y) a; else b; } ?
            
            f.state(block._Statements);
    	}
    	
    	if(_ElseBlock!=null)
            f.p("else").state(_ElseBlock);
    }

}
