package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 * Unstructured text whose composition we don't care.
 * 
 * This can be used either as an expression or a statement.
 */
public class LanguageSpecificString extends Expression implements Statement {
	
	private final String[] _Data = new String[Language.LANGUAGE_COUNT];
	
	public LanguageSpecificString() {}
	public LanguageSpecificString(String data) {
		setString(data);
	}
	
	public void setString(int language, String data) {
		_Data[language] = data;
	}
    
	public void setString(String data) {
		for(int i=0; i<_Data.length; i++) {
			_Data[i] = data;
		}
	}
    
    public String getString(int language) {
        return _Data[language];
    }
    
    public void state(Formatter f) throws IOException {
        f.write(this);
    }
    
    public void express(Formatter f) throws IOException {
        f.write(this);
    }
}
