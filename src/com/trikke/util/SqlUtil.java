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
		return obj.name.toUpperCase() + "_URI";
	}

	public static String IDENTIFIER( SQLObject obj )
	{
		return obj.name.toUpperCase() + "_TABLE";
	}

	public static String ROW_COLUMN( SQLObject obj, Triple<String, String, String> row )
	{
		return obj.name.toUpperCase() + "_" + row.snd.toUpperCase() + "_COLUMN";
	}

	public static String ROW_COLUMN_POSITION( SQLObject obj, Triple<String, String, String> row )
	{
		return obj.name.toUpperCase() + "_" + row.snd.toUpperCase() + "_COLUMN_POSITION";
	}

	public static String ROW_COLUMN( SQLObject obj, String selector )
	{
		return obj.name.toUpperCase() + "_" + printSelect( selector ).toUpperCase() + "_COLUMN";
	}

	public static String ROW_COLUMN_POSITION( SQLObject obj, String selector )
	{
		return obj.name.toUpperCase() + "_" + printSelect( selector ).toUpperCase() + "_COLUMN_POSITION";
	}

	public static String generateCreateStatement( Model model, Table table )
	{
		String statement = "CREATE TABLE " + table.name + " (\" + \n";

		// default android row
		if ( !table.hasPrimaryKey() )
		{
			statement += "\t\t\t \"" + Table.ANDROID_ID + " integer primary key autoincrement,\" + \n";
		}

		Iterator<Triple<String, String, String>> fieldsiter = table.fields.iterator();

		while ( fieldsiter.hasNext() )
		{
			Triple<String, String, String> row = fieldsiter.next();

			statement += "\t\t\t " + ROW_COLUMN( table, row ) + " + \" " + row.fst;

			if ( row.lst != null )
			{
				statement += " " + row.lst;
			}

			if ( fieldsiter.hasNext() || !table.constraints.isEmpty() )
			{
				statement += ",\" + \n";
			}
		}

		Iterator<Pair<String, String>> constraintiter = table.constraints.iterator();

		while ( constraintiter.hasNext() )
		{
			Pair<String, String> constraint = constraintiter.next();
			statement += "\t\t\t \"CONSTRAINT " + constraint.fst + " " + constraint.snd;
			if ( constraintiter.hasNext() )
			{
				statement += ",\" + \n";
			}
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
		type = Util.sanitize( type, false ).toLowerCase();
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
		if ( type.equals( "boolean" ) )
		{
			return "boolean";
		}
		if ( type.equals( "string" ) )
		{
			return "text";
		}
		if ( type.equals( "null" ) )
		{
			return "null";
		}
		if ( type.equals( "integer" ) )
		{
			return "integer";
		}
		if ( type.equals( "real" ) )
		{
			return "real";
		}
		if ( type.equals( "text" ) )
		{
			return "text";
		}
		// special case
		if ( type.equals( "autoincrement" ) )
		{
			return "integer";
		}
		// fallback to blob
		return "blob";
	}
}
