package relaxngcc.util;

import java.util.Iterator;

/**
 * Iterator that walks over two other iterators.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class ConcatIterator implements Iterator {
    public ConcatIterator( Iterator _first, Iterator _second ) {
        this.first = _first;
        this.second = _second;
    }
    
    private Iterator first,second;
    
    public boolean hasNext() { return first.hasNext()||second.hasNext(); }
    public Object next() {
        if(first.hasNext()) return first.next();
        else                return second.next();
    }
    public void remove() { throw new UnsupportedOperationException(); }
}
