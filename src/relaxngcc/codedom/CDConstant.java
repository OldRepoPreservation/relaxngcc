package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class CDConstant extends CDExpression {

	private int _Type; //one of the constants in CDType
	private int _IntVal;
	private boolean _BooleanVal;
	private String _StringVal;
	
	public CDConstant(int value) {
		_Type = CDType.TYPE_INTEGER;
		_IntVal = value;
	}
	public CDConstant(boolean value) {
		_Type = CDType.TYPE_BOOLEAN;
		_BooleanVal = value;
	}
	public CDConstant(String value) {
		_Type = CDType.TYPE_STRING;
		_StringVal = value;
	}
    
    
    private static class Atom extends CDExpression {
        private Atom( String _token ) { this.token = _token; }
        private final String token;
        public void express(CDFormatter f) throws IOException {
            f.p(token);
        }
    }
    

	public static final CDExpression NULL = new Atom("null");
    public static final CDExpression THIS = new Atom("this");
    public static final CDExpression SUPER = new Atom("super");
	
    public void express(CDFormatter f) throws IOException {
    	switch(_Type) {
    		case CDType.TYPE_INTEGER:
    		   	f.p(Integer.toString(_IntVal));
    		   	break;
    		case CDType.TYPE_BOOLEAN:
    		   	f.p(_BooleanVal? "true" : "false");
    		   	break;
    		case CDType.TYPE_STRING:
    		   	f.p('"'+_StringVal+'"');
    		   	break;
            default:
                throw new InternalError();
    	}
    }

}
