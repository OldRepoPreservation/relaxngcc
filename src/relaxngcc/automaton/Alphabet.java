/*
 * Alphabet.java
 *
 * Created on 2001/08/04, 21:40
 */

package relaxngcc.automaton;
import java.io.PrintStream;
import java.util.Comparator;

import org.xml.sax.Locator;

import relaxngcc.MetaDataType;
import relaxngcc.NGCCGrammar;
import relaxngcc.builder.ScopeInfo;
import relaxngcc.grammar.NameClass;

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
public abstract class Alphabet
{
    // type of alphabets
	public static final int ENTER_ELEMENT      = 1;
	public static final int LEAVE_ELEMENT      = 2;
	public static final int ENTER_ATTRIBUTE    = 4;
    public static final int LEAVE_ATTRIBUTE    = 8;
	public static final int DATA_TEXT          = 16;
	public static final int VALUE_TEXT         = 32;
    public static final int REF_BLOCK          = 64;
    public static final int FORK               = 128;
    
    /** Type of this alphabet. One of the above constants. */
	private final int _Type;
    public final int getType() { return _Type; }
    
    /** Source location where this alphabet came from. */
    public final Locator locator;
    
    protected Alphabet( int type, Locator loc ) {
        this._Type = type;
        this.locator = loc;
    }


