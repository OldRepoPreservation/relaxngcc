package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public abstract class CodeDOMRoot {

    protected static final String NEWLINE = System.getProperty("line.separator");
    
    protected static void writeIndent(OutputParameter param, Writer writer) throws IOException {
    	for(int i=0; i<param.getIndent(); i++) {
    		writer.write("  ");
    	}
    }

//	public abstract void writeTo(OutputParameter param, Writer writer) throws IOException;

}
