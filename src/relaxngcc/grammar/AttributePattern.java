package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AttributePattern extends MarkupPattern {
    public AttributePattern(NameClass _name, Pattern _body, boolean _workaroundSignificant) {
        super(_name, _body);
        this.workaroundSignificant = _workaroundSignificant;
    }
    
    /**
     * Temporary mechanism to allow users to avoid a bug in RelaxNGCC.
     * 
     * For details, see
     * <a href="https://sourceforge.net/tracker/index.php?func=detail&aid=579864&group_id=53706&atid=471312">
     * the bug report
     * </a>.
     * 
     * This flag forces a compiler to generate an attribute transition
     * that does NOT come back to the start state.
     */
    public final boolean workaroundSignificant;
    
    public Object apply( PatternFunction f ) {
        return f.attribute(this);
    }
}
