package relaxngcc.codedom;

import java.io.IOException;
import java.io.Writer;

/**
 */
public class LanguageSpecificString extends CodeDOMRoot {
	
	String[] _Data;
	
	public LanguageSpecificString() {
		_Data = new String[Language.LANGUAGE_COUNT];
	}
	public LanguageSpecificString(String data) {
		_Data = new String[Language.LANGUAGE_COUNT];
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

    public void writeTo(OutputParameter param, Writer writer) throws IOException {
    	String t = _Data[param.getLanguage()];
    	if(t==null)
    		throw new IllegalArgumentException("unsupported language");
    	else
    		writer.write(t);
    }

}
