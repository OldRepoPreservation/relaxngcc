package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class BinaryOperatorExpression extends Expression {

    public static final int EQ = 1;
    public static final int AND = 2;
    public static final int OR = 3;

    private int _Type;
    private Expression _Left;
    private Expression _Right;

    protected BinaryOperatorExpression(int type, Expression left, Expression right) {
    	_Type = type;
    	_Left = left;
    	_Right = right;
    }
    
    public static BinaryOperatorExpression EQ(Expression left, Expression right) {
    	return new BinaryOperatorExpression(EQ, left, right);
    }
    public static BinaryOperatorExpression AND(Expression left, Expression right) {
    	return new BinaryOperatorExpression(AND, left, right);
    }
    public static BinaryOperatorExpression OR(Expression left, Expression right) {
    	return new BinaryOperatorExpression(OR, left, right);
    }

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	//TODO: eliminate excessive brackets
        writer.write("(");
        _Left.writeTo(param, writer);
        switch(_Type) {
        	case AND:
        	    writer.write(" && ");
        	    break;
        	case OR:
        	    writer.write(" || ");
        	    break;
        	case EQ:
        	    writer.write(" == "); //TODO: string support
        	    break;
        }
        _Right.writeTo(param, writer);
        writer.write(")");
    }

}
