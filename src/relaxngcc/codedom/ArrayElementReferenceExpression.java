package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * x[y]
 */
class ArrayElementReferenceExpression extends Expression {

	private Expression _Array;
	private Expression _Index;
	
    // use Expression.arrayRef()
	ArrayElementReferenceExpression(Expression array, Expression index) {
		_Array = array;
		_Index = index;
	}

    public void express( Formatter f) throws IOException {
    	_Array.express(f);
        f.p('[').express(_Index).p(']');
    }

}
