package com.trikke.data;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 02/12/13
 * Time: 13:42
 */
public class Constraint
{
	public enum Type
	{
		NOT_NULL,
		UNIQUE,
		PRIMARY_KEY,
		FOREIGN_KEY,
		CHECK,
		DEFAULT
	}

	public final String name;
	public final String value;
	public final Type type;

	public Constraint( String name, String value )
	{
		this.name = name;
		this.value = value;
		this.type = parse( value );
	}

	private Type parse( String value )
	{
		for ( Type type : Type.values() )
		{
			if ( value.toLowerCase().contains( type.name().replaceAll( "_", " " ).toLowerCase() ) )
			{
				return type;
			}
		}

		throw new NullPointerException( "Unidentified constraint found in " + value );
	}
}
