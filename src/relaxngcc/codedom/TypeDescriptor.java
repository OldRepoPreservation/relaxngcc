package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class TypeDescriptor {
	
	public static final int TYPE_VOID    = 0;
	public static final int TYPE_OBJECT  = 1;
	public static final int TYPE_INTEGER = 2;
	public static final int TYPE_BOOLEAN = 3;
	public static final int TYPE_STRING  = 4;

	public static TypeDescriptor VOID;
	public static TypeDescriptor INTEGER;
	public static TypeDescriptor BOOLEAN;
	public static TypeDescriptor STRING;
	
	static {
		VOID    = new TypeDescriptor(TYPE_VOID);
		INTEGER = new TypeDescriptor(TYPE_INTEGER);
		BOOLEAN = new TypeDescriptor(TYPE_BOOLEAN);
		STRING  = new TypeDescriptor(TYPE_STRING);
	}		

	private TypeDescriptor(int type) {
		_Type = type;
        name = null;
	}


	private final int _Type;
	public final String name;
    
	
	public TypeDescriptor(String classname) {
		_Type = TYPE_OBJECT;
		name = classname;
	}
    
    /** Creates a new instance of this type. */
    public ObjectCreateExpression _new() {
        return new ObjectCreateExpression(this);
    }

	public void writeTo( Formatter f ) throws IOException {
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
