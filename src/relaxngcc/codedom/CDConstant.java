package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class CDConstant extends CDExpression {

    /** Type of this constant. */
	private final CDType _type;
	private int _IntVal;
	private boolean _BooleanVal;
	private String _StringVal;
	
	public CDConstant(int value) {
		_type = CDType.INTEGER;
		_IntVal = value;
	}
	public CDConstant(boolean value) {
		_type = CDType.BOOLEAN;
		_BooleanVal = value;
	}
	public CDConstant(String value) {
		_type = CDType.STRING;
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
        if(_type==CDType.INTEGER) {
		   	f.p(Integer.toString(_IntVal));
		   	return;
        }
        if(_type==CDType.BOOLEAN) {
		   	f.p(_BooleanVal? "true" : "false");
            return;
        }
        if(_type==CDType.STRING) {
		   	f.p('"'+_StringVal+'"');
            return;
        }
        throw new InternalError();
    }

}
