/*
 * Alphabet.java
 *
 * Created on 2001/08/04, 21:40
 */

package relaxngcc.automaton;
import relaxngcc.MetaDataType;
import relaxngcc.builder.NameClass;

/**
 * An alphabet in RelaxNGCC is one of following types:
 * 1. element start
 * 2. element end
 * 3. attribute start
 * 4. ref
 * 5. typed value
 *
 */
public class Alphabet implements Comparable
{
	public static final int START_ELEMENT = 1;
	public static final int END_ELEMENT = 2;
	public static final int START_ATTRIBUTE = 3;
	public static final int TYPED_VALUE = 4;
	public static final int FIXED_VALUE = 5;
	public static final int REF_BLOCK = 6;

	private int _Type; //one of above constants
	private MetaDataType _DataType; //used when _Type is TYPED_VALUE
	private String _Alias; //user-defined name for this alphabet
	private NameClass _Key;
	private String _Value;
	
	private Alphabet() {}
	/*
	 * constructs from type and key
	 */
	public Alphabet(int type, NameClass key)
	{ _Type=type; _Key = key; }
	public Alphabet(MetaDataType mdt, String alias)
	{ _Type = TYPED_VALUE; _DataType=mdt; _Alias = alias; }
	
    public static Alphabet createFixedValue(String key, String alias)
    {
        Alphabet a = new Alphabet();
		a._Type = FIXED_VALUE;
		a._Value = key;
        a._Alias = alias;
        return a;
    }
    public static Alphabet createRef(String n)
	{
        Alphabet a = new Alphabet();
		a._Type = REF_BLOCK;
		a._Value = n;
		return a;
	}
    public static Alphabet createRef(String n, String alias)
	{
        Alphabet a = new Alphabet();
		a._Type = REF_BLOCK;
		a._Value = n;
		a._Alias = alias;
		return a;
	}
	public NameClass getKey() { return _Key; }
	public MetaDataType getMetaDataType() { return _DataType; }
	public String getAlias() { return _Alias; }
	public int getType() { return _Type; }
	public String getValue() { return _Value; }
	
	public boolean equals(Object obj)
	{
		if(!(obj instanceof Alphabet)) return false; //equals never
		
		Alphabet i = (Alphabet)obj;
		if(_Type != i._Type) return false;
		
		if(_Type==TYPED_VALUE)
			return _DataType.equals(i._DataType);
		else if(_Type==FIXED_VALUE || _Type==REF_BLOCK)
			return _Value.equals(i._Value);
		else
			return _Key.equals(i._Key);
	}

	public int compareTo(Object o)
	{
		if(!(o instanceof Alphabet)) throw new ClassCastException("not an Alphabet");
		
		Alphabet i = (Alphabet)o;
		if(_Type != i._Type) return _Type-i._Type;
		
		if(_Type==TYPED_VALUE)
			return _DataType.hashCode() - i._DataType.hashCode();
		else if(_Type==FIXED_VALUE || _Type==REF_BLOCK)
			return _Value.compareTo(i._Value);
		else
			return _Key.compareTo(i._Key);
		
	}
	
	/**
	 * dumps this alphabet
	 */
	public String toString()
	{
		switch(_Type)
		{
			case START_ELEMENT:
				return "startElement '" + _Key.toString() + "'";
			case END_ELEMENT:
				return "endElement '" + _Key.toString() + "'";
			case START_ATTRIBUTE:
				return "attribute '" + _Key.toString() + "'";
			case TYPED_VALUE:
				return "data '" + _DataType.getXSTypeName() + "'";
			case FIXED_VALUE:
				return "value '" + _Value + "'";
			case REF_BLOCK:
				return "ref '" + _Value + "'";
			default:
				return super.toString();
		}
	}
}
