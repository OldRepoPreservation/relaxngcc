package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class CDAssignStatement implements CDStatement {

	private CDExpression _Destination;
	private CDExpression _Source;

    // use CDBlock to create one
	CDAssignStatement(CDExpression destination, CDExpression source) {
		_Destination = destination;
		_Source = source;
	}
	

    public void state(CDFormatter f) throws IOException {
        f.express(_Destination).p('=').express(_Source).eos().nl();
    }
}
