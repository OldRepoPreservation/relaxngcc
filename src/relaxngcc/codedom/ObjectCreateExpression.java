package relaxngcc.codedom;
import java.io.IOException;
import java.io.Writer;

/**
 */
public class ObjectCreateExpression extends Expression {
    
    private TypeDescriptor _ClassName;
    private Expression[] _ConstructorArgs;
    
    public ObjectCreateExpression(TypeDescriptor classname, Expression[] constructorArgs) {
    	_ClassName = classname;
    	_ConstructorArgs = constructorArgs;
    }

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	writer.write("new ");
    	_ClassName.writeTo(param, writer);
    	writer.write("(");
    	
    	if(_ConstructorArgs!=null) {
    		for(int i=0; i<_ConstructorArgs.length; i++) {
    			if(i > 0) writer.write(", ");
    			_ConstructorArgs[i].writeTo(param, writer);
    		}
    	}
    	writer.write(")");
    	
    }
    
}
