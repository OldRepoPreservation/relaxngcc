package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class CDType {
	
	public static final int TYPE_VOID    = 0;
	public static final int TYPE_OBJECT  = 1;
	public static final int TYPE_INTEGER = 2;
	public static final int TYPE_BOOLEAN = 3;
	public static final int TYPE_STRING  = 4;

	public static CDType VOID;
	public static CDType INTEGER;
	public static CDType BOOLEAN;
	public static CDType STRING;
	
	static {
		VOID    = new CDType(TYPE_VOID);
		INTEGER = new CDType(TYPE_INTEGER);
		BOOLEAN = new CDType(TYPE_BOOLEAN);
		STRING  = new CDType(TYPE_STRING);
	}		

	private CDType(int type) {
		_Type = type;
        name = null;
	}


	private final int _Type;
	public final String name;
    
	
	public CDType(String classname) {
		_Type = TYPE_OBJECT;
		name = classname;
	}
    
    /** Creates a new instance of this type. */
    public CDObjectCreateExpression _new() {
        return new CDObjectCreateExpression(this);
    }

	public void writeTo( CDFormatter f ) throws IOException {
		switch(_Type) {
			case TYPE_OBJECT:
				f.p(name);
				break;
			case TYPE_VOID:
				f.p("void");
				break;
			case TYPE_INTEGER:
				f.p("int");
				break;
			case TYPE_BOOLEAN:
				f.p("boolean");
				break;
			case TYPE_STRING:
				f.p("String");
				break;
            default:
                throw new InternalError();
		}
	}		
}
