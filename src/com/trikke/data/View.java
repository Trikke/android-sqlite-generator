package com.trikke.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 18/10/13
 * Time: 15:34
 */
public class View extends SQLObject
{
	public String jointype = "left join";

	private ArrayList<String> fromtables = new ArrayList<String>();
	private ArrayList<String> joinonfields = new ArrayList<String>();
	private HashMap<String, String> orderfields = new HashMap<String, String>();
	private ArrayList<String> groupfields = new ArrayList<String>();
	public ArrayList<Pair<String, String>> fields = new ArrayList<Pair<String, String>>();

	public void addSelect( String name, String from )
	{
		if ( name == null )
		{
			name = from.split( "\\." )[1];
		}
		fields.add( new Pair<String, String>( from, name ) );
	}

	public void addFromTable( String name )
	{
		fromtables.add( name );
	}

	public void addJoinOn( String name )
	{
		joinonfields.add( name );
	}

	public void addOrder( String by, String order )
	{
		orderfields.put( by, order );
	}

	public void addGroup( String by )
	{
		groupfields.add( by );
	}

	public ArrayList<String> getFromtables()
	{
		return fromtables;
	}

	public ArrayList<String> getJoinonfields()
	{
		return joinonfields;
	}

	public ArrayList<String> getGroupfields()
	{
		return groupfields;
	}

	public HashMap<String, String> getOrderfields()
	{
		return orderfields;
	}
}
