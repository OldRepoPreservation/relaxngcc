package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class SwitchStatement extends CodeDOMRoot implements Statement {

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

    public void state(OutputParameter param, Writer writer) throws IOException {

        writeIndent(param, writer);
        writer.write("switch(");
        _CheckValue.express(param, writer);
        writer.write(") {");
        writer.write(NEWLINE);
        param.incrementIndent();
        
    	for(int i=0; i<_Blocks.size(); i++) {
	    	Block block = (Block)_Blocks.get(i);
	    	writeIndent(param, writer);
	    	writer.write("case ");
	    	block._Expr.express(param, writer);
	    	writer.write(":");
	    	writer.write(NEWLINE);
	    	
	    	param.incrementIndent();
	    	block._Statements.writeTo(param, writer);
	    	writeIndent(param, writer);
	    	writer.write("break;");
	    	writer.write(NEWLINE);
	    	param.decrementIndent();

    	}
    	
    	if(_DefaultBlock!=null) {
	    	writeIndent(param, writer);
	    	writer.write("default:");
	    	writer.write(NEWLINE);
	    	
	    	param.incrementIndent();
    		_DefaultBlock.writeTo(param, writer);
	    	writeIndent(param, writer);
	    	writer.write("break;");
	    	writer.write(NEWLINE);
	    	param.decrementIndent();
    	}

        param.decrementIndent();
        writeIndent(param, writer);
        writer.write("}");
    	writer.write(NEWLINE);
    }

}
