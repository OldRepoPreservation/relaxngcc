package relaxngcc.grammar;

import org.xml.sax.Locator;

import relaxngcc.parser.ParserRuntime;
/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class RefPattern extends Pattern {
    public RefPattern(ParserRuntime rt, Locator loc, Scope target_, NGCCCallParam param_) {
        this.target = target_;
        this.param = param_;
        this.locator = loc;
    }
    
    public final Locator locator;
    public final Scope target;
    public final NGCCCallParam param;

    public Object apply( PatternFunction f ) {
        return f.ref(this);
    }
    
    public int getParamCount() {
        String s = param.getWithParams();
        if(s==null || s.length()==0) return 0;
        //count the number of comma
        int c = 1;
        int bracket = 0;
        for(int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            if(bracket==0 && ch==',') c++;
            
            if(ch=='(') bracket++;
            else if(ch==')') bracket--;
        }
        return c;
    }
}
