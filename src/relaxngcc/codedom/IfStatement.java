package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class IfStatement extends Statement {
	
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
	
	
    public void writeTo(OutputParameter param, Writer writer) throws IOException {

    	for(int i=0; i<_Blocks.size(); i++) {
	    	writeIndent(param, writer);
	    	writer.write(i==0? "if(" : "else if(");
	    	Block block = (Block)_Blocks.get(i);
	    	block._Expr.writeTo(param, writer);
            // not writing '{' causes ambiguity if the only statement
            // inside it is another IfStatement.
            // consider
            // if(x) if(y) a; else b;
            
            // Is this
            // if(x) { if(y) a; } else b;
            // or
            // if(x) { if(y) a; else b; } ?
            //
	    	writer.write(") {");
	    	writer.write(NEWLINE);
	    	
	    	param.incrementIndent();
	    	block._Statements.writeTo(param, writer);
	    	param.decrementIndent();

	    	writeIndent(param, writer);
    		writer.write("} ");
            
	    	writer.write(NEWLINE);
    	}
    	
    	if(_ElseBlock!=null) {
	    	writeIndent(param, writer);
	    	writer.write("else");

	    	if(_ElseBlock.size()>1) writer.write(" {");
	    	writer.write(NEWLINE);
	    	
	    	param.incrementIndent();
    		_ElseBlock.writeTo(param, writer);
	    	param.decrementIndent();

	    	if(_ElseBlock.size()>1) {
   		    	writeIndent(param, writer);
				writer.write("} ");
	    	}
    	}

    	writer.write(NEWLINE);
    }

}
