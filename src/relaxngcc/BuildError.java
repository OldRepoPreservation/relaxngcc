package relaxngcc;

import org.xml.sax.Locator;

/**
 * @author okajima
 *
 * An error or warning detected in the parsing of grammar
 */
public class BuildError {
    public static final int ERROR = 1;
    public static final int WARNING = 2;
    
    
    private int _type;
    private Locator _locator;
    private String _message;
    
    public BuildError(int type, Locator loc, String msg) {
        _type = type;
        _locator = loc;
        _message = msg;
    }
    public int getType() {
        return _type;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if(_type==ERROR)
            buf.append("[Error]   ");
        else
            buf.append("[Warning] ");
        
        buf.append("line ");
        buf.append(Integer.toString(_locator.getLineNumber()));
        buf.append(" in ");
        buf.append(_locator.getSystemId());
        buf.append(" : ");
        buf.append(_message);
        return buf.toString();
    }
  
    
    
}
