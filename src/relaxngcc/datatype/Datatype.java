package relaxngcc.datatype;

import java.io.IOException;
import java.util.HashMap;

import relaxngcc.NGCCGrammar;
import relaxngcc.codedom.CDExpression;
import relaxngcc.codedom.CDLanguageSpecificString;
import relaxngcc.codedom.CDType;
import relaxngcc.codedom.CDVariable;

/**
 * Represents a RELAX NG datatype and its parsing code generator.
 * 
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public final class Datatype {
    
    private final CDType returnType;
    private final Macro macro;
    private final String name;
    
    /**
     * Resources that need to be generated when this converter is used.
     */
    private final Resource[] requiredResources;
    
    /**
     * Datatype instance that doesn't do any conversion.
     * This is the default logic.
     */
    public static final Datatype NOOP;
    
    protected Datatype(String _name, CDType _returnType, Macro _macro,Resource[] _resources) {
        this.name = _name;
        this.returnType = _returnType;
        this.macro = _macro;
        this.requiredResources = _resources;
    }
    
    /**
     * Generates the expression that parses the specified string
     * into a target language value type.
     * 
     * @param grammar
     *      This datatype is used for this grammar.
     */
    public CDExpression generate( NGCCGrammar grammar, CDVariable $text ) throws NoDefinitionException, IOException {
        // let resources know that they are used.
        for (int i = 0; i < requiredResources.length; i++)
            requiredResources[i].use(grammar);
        
        HashMap dic = new HashMap();
        dic.put("value",$text.getName());
        return new CDLanguageSpecificString(macro.toString(dic));
    }
    
    /**
     * Returns the type to which the return value from the generate
     * method evaluates to.
     */
    public CDType getType() {
        return returnType;
    }
    
    /**
     * Gets the display name of this datatype.
     * Usually this is the datatype name, but not always.
     * 
     * @return      a non-null valid string.
     */
    public String displayName() {
        return name;
    }
    
    static {
        // create a datatype with no-transformation.
        Macro m = new Macro();
        m.add(new Macro.Variable("value",null));
        NOOP = new Datatype( "string", CDType.STRING, m, new Resource[0] );
    }
}
