package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link Formatter} implementation for Java
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class JavaFormatter extends Formatter {

    public JavaFormatter(Writer _writer) {
        super(_writer);
    }

    public Formatter write( LanguageSpecificString str ) throws IOException {
        return p(str.getString(Language.JAVA).trim());
    }
}
