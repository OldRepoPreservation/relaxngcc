package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Abstract statement of programming languages.
 * 
 * @author Daisuke OKAJIMA
 */
public interface Statement {
    /**
     * Prints itself as a statement.
     */
    void state( OutputParameter param, Writer writer ) throws IOException;
}
