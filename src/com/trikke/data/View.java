package com.trikke.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 18/10/13
 * Time: 15:34
 */
public class View extends SQLObject
{
	private static final String VIEW_SELECT = "select";
	private static final String VIEW_FROM = "from";
	private static final String VIEW_ON_JOIN = "on";
	private static final String VIEW_GROUP_BY = "group";
	private static final String VIEW_ORDER_BY = "order";

	public String jointype = "left join";

	private ArrayList<String> fromtables = new ArrayList<String>();
	private ArrayList<String> joinonfields = new ArrayList<String>();
	private HashMap<String, String> orderfields = new HashMap<String, String>();
	private ArrayList<String> groupfields = new ArrayList<String>();

	public void addField( String[] parts )
	{
		String what = parts[0];
		String name = parts[1];

		if ( what.equals( VIEW_SELECT ) )
		{
			fields.add( new Pair<String, String>( name, (parts.length > 2) ? parts[2] : null ) );
		}

		if ( what.equals( VIEW_FROM ) )
		{
			fromtables = new ArrayList<String>( Arrays.asList( name.toUpperCase().split( "\\s*,\\s*" ) ) );
		}

		if ( what.equals( VIEW_ON_JOIN ) )
		{
			joinonfields = new ArrayList<String>( Arrays.asList( name.split( "\\s*,\\s*" ) ) );
		}

		if ( what.equals( VIEW_ORDER_BY ) )
		{
			orderfields.put( name, (parts.length > 2) ? parts[2] : null );
		}

		if ( what.equals( VIEW_GROUP_BY ) )
		{
			groupfields.add( name );
		}
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
