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

	public static final ConstantExpression NULL;
	
	private ConstantExpression() {}
	static {
		NULL = new ConstantExpression();
		NULL._Type = TypeDescriptor.TYPE_OBJECT;
	}

    public void express(Formatter f) throws IOException {
    	switch(_Type) {
    		case TypeDescriptor.TYPE_OBJECT:
    		   	f.p("null");
    		   	break;
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
