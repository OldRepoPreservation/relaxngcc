package relaxngcc.runtime;

import org.xml.sax.Attributes;

/**
 * One NGCC event.
 * 
 * <p>
 * All sub-classes are immutable.
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public abstract class NGCCEvent {
    public static abstract class Named extends NGCCEvent {
        public Named( String uri, String local, String qname ) {
            this.uri=uri; this.local=local; this.qname=qname;
        }
        public final String uri;
        public final String local;
        public final String qname;
    }
    
    public static final class EnterElement extends Named {
        public EnterElement( String uri, String local, String qname, Attributes _atts ) {
            super(uri,local,qname);
            this.atts = _atts;
        }
        public final Attributes atts;
    }
    
    public static final class LeaveElement extends Named {
        public LeaveElement( String uri, String local, String qname ) {
            super(uri,local,qname);
        }
    }
    
    public static final class EnterAttribute extends Named {
        public EnterAttribute( String uri, String local, String qname ) {
            super(uri,local,qname);
        }
    }
    
    public static final class LeaveAttribute extends Named {
        public LeaveAttribute( String uri, String local, String qname ) {
            super(uri,local,qname);
        }
    }
    
    public static final class Text extends NGCCEvent {
        public Text( String value ) {
            this.value=value;
        }
        public final String value;
    }
}