    /** Prints the locator associated with this. */    
    public void printLocator( PrintStream out ) {
        if(locator!=null) {
            out.print("  line ");
            out.print(locator.getLineNumber());
            out.print(" of ");
            out.println(locator.getSystemId());
        }
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
    public Fork             asFork() { return null; }
    
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
    public final boolean isFork() { return asFork()!=null; }
    
    /**
     * Base class for (enter|leave)(Attribute|Element).
     */
    public static abstract class Markup extends Alphabet {
        protected Markup( int type, NameClass _key, Locator loc ) {
            super(type,loc);
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
        public boolean equals( Object o ) {
            if(!super.equals(o))    return false;
            return equals(key,((Markup)o).key);
        }
    }
    
    /** Alphabet of the type "enter element." */
    public static class EnterElement extends Markup {
        public EnterElement( NameClass key, Locator loc ) {
            super( ENTER_ELEMENT, key, loc );
        }
        public EnterElement asEnterElement() { return this; }
        public String toString() { return "<"+getKey()+">"; }
    }
    
    /** Alphabet of the type "leave element." */
    public static class LeaveElement extends Markup {
        public LeaveElement( NameClass key, Locator loc ) {
            super( LEAVE_ELEMENT, key, loc );
        }
        public LeaveElement asLeaveElement() { return this; }
        public String toString() { return "</"+getKey()+">"; }
    }
    
    /** Alphabet of the type "enter attribute." */
    public static class EnterAttribute extends Markup implements WithOrder {
        public EnterAttribute( NameClass key, int order, Locator loc ) {
            super( ENTER_ATTRIBUTE, key, loc );
            _Order = order;
        }
        public EnterAttribute asEnterAttribute() { return this; }
        public String toString() { return "@"+getKey(); }
        
        /**
         * See AttributePattern for detail.
         * With this flag set to true, this attribute transition
         * will not be cloned. 
         */
        public boolean workaroundSignificant;
        
        private final int _Order;
        /**
         * Gets the number that introduces order
         * relationship between attribute declarations.
         * Attributes that appear later in the schema gets
         * yonger number.
         */
        public final int getOrder() { return _Order; }
    }
    
    /** Alphabet of the type "leave attribute." */
    public static class LeaveAttribute extends Markup {
        public LeaveAttribute( NameClass key, Locator loc ) {
            super( LEAVE_ATTRIBUTE, key, loc );
        }
        public LeaveAttribute asLeaveAttribute() { return this; }
        public String toString() { return "/@"+getKey(); }
    }
    
    /** Alphabet that "forks" a state into a set of sub-automata. */
    public static final class Fork extends Alphabet {
        public Fork( State[] subAutomata,
            NameClass[] elementNC, NameClass[] attNC, boolean[] text,
            Locator loc ) {
            
            super( FORK, loc );
            this._subAutomata = subAutomata;
            this.elementNameClasses = elementNC;
            this.attributeNameClasses = attNC;
            this.canConsumeText = text;
        }
        
        /** Initial states of sub-automata. */
        public final State[] _subAutomata;
        
        /** NameClass that represents elements that can be consumed by each bracnh.*/
        public final NameClass[] elementNameClasses;
        /** for attributes. */
        public final NameClass[] attributeNameClasses;
        /** for texts. */
        public final boolean[] canConsumeText;
        
        public String toString() {
            StringBuffer buf = new StringBuffer("fork&join ");
            for( int i=0; i<_subAutomata.length; i++ ) {
                if(i!=0)    buf.append(',');
                buf.append( Integer.toString( _subAutomata[i].getIndex() ) );
            }
            return buf.toString();
        }
        
        public int hashCode() {
            int h=0;
            for( int i=0; i<_subAutomata.length; i++ )
                h ^= _subAutomata[i].hashCode();
            return h;
        }
        public boolean equals( Object o ) {
            if(!super.equals(o)) return false;
            
            Fork rhs = (Fork)o;
            if(_subAutomata.length!=rhs._subAutomata.length)    return false;
            
            for( int i=_subAutomata.length-1; i>=0; i-- )
                if( _subAutomata[i]!=rhs._subAutomata[i] )
                    return false;
            
            return true;
        }
        public Fork asFork() { return this; }
        
        /**
         * Gets the name of the InterleaveFilter implementation class.
         */
        public String getClassName() {
            StringBuffer id = new StringBuffer("InterleaveFilter");
            for( int i=0; i<_subAutomata.length; i++ ) {
                id.append('_');
                id.append(_subAutomata[i].getIndex());
            }
            return id.toString();
        }
    }
    
    /** Alphabet of the type "ref." */
    public static final class Ref extends Alphabet implements WithOrder {
        public Ref( ScopeInfo target, String alias, String params, int order, Locator loc ) {
            super( REF_BLOCK, loc );
            this._Target = target;
            this._Alias  = alias;
            this._Params = params;
            this._Order = order;
        }
        public Ref( ScopeInfo _target, int order, Locator loc ) {
            this(_target,null,null,order,loc);
        }
        public Ref asRef() { return this; }
        
        /** Name of the scope object to be spawned. */
        private final ScopeInfo _Target;

        /** Gets the child scope to be spawned. */
        public ScopeInfo getTargetScope() {
            return _Target;
        }
        
        /** order relationship between attributes and refs. */
        private final int _Order;
        public final int getOrder() { return _Order; }
        
        /**
         * Additional parameters passed to
         * the constructor of the child object.
         * 
         * Used only with Alphabets of the REF_BLOCK type.
         */
        private final String _Params;
        public String getParams() {
            // TODO: this might be an excessively work.
            // maybe I should just return the value as is
            if(_Params==null)  return "";
            return ','+_Params;
        }
        
        /**
         * User-defined variable name assigned to this alphabet.
         * User program can access this child object through this
         * variable.
         */
        private final String _Alias;
        public String getAlias() { return _Alias; }

        public String toString() { return "ref '"+_Target.getClassName()+"'"; }
        
        public int hashCode() {
            return h(_Target)^h(_Alias)^h(_Params);
        }
        public boolean equals( Object o ) {
            if(!super.equals(o)) return false;
            
            Ref rhs = (Ref)o;
            if(!equals(_Target,rhs._Target))    return false;
            if(!equals(_Alias, rhs._Alias ))    return false;
            return equals(_Params,rhs._Params);
        }
    }
    
    
    public static abstract class Text extends Alphabet {
        protected Text( int _type, String _alias, Locator loc ) {
            super(_type,loc);
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
        
        public boolean equals( Object o ) {
            if(!super.equals(o)) return false;
            return equals(alias,((Text)o).alias);
        }
    }
    
    public static class ValueText extends Text {
        public ValueText( String _value, String _alias, Locator loc ) {
            super(VALUE_TEXT,_alias,loc);
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
        public boolean equals( Object o ) {
            if(!super.equals(o)) return false;
            return value.equals( ((ValueText)o).value );
        }
    }
    
    public static class DataText extends Text {
        public DataText( MetaDataType dt, String _alias, Locator loc ) {
            super(DATA_TEXT,_alias,loc);
            this._DataType = dt;
        }
        public DataText asDataText() { return this; }

        /** Datatype of this &lt;data> element. */
        private final MetaDataType _DataType;
        public MetaDataType getMetaDataType() { return _DataType; }
        
        public String toString() { return "data '"+_DataType.name+"'"; }
        public int hashCode() { return _DataType.hashCode(); }
        public boolean equals( Object o ) {
            if(!super.equals(o)) return false;
            return _DataType.equals( ((DataText)o)._DataType );
        }
    }


    public  boolean equals( Object o ) {
        if(!this.getClass().isInstance(o))    return false;
        return _Type==((Alphabet)o)._Type;
    }
    
    // the hashCode method needs to be implemented properly
    public abstract int hashCode();

    /** Computes the hashCode of the object, even if it's null. */
    protected static int h( Object o ) {
        if(o==null) return 0;
        else        return o.hashCode();
    }
    /** Compares two objects even if some of them are null. */
    protected static boolean equals( Object o1, Object o2 ) {
        if(o1==null && o2==null)    return true;
        if(o1==null || o2==null)    return false;
        return o1.equals(o2);
    }
    
    /**
     * Implemented by those alphabets that have orders.
     * Currently, just EnterAttribute and Ref.
     */
    public static interface WithOrder {
        public int getOrder();
    }
    
    /**
     * Comparator that can be used to sort ordered alphabets into
     * descending orders (larger numbers first.)
     */
    public static Comparator orderComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            return ((WithOrder)o2).getOrder()-((WithOrder)o1).getOrder();
        }
    };
}
