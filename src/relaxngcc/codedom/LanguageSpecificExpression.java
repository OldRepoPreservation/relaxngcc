package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class LanguageSpecificExpression extends Expression {

	private LanguageSpecificString _Content;
	
	public LanguageSpecificExpression(LanguageSpecificString content) {
		_Content = content;
	}
	public LanguageSpecificExpression(String content) {
		_Content = new LanguageSpecificString(content);
	}

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	_Content.writeTo(param, writer);
    }

}
