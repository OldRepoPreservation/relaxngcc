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
    
    /** use TypeDescriptor._new */
    ObjectCreateExpression(TypeDescriptor classname ) {
    	_ClassName = classname;
    }
    
    public ObjectCreateExpression arg( Expression arg ) {
        _Args.add(arg);
        return this;
    }

    public void express(Formatter f) throws IOException {
        f.p("new").type(_ClassName).p('(');
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            if(!first)  f.p(',');
            first = false;
            
            Expression arg = (Expression) itr.next();
            f.express(arg);
        }
        
    	f.p(')');
    }

    public void state(Formatter f) throws IOException {
        express(f);
        f.eos().nl();
    }
    
}
