package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class AssignStatement implements Statement {

	private Expression _Destination;
	private Expression _Source;

    // use StatementVector to create one
	AssignStatement(Expression destination, Expression source) {
		_Destination = destination;
		_Source = source;
	}
	

    public void state(Formatter f) throws IOException {
        f.express(_Destination).p('=').express(_Source).eos().nl();
    }
}
