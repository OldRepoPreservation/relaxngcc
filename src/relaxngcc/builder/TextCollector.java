package relaxngcc.builder;

import relaxngcc.grammar.*;

/**
 * Returns true if the given pattern can consume text.
 * 
 * <p>
 * Use the static "collect" method.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class TextCollector extends PatternWalker {
    public static boolean collect( Pattern p ) {
        TextCollector tc = new TextCollector();
        p.apply(tc);
        return tc.result;
    }
    
    private boolean result = false;
    
    public Object element(ElementPattern p) {
        return null;
    }
    public Object attribute(AttributePattern p) {
        return null;
    }
    
    public Object data(DataPattern p) {
        result = true;
        return super.data(p);
    }

    public Object list(ListPattern p) {
        result = true;
        return super.list(p);
    }

    public Object value(ValuePattern p) {
        result = true;
        return super.value(p);
    }

}
