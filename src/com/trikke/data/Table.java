package com.trikke.data;

import com.trikke.util.SqlUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 16:58
 */
public class Table extends SQLObject
{
	public static final String ANDROID_ID = "_id";

	public ArrayList<Triple<String, String, List<Constraint>>> fields = new ArrayList<Triple<String, String, List<Constraint>>>(  );
	public ArrayList<Constraint> constraints = new ArrayList<Constraint>(  );

	private boolean hasPrimaryKey;

	private Pair<String, String> primaryKey = null;

	public Table()
	{

	}

	public void addField( String type, String name, List<Constraint> constraints )
	{
		fields.add( new Triple<String, String, List<Constraint>>( SqlUtil.getJavaTypeFor( type ), name, constraints ) );
	}

	public void addConstraint( String name, String definition )
	{
		constraints.add( new Constraint( name, definition ) );
	}

	public String getSingleName()
	{
		return "SINGLE" + name;
	}

	public String getAllName()
	{
		return "ALL" + name;
	}

	public Triple<String, String, List<Constraint>> getFieldByName( String name )
	{
		for ( Triple<String, String, List<Constraint>> field : fields )
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
		primaryKey = new Pair<String, String>( SqlUtil.getJavaTypeFor( type ), name );
	}
}
