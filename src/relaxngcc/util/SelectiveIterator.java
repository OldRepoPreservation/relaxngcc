package relaxngcc.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator that returns a subset of another iterator
 * by filtering out some of the elements.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class SelectiveIterator implements Iterator {
    public SelectiveIterator( Iterator _base ) {
        this.base=_base;
    }
    /** base iterator to filter. */
    private final Iterator base;

    /** return false to skip this object. */
    protected abstract boolean filter( Object o );
            
    private Object next;
    /** Finds the next object to return, if any. */
    private void findNext() {
        if(next!=null)  return;
        
        while(base.hasNext()) {
            next = base.next();
            if(filter(next))
                return;  // this is fine.
        }
        next=null;  // not found
    }
    
    public boolean hasNext() {
        findNext();
        return next!=null;
    }
    public Object next() {
        findNext();
        if(next==null)  throw new NoSuchElementException();
        
        Object r = next;
        next=null;
        return r;
    }
    public void remove() { throw new UnsupportedOperationException(); }
    
}

