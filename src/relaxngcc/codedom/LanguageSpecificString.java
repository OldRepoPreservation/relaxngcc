package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class LanguageSpecificString {
	
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
}
