package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

/**
 */
public class CDSwitchStatement implements CDStatement {

	private class Block {
		CDConstant _Expr;
		CDBlock _Statements;
		
		Block(CDConstant e, CDBlock s) {
			_Expr = e;
			_Statements = s;
		}
	}
	
	private CDExpression _CheckValue;
	private Vector _Blocks;
	private CDBlock _DefaultBlock;
	
	public CDSwitchStatement(CDExpression expr) {
		_CheckValue = expr;
		_Blocks = new Vector();
		_DefaultBlock = null;
	}
	public void addCase(CDConstant expr, CDBlock statements) {
		if(_DefaultBlock!=null) throw new IllegalStateException("this SwitchStatement is closed already");
		_Blocks.add(new Block(expr, statements));
	}
//	public void setDefaultCase(CDBlock statements) {
//		if(_DefaultBlock!=null) throw new IllegalStateException("this SwitchStatement is closed already");
//		_DefaultBlock = statements;
//	}
    public CDBlock defaultCase() {
        if(_DefaultBlock==null) _DefaultBlock = new CDBlock();
        return _DefaultBlock;
    }

    public void state(CDFormatter f) throws IOException {

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
