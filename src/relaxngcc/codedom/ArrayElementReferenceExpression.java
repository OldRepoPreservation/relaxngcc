package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class ArrayElementReferenceExpression extends Expression {

	private Expression _Array;
	private Expression _Index;
	
	public ArrayElementReferenceExpression(Expression array, Expression index) {
		_Array = array;
		_Index = index;
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	_Array.writeTo(param, writer);
    	writer.write("[");
    	_Index.writeTo(param, writer);
    	writer.write("]");
    }

}
