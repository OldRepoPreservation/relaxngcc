/*
 * Alphabet.java
 *
 * Created on 2001/08/04, 21:40
 */

package relaxngcc.automaton;
import relaxngcc.MetaDataType;
import relaxngcc.NGCCGrammar;
import relaxngcc.builder.NameClass;
import relaxngcc.builder.ScopeInfo;

/**
 * An alphabet in RelaxNGCC is one of following types:
 * 1. element start
 * 2. element end
 * 3. attribute start
 * 3. attribute end
 * 4. ref
 * 5. typed value (&lt;data>)
 * 6. fixed value (&lt;value>)
 *
 */
public abstract class Alphabet implements Comparable
{
    // type of alphabets
	public static final int ENTER_ELEMENT      = 1;
	public static final int LEAVE_ELEMENT      = 2;
	public static final int ENTER_ATTRIBUTE    = 4;
    public static final int LEAVE_ATTRIBUTE    = 8;
    public static final int REF_BLOCK          = 64;
	public static final int DATA_TEXT          = 16;
	public static final int VALUE_TEXT         = 32;
    
    /**
     * type of this alphabet. One of the above constants.
     */
	private final int _Type;
    public final int getType() { return _Type; }

    protected Alphabet( int type ) {
        this._Type = type;
    }


    
    //
    // dynamic cast functions
    //
    public Markup           asMarkup() { return null; }
        public EnterElement     asEnterElement() { return null; }
        public LeaveElement     asLeaveElement() { return null; }
        public EnterAttribute   asEnterAttribute() { return null; }
        public LeaveAttribute   asLeaveAttribute() { return null; }
    public Ref              asRef() { return null; }
    public Text             asText() { return null; }
        public ValueText        asValueText() { return null; }
        public DataText         asDataText() { return null; }
    
    //
    // type check functions
    //
    public final boolean isMarkup() { return asMarkup()!=null; }
    public final boolean isEnterElement() { return asEnterElement()!=null; }
    public final boolean isLeaveElement() { return asLeaveElement()!=null; }
    public final boolean isEnterAttribute() { return asEnterAttribute()!=null; }
    public final boolean isLeaveAttribute() { return asLeaveAttribute()!=null; }
    public final boolean isRef() { return asRef()!=null; }
    public final boolean isText() { return asText()!=null; }
    public final boolean isValueText() { return asValueText()!=null; }
    public final boolean isDataText() { return asDataText()!=null; }
    
    /**
     * Base class for (enter|leave)(Attribute|Element).
     */
    public static abstract class Markup extends Alphabet {
        protected Markup( int type, NameClass _key ) {
            super(type);
            this.key = _key;
        }
        
        /**
         * Label of this transition.
         * A transition is valid if the element/attribute name
         * is accepted by this name class.
         */
        private final NameClass key;
        // TODO: the variable name "key" seems to be wrong.
        public NameClass getKey() { return key; }
        
        public Markup asMarkup() { return this; }
        
        public int hashCode() {
            return key.hashCode() ^ getType();
        }
        public int compareTo( Object o ) {
            int r = super.compareTo(o);
            if(r!=0)    return r;
            
            return key.compareTo(((Markup)o).key);
        }
    }
    
    /** Alphabet of the type "enter element." */
    public static class EnterElement extends Markup {
        public EnterElement( NameClass key ) {
            super( ENTER_ELEMENT, key );
        }
        public EnterElement asEnterElement() { return this; }
        public String toString() { return "enterElement '"+getKey()+"'"; }
    }
    
    /** Alphabet of the type "leave element." */
    public static class LeaveElement extends Markup {
        public LeaveElement( NameClass key ) {
            super( LEAVE_ELEMENT, key );
        }
        public LeaveElement asLeaveElement() { return this; }
        public String toString() { return "leaveElement '"+getKey()+"'"; }
    }
    
    /** Alphabet of the type "enter attribute." */
    public static class EnterAttribute extends Markup {
        public EnterAttribute( NameClass key ) {
            super( ENTER_ATTRIBUTE, key );
        }
        public EnterAttribute asEnterAttribute() { return this; }
        public String toString() { return "enterAttribute '"+getKey()+"'"; }
    }
    
    /** Alphabet of the type "leave attribute." */
    public static class LeaveAttribute extends Markup {
        public LeaveAttribute( NameClass key ) {
            super( LEAVE_ATTRIBUTE, key );
        }
        public LeaveAttribute asLeaveAttribute() { return this; }
        public String toString() { return "leaveAttribute '"+getKey()+"'"; }
    }
    
