package relaxngcc.grammar;

/**
 *
 *
 * @author
 *      Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ChoiceNameClass extends NameClass {
    public ChoiceNameClass(NameClass nc1_, NameClass nc2_) {
        this.nc1 = nc1_;
        this.nc2 = nc2_;
    }
    
    public final NameClass nc1;
    public final NameClass nc2;

    public Object apply(NameClassFunction f) {
        return f.choice(nc1,nc2);
    }
    
    public String toString() {
        return nc1.toString()+'|'+nc2.toString();
    }
}
