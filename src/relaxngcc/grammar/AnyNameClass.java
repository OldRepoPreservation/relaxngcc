package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class AnyNameClass extends NameClass {
    public AnyNameClass(ParserRuntime rt, NameClass _except) {
        this.except = _except;
    }
    
    public final NameClass except;

    public Object apply(NameClassFunction f) {
        return f.anyName(except);
    }
    
    public String toString() {
        return '*'+(except!=null?'-'+except.toString():"");
    }
}
