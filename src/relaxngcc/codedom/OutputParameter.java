package relaxngcc.codedom;

/**
 * @author Daisuke OKAJIMA
 *
 */
public class OutputParameter {
    
    private int _Language;
    private int _Indent;
    
    public OutputParameter(int language) {
    	_Language = language;
    	_Indent = 0;
    }
    
    public int getIndent() { return _Indent; }
    public void incrementIndent() { _Indent++; }
    public void decrementIndent() { _Indent--; }

	public int getLanguage() { return _Language; }
}
