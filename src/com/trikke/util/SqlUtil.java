package com.trikke.util;

import com.trikke.data.*;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 21:04
 */
public class SqlUtil
{
	public static String URI( SQLObject obj )
	{
		return obj.name + "_URI";
	}

	public static String IDENTIFIER( SQLObject obj )
	{
		return obj.name + "_TABLE";
	}

	public static String ROW_COLUMN( SQLObject obj, Pair<String, String> row )
	{
		return obj.name + "_" + row.snd.toUpperCase() + "_COLUMN";
	}

	public static String ROW_COLUMN_POSITION( SQLObject obj, Pair<String, String> row )
	{
		return obj.name + "_" + row.snd.toUpperCase() + "_COLUMN_POSITION";
	}

	public static String ROW_COLUMN( SQLObject obj, String selector )
	{
		return obj.name + "_" + printSelect( selector ).toUpperCase() + "_COLUMN";
	}

	public static String ROW_COLUMN_POSITION( SQLObject obj, String selector )
	{
		return obj.name + "_" + printSelect( selector ).toUpperCase() + "_COLUMN_POSITION";
	}

	public static String generateCreateStatement( Model model, Table table )
	{
		String statement = "create table " + table.name + " (";

		// default android row
		statement += Table.ANDROID_ID + " integer primary key autoincrement,\" + \n";

		Iterator<Pair<String, String>> iterator = table.fields.iterator();

		while ( iterator.hasNext() )
		{
			Pair<String, String> row = iterator.next();
			statement += "\t\t\t " + table.name.toUpperCase() + "_" + row.snd.toUpperCase() + "_COLUMN + \" " + getSQLtypeFor( row.fst );
			if ( iterator.hasNext() )
			{
				statement += ",\" + \n";
			}
		}

		if ( table.uniqueKey != null )
		{
			statement += ",\" + \n\t\t\t\" UNIQUE (" + table.uniqueKey + ") ON CONFLICT "+model.getConflictStrategy()+");";
		} else
		{
			statement += ");";
		}

		return statement;
	}

	public static String generateCreateStatement( View view )
	{
		String statement;

		statement = "CREATE VIEW " + view.name + " AS \" +\n";
		statement += "\t\t\t\"SELECT \" +\n";
		Iterator<Pair<String, String>> iterator = view.fields.iterator();
		Pair<String, String> select;
		while ( iterator.hasNext() )
		{
			select = iterator.next();
			statement += "\t\t\t\t\"" + select.fst + " AS " + printSelect( (select.snd == null) ? select.fst : select.snd );
			if ( iterator.hasNext() )
			{
				statement += ", ";
			}
			statement += "\"+\n";
		}
		statement += "\t\t\t\" FROM " + view.getFromtables().get( 0 ) + "\" + \n";

		int i;
		String tablename;
		for ( i = 1; i < view.getFromtables().size(); i++ )
		{
			tablename = view.getFromtables().get( i );
			statement += "\t\t\t\t\" " + view.jointype.toUpperCase() + " " + tablename + " ON ";
			if ( i <= view.getJoinonfields().size() )
			{
				statement += view.getFromtables().get( 0 ) + "." + view.getJoinonfields().get( i - 1 ) + " = " + tablename + "." + view.getJoinonfields().get( i - 1 );
			}
			if ( i < view.getFromtables().size() - 1 )
			{
				statement += "\" +\n";
			}
		}

		if ( !view.getGroupfields().isEmpty() )
		{
			statement += "\" +\n\t\t\t\" GROUP BY ";
		}

		Iterator<String> groupiterator = view.getGroupfields().iterator();
		while ( groupiterator.hasNext() )
		{
			statement += groupiterator.next();
			if ( iterator.hasNext() )
			{
				statement += ", ";
			}
		}

		if ( !view.getOrderfields().isEmpty() )
		{
			statement += "\" +\n\t\t\t\" ORDER BY ";
		}

		Iterator it = view.getOrderfields().entrySet().iterator();
		while ( it.hasNext() )
		{
			Map.Entry entry = (Map.Entry) it.next();

			if ( entry.getValue() == null )
			{
				statement += entry.getKey();
			} else
			{
				statement += entry.getKey() + " " + entry.getValue();
			}

			if ( it.hasNext() )
			{
				statement += ", ";
			}
		}

		return statement;
	}

	public static String printSelect( String selector )
	{
		String[] splitter = selector.split( "\\." );
		if ( splitter.length <= 1 )
		{
			return selector;
		}
		String output = "";
		if ( splitter[0].contains( "(" ) )
		{
			// its a function!
			output += splitter[0].substring( 0, splitter[0].indexOf( "(" ) );
		}
		output += Util.sanitize( splitter[1], false );

		return output;
	}

	public static String getSQLtypeFor( String type )
	{
		type = Util.sanitize( type, false );
		if ( type.equals( "Float" ) )
		{
			return "float";
		}
		if ( type.equals( "Double" ) )
		{
			return "real";
		}
		if ( type.equals( "Long" ) )
		{
			return "integer";
		}
		if ( type.equals( "Integer" ) )
		{
			return "integer";
		}
		if ( type.equals( "Date" ) )
		{
			return "integer";
		}
		if ( type.equals( "float" ) )
		{
			return "float";
		}
		if ( type.equals( "double" ) )
		{
			return "real";
		}
		if ( type.equals( "long" ) )
		{
			return "integer";
		}
		if ( type.equals( "int" ) )
		{
			return "integer";
		}

		if ( type.equals( "Boolean" ) )
		{
			return "boolean";
		}

		if ( type.equals( "boolean" ) )
		{
			return "boolean";
		}

		if ( type.equals( "String" ) )
		{
			return "text";
		}

		if ( type.equals( "string" ) )
		{
			return "text";
		}

		return "blob";
	}
}
