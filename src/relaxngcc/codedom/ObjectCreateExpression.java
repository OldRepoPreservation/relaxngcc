package relaxngcc.codedom;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class ObjectCreateExpression extends Expression implements Statement {
    
    private final TypeDescriptor _ClassName;
    private final ArrayList _Args = new ArrayList();
    
    public ObjectCreateExpression(TypeDescriptor classname ) {
    	_ClassName = classname;
    }
    
    public ObjectCreateExpression arg( Expression arg ) {
        _Args.add(arg);
        return this;
    }

    public void express(OutputParameter param, Writer writer) throws IOException {
    	writer.write("new ");
    	_ClassName.writeTo(param, writer);
    	writer.write("(");
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            if(!first)  writer.write(",");
            first = false;
            
            Expression arg = (Expression) itr.next();
            arg.express(param,writer);
        }
        
    	writer.write(")");
    }

    public void state(OutputParameter param, Writer writer) throws IOException {
        express(param,writer);
        writer.write(';');
    }
    
}
