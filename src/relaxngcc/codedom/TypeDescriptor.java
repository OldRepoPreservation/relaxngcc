package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class TypeDescriptor extends CodeDOMRoot {
	
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
	}


	private int _Type;
	private String _Name;
	
	public TypeDescriptor(String classname) {
		_Type = TYPE_OBJECT;
		_Name = classname;
	}

	public void writeTo(OutputParameter param, Writer writer) throws IOException {
		switch(_Type) {
			case TYPE_OBJECT:
				writer.write(_Name);
				break;
			case TYPE_VOID:
				writer.write("void");
				break;
			case TYPE_INTEGER:
				writer.write("int");
				break;
			case TYPE_BOOLEAN:
				writer.write("boolean");
				break;
			case TYPE_STRING:
				writer.write("String");
				break;
		}
	}		
}
