package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class CDCastExpression extends CDExpression {

	private CDType _Type;
	private CDExpression _Expression;

	public CDCastExpression(CDType type, CDExpression expr) {
		_Type = type;
		_Expression = expr;
	}

    public void express( CDFormatter f ) throws IOException {
        f.p("((").type(_Type).p(")").express(_Expression).p(")");
    }

}
