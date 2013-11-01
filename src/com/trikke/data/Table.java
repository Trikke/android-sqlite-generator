package com.trikke.data;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 16:58
 */
public class Table extends SQLObject
{
	public static final String ANDROID_ID = "_id";

	public String uniqueKey;

	public Table()
	{

	}

	public void addField( String type, String name )
	{
		fields.add( new Pair<String, String>( type, name ) );
	}

	public String getSingleName()
	{
		return "SINGLE" + name;
	}

	public String getAllName()
	{
		return "ALL" + name;
	}

	private String findType( String name )
	{
		for ( Pair<String, String> field : fields )
		{
			if ( field.snd.equals( name ) )
			{
				return field.fst;
			}
		}

		return null;
	}

	public Pair<String, String> UNIQUEROWID()
	{
		if ( uniqueKey != null )
		{
			return new Pair<String, String>( String.valueOf( findType( uniqueKey ) ), uniqueKey );
		} else
		{
			return new Pair<String, String>( "int", ANDROID_ID );
		}
	}
}
