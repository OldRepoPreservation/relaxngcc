package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class ConstantExpression extends Expression {

	private int _Type; //one of the constants in TypeDescriptor
	private int _IntVal;
	private boolean _BooleanVal;
	private String _StringVal;
	
	public ConstantExpression(int value) {
		_Type = TypeDescriptor.TYPE_INTEGER;
		_IntVal = value;
	}
	public ConstantExpression(boolean value) {
		_Type = TypeDescriptor.TYPE_BOOLEAN;
		_BooleanVal = value;
	}
	public ConstantExpression(String value) {
		_Type = TypeDescriptor.TYPE_STRING;
		_StringVal = value;
	}
    
    
    private static class Atom extends Expression {
        private Atom( String _token ) { this.token = _token; }
        private final String token;
        public void express(Formatter f) throws IOException {
            f.p(token);
        }
    }
    

	public static final Expression NULL = new Atom("null");
    public static final Expression THIS = new Atom("this");
    public static final Expression SUPER = new Atom("super");
	
    public void express(Formatter f) throws IOException {
    	switch(_Type) {
    		case TypeDescriptor.TYPE_INTEGER:
    		   	f.p(Integer.toString(_IntVal));
    		   	break;
    		case TypeDescriptor.TYPE_BOOLEAN:
    		   	f.p(_BooleanVal? "true" : "false");
    		   	break;
    		case TypeDescriptor.TYPE_STRING:
    		   	f.p('"'+_StringVal+'"');
    		   	break;
            default:
                throw new InternalError();
    	}
    }

}