    /** Alphabet of the type "ref." */
    public static class Ref extends Alphabet {
        public Ref( NGCCGrammar _grammar, String _target, String _alias, String _params ) {
            super( REF_BLOCK );
            this.grammar = _grammar;
            this.target = _target;
            this.alias = _alias;
            this.params = _params;
        }
        public Ref( NGCCGrammar _grammar, String _target ) {
            this(_grammar,_target,null,null);
        }
        public Ref asRef() { return this; }
        
        /**
         * The grammar with which we resolve the target name.
         * Note that we don't need to take this variable into account
         * when comparing equality, because we only compare alphabets
         * that belong to the same grammar.
         */
        private final NGCCGrammar grammar;
        
        /** Name of the scope object to be spawned. */
        private final String target;

        /** Gets the child scope to be spawned. */
        public ScopeInfo getTargetScope() {
            return grammar.getScopeInfoByName(target);
        }
        
        /**
         * Additional parameters passed to
         * the constructor of the child object.
         * 
         * Used only with Alphabets of the REF_BLOCK type.
         */
        private final String params;
        public String getParams() {
            // TODO: this might be an excessively work.
            // maybe I should just return the value as is
            if(params==null)  return "";
            return ','+params;
        }
        
        /**
         * User-defined variable name assigned to this alphabet.
         * User program can access this child object through this
         * variable.
         */
        private final String alias;
        public String getAlias() { return alias; }

        public String toString() { return "ref '"+target+"'"; }
        
        public int hashCode() {
            return h(target)^h(alias)^h(params);
        }
        public int compareTo( Object o ) {
            int r = super.compareTo(o);
            if(r!=0)    return r;
            
            Ref rhs = (Ref)o;
            r = compare(target,rhs.target);
            if(r!=0)    return r;
            r = compare(alias,rhs.alias);
            if(r!=0)    return r;
            return compare(params,rhs.params);
        }
    }
    
    
    public static abstract class Text extends Alphabet {
        protected Text( int _type, String _alias ) {
            super(_type);
            this.alias = _alias;
        }
        public Text asText() { return this; }
        
        /**
         * User-defined variable name assigned to this alphabet.
         * User program can access this child object through this
         * variable.
         */
        private final String alias;
        public String getAlias() { return alias; }   
        
        public int compareTo( Object o ) {
            int r = super.compareTo(o);
            if(r!=0)    return r;
            
            return compare(alias,((Text)o).alias);
        }
    }
    
    public static class ValueText extends Text {
        public ValueText( String _value, String _alias ) {
            super(VALUE_TEXT,_alias);
            this.value = _value;
        }
        public ValueText asValueText() { return this; }
        
        /**
         * Value of the &lt;value> element.
         */
        private final String value;
        public String getValue() { return value; }
        
        public String toString() { return "value '"+value+"'"; }
        public int hashCode() { return value.hashCode(); }
        public int compareTo( Object o ) {
            int r = super.compareTo(o);
            if(r!=0)    return r;
            
            return compare(value,((ValueText)o).value);
        }
    }
    
    public static class DataText extends Text {
        public DataText( MetaDataType dt, String _alias ) {
            super(DATA_TEXT,_alias);
            this._DataType = dt;
        }
        public DataText asDataText() { return this; }

        /** Datatype of this &lt;data> element. */
        private final MetaDataType _DataType;
        public MetaDataType getMetaDataType() { return _DataType; }
        
        public String toString() { return "data '"+_DataType.getXSTypeName()+"'"; }
        public int hashCode() { return _DataType.hashCode(); }
        public int compareTo( Object o ) {
            int r = super.compareTo(o);
            if(r!=0)    return r;
            
            // TODO: datatype is uncomparable!!
//            return compare(_DataType,((DataText)o)._DataType);
            return _DataType.hashCode() - ((DataText)o)._DataType.hashCode();
        }
    }


    public final boolean equals( Object o ) {
        if( o instanceof Alphabet )
            return compareTo(o)==0;
        else
            return false;
    }
    
    public int compareTo(Object o) {
        if(!(o instanceof Alphabet)) throw new ClassCastException("not an Alphabet");
        
        return _Type-((Alphabet)o)._Type;
    }
    
    // the hashCode method needs to be implemented properly
    public abstract int hashCode();

    /** Computes the hashCode of the object, even if it's null. */
    protected static int h( Object o ) {
        if(o==null) return 0;
        else        return o.hashCode();
    }
    /** Compares two objects, even if they are null. */
    protected static int compare( Comparable o1, Comparable o2 ) {
        if(o1==null && o2==null)    return 0;
        if(o1==null)                return -1;
        if(o2==null)                return +1;
        return o1.compareTo(o2);
    }
}
