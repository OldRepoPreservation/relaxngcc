package relaxngcc.grammar;

import relaxngcc.parser.ParserRuntime;
/**
 * 
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class NsNameClass extends NameClass {
    public NsNameClass(ParserRuntime rt, String uri_, NameClass except_) {
        this.uri = uri_;
        this.except = except_;
    }
    
    public final String uri;
    public final NameClass except;

    public Object apply(NameClassFunction f) {
        return f.nsName(uri,except);
    }
    
    public String toString() {
        return '{'+uri+'}'+(except!=null?'-'+except.toString():"");
    }
    
}
