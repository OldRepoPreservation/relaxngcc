package relaxngcc.codedom;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 */
public class CDObjectCreateExpression extends CDExpression implements CDStatement {
    
    private final CDType _ClassName;
    private final ArrayList _Args = new ArrayList();
    
    /** use CDType._new */
    CDObjectCreateExpression(CDType classname ) {
    	_ClassName = classname;
    }
    
    public CDObjectCreateExpression arg( CDExpression arg ) {
        _Args.add(arg);
        return this;
    }

    public void express(CDFormatter f) throws IOException {
        f.p("new").type(_ClassName).p('(');
    	
        boolean first = true;
        for (Iterator itr = _Args.iterator(); itr.hasNext();) {
            if(!first)  f.p(',');
            first = false;
            
            CDExpression arg = (CDExpression) itr.next();
            f.express(arg);
        }
        
    	f.p(')');
    }

    public void state(CDFormatter f) throws IOException {
        express(f);
        f.eos().nl();
    }
    
}
