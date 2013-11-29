package com.trikke.data;

import com.trikke.util.SqlUtil;

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

	public ArrayList<Triple<String, String, String>> fields = new ArrayList<Triple<String, String, String>>();
	public ArrayList<Pair<String, String>> constraints = new ArrayList<Pair<String, String>>();

	private boolean hasPrimaryKey;

	private Pair<String, String> primaryKey = null;

	public Table()
	{

	}

	public void addField( String type, String name, String constraints )
	{
		if ( type.equals( "autoincrement" ) )
		{
			constraints = "primary key autoincrement";
		}
		fields.add( new Triple<String, String, String>( SqlUtil.getSQLtypeFor( type ), name, constraints ) );
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

	public Triple<String, String, String> getFieldByName( String name )
	{
		for ( Triple<String, String, String> field : fields )
		{
			if ( field.snd.toLowerCase().equals( name.toLowerCase() ) )
			{
				return field;
			}
		}
		return null;
	}

	public Pair<String, String> getPrimaryKey()
	{
		if ( hasPrimaryKey )
		{
			return primaryKey;
		}
		return new Pair<String, String>( "int", ANDROID_ID );
	}

	public boolean hasPrimaryKey()
	{
		return hasPrimaryKey;
	}

	public void setPrimaryKey( String type, String name )
	{
		this.hasPrimaryKey = true;
		primaryKey = new Pair<String, String>( SqlUtil.getSQLtypeFor( type ), name );
	}
}
