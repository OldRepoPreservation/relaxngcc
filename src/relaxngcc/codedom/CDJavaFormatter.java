package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link CDFormatter} implementation for Java
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class CDJavaFormatter extends CDFormatter {

    public CDJavaFormatter(Writer _writer) {
        super(_writer);
    }

    public CDFormatter write( CDLanguageSpecificString str ) throws IOException {
        return p(str.getString(CDLanguage.JAVA).trim());
    }
}
