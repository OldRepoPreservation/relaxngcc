package relaxngcc.builder;

import relaxngcc.BuildError;
import relaxngcc.grammar.AttributePattern;
import relaxngcc.grammar.DataPattern;
import relaxngcc.grammar.ElementPattern;
import relaxngcc.grammar.ListPattern;
import relaxngcc.grammar.Pattern;
import relaxngcc.grammar.PatternWalker;
import relaxngcc.grammar.ValuePattern;

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
        return tc._result;
    }
    
    private boolean _result = false;
    
    public Object element(ElementPattern p) {
        return null;
    }
    public Object attribute(AttributePattern p) {
        return null;
    }
    
    public Object data(DataPattern p) {
        _result = true;
        return super.data(p);
    }

    public Object list(ListPattern p) {
        _result = true;
        return super.list(p);
    }

    public Object value(ValuePattern p) {
        _result = true;
        return super.value(p);
    }

    public void addError(BuildError err) {
        throw new UnsupportedOperationException();
    }
}
