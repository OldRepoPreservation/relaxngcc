package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Operator factory
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class CDOp {

    public static final int EQ = 1;
    public static final int STREQ = 2;
    public static final int AND = 3;
    public static final int OR = 4;
    
    private static class BinaryOperator extends CDExpression {
        private int _Type;
        private CDExpression _Left;
        private CDExpression _Right;
    
        protected BinaryOperator(int type, CDExpression left, CDExpression right) {
        	_Type = type;
        	_Left = left;
        	_Right = right;
        }
        
        public void express(CDFormatter f) throws IOException {
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
    public static CDExpression EQ(CDExpression left, CDExpression right) {
    	return new BinaryOperator(EQ, left, right);
    }
    /** String value equality operator. */
    public static CDExpression STREQ(CDExpression left, CDExpression right) {
        return new BinaryOperator(STREQ, left, right);
    }
    /** Logical and operator. */
    public static CDExpression AND(CDExpression left, CDExpression right) {
    	return new BinaryOperator(AND, left, right);
    }
    /** Logical or operator. */
    public static CDExpression OR(CDExpression left, CDExpression right) {
    	return new BinaryOperator(OR, left, right);
    }
    /** logical not operator */
    public static CDExpression not(final CDExpression exp) {
        return new CDExpression() {
            public void express(CDFormatter f) throws IOException {
                f.p('(').p('!').express(exp).p(')');
            }
        };
    }


}
