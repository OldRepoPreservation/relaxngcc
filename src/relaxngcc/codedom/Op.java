package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Operator factory
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class Op {

    public static final int EQ = 1;
    public static final int STREQ = 2;
    public static final int AND = 3;
    public static final int OR = 4;
    
    private static class BinaryOperator extends Expression {
        private int _Type;
        private Expression _Left;
        private Expression _Right;
    
        protected BinaryOperator(int type, Expression left, Expression right) {
        	_Type = type;
        	_Left = left;
        	_Right = right;
        }
        
        public void express(Formatter f) throws IOException {
            if(_Type==STREQ) {
                f.express(_Left).p('.').p("equals").p('(').express(_Right).p(')');
            } else {
                //TODO: eliminate excessive brackets
                f.p('(');
                _Left.express(f);
                switch(_Type) {
                    case AND:
                        f.p("&&");
                        break;
                    case OR:
                        f.p("||");
                        break;
                    case EQ:
                        f.p("==");
                        break;
                }
                _Right.express(f);
                f.p(')');
            }
        }
    }
    
    /** Object identity equality operator. */
    public static Expression EQ(Expression left, Expression right) {
    	return new BinaryOperator(EQ, left, right);
    }
    /** String value equality operator. */
    public static Expression STREQ(Expression left, Expression right) {
        return new BinaryOperator(STREQ, left, right);
    }
    /** Logical and operator. */
    public static Expression AND(Expression left, Expression right) {
    	return new BinaryOperator(AND, left, right);
    }
    /** Logical or operator. */
    public static Expression OR(Expression left, Expression right) {
    	return new BinaryOperator(OR, left, right);
    }
    /** logical not operator */
    public static Expression not(final Expression exp) {
        return new Expression() {
            public void express(Formatter f) throws IOException {
                f.p('(').p('!').express(exp).p(')');
            }
        };
    }


}
