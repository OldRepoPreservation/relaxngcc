package relaxngcc.datatype;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;

/**
 * A string with some free variables
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class Macro {
    /**
     * List of {@link String}s and {@link Token}s.
     */
    private final List tokens = new ArrayList();
    
    /**
     * Variable.
     */
    public static final class Variable
    {
        private String name;
        private Locator locator;
        
        /**
         * @param _name
         *      variable name.
         * @param loc
         *      Location of the occurence of this variable.
         *      used for error messages.
         */
        public Variable(String _name,Locator loc) {
            this.name = _name;
            this.locator = loc==null ? null : new LocatorImpl(loc);
        }
        /**
         * Replaces this token with its definition in the dictionary file.
         */
        public String toString(Map dictionary) throws NoDefinitionException {
            String value = (String)dictionary.get(name);
            if( value==null )
                throw new NoDefinitionException(name,locator);
            return value;
        }
    }
    
    /**
     * Adds a new literal string to the token list.
     */
    public void add( String str ) {
        tokens.add(str);
    }
    
    /**
     * Adds a new macro to the token list.
     */
    public void add( Variable tkn ) {
        tokens.add(tkn);
    }
    
    /**
     * Expands all the variables with a given dictionary
     * 
     * @param
     *      String to string dictionary.
     * 
     * @exception NoDefinitionException
     *      thrown when there's a variable whose definition is not given
     *      by the specified dictionary.
     */
    public String toString( Map dictionary ) throws NoDefinitionException {
        StringBuffer buf = new StringBuffer();
        
        for (Iterator itr = tokens.iterator(); itr.hasNext();) {
            Object v = itr.next();
            if(v instanceof String)     buf.append((String)v);
            else
                buf.append( ((Variable)v).toString(dictionary) );
        }
        
        return buf.toString();
    }
}
