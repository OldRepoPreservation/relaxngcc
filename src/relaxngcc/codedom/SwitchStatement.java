package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class SwitchStatement implements Statement {

	private class Block {
		ConstantExpression _Expr;
		StatementVector _Statements;
		
		Block(ConstantExpression e, StatementVector s) {
			_Expr = e;
			_Statements = s;
		}
	}
	
	private Expression _CheckValue;
	private Vector _Blocks;
	private StatementVector _DefaultBlock;
	
	public SwitchStatement(Expression expr) {
		_CheckValue = expr;
		_Blocks = new Vector();
		_DefaultBlock = null;
	}
	public void addCase(ConstantExpression expr, StatementVector statements) {
		if(_DefaultBlock!=null) throw new IllegalStateException("this SwitchStatement is closed already");
		_Blocks.add(new Block(expr, statements));
	}
	public void setDefaultCase(StatementVector statements) {
		if(_DefaultBlock!=null) throw new IllegalStateException("this SwitchStatement is closed already");
		_DefaultBlock = statements;
	}

    public void state(Formatter f) throws IOException {

        f.p("switch").p('(').express(_CheckValue).p(')').p('{').nl();
        
    	for(int i=0; i<_Blocks.size(); i++) {
	    	final Block block = (Block)_Blocks.get(i);
            
            f.p("case").express(block._Expr).p(':').nl();
	    	
            f.in();
            f.state(block._Statements);
            f.p("break").eos().nl();
            f.out();
    	}
    	
    	if(_DefaultBlock!=null) {
            f.p("default:").nl();
	    	
            f.in();
            f.state(_DefaultBlock);
            f.p("break").eos().nl();
            f.out();
    	}
        
        f.p('}').nl();
    }

}
