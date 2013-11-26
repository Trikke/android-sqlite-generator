package com.trikke.data;

import com.trikke.util.Util;

import java.util.ArrayList;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 16:58
 */
public class Table extends SQLObject
{
	public static final String ANDROID_ID = "_id";

	public ArrayList<Pair<String, String>> fields = new ArrayList<Pair<String, String>>();
	public ArrayList<Pair<String, String>> constraints = new ArrayList<Pair<String, String>>();

	public String uniqueKey;

	public Table()
	{

	}

	public void addField( String type, String name )
	{
		fields.add( new Pair<String, String>( Util.getValidType(type), name ) );
	}

	public void addConstraint( String name, String constraint )
	{
		constraints.add( new Pair<String, String>( name, constraint ) );
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

	public String findConstraint( String name )
	{
		for ( Pair<String, String> constr : constraints )
		{
			if ( constr.fst.equals( name ) )
			{
				return constr.snd;
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
