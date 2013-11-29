package com.trikke;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.trikke.data.Model;
import com.trikke.data.Table;
import com.trikke.data.View;
import com.trikke.util.Util;
import com.trikke.writer.CRUDBatchClientWriter;
import com.trikke.writer.CRUDClientWriter;
import com.trikke.writer.ContentProviderWriter;
import com.trikke.writer.DatabaseWriter;

import java.io.File;
import java.io.IOException;

public class SqliteGenerator
{
	private static final String VERSION = "1.2";

	private static final String DESCRIBE_PATH_FLAG = "--describe=";
	private static final String JAVA_OUT_FLAG = "--java_out=";

	private static final String DB_NAME = "NAME";
	private static final String DB_VERSION = "VERSION";
	private static final String DB_CONTENTPROVIDER = "CONTENTPROVIDERNAME";
	private static final String PACKAGE = "PACKAGE";
	private static final String AUTHORITY = "AUTHORITY";
	private static final String CONFLICTSTRATEGY = "CONFLICTSTRATEGY";

	private static final String TABLE_TAG = "TABLE";
	private static final String TABLE_END_TAG = "ENDTABLE";

	private static final String VIEW_TAG = "VIEW";
	private static final String VIEW_END_TAG = "ENDVIEW";

	private static final String UNIQUE_TAG = "UNIQUE=";
	private static final String TYPE_TAG = "TYPE=";

	private static final String CONFIG_TAG = "TYPE=";

	private String filepath = "assets/describe/";
	private String configFile = "assets/describe/config.json";
	private Model mModel;

	public static void main( String[] args ) throws Exception
	{
		String describeFile = null;
		String javaOut = null;

		int index = 0;
		while ( index < args.length )
		{
			if ( args[index].startsWith( DESCRIBE_PATH_FLAG ) )
			{
				describeFile = args[index].substring( DESCRIBE_PATH_FLAG.length() );
			} else if ( args[index].startsWith( JAVA_OUT_FLAG ) )
			{
				javaOut = args[index].substring( JAVA_OUT_FLAG.length() );
			}
			index++;
		}

		if ( describeFile == null )
		{
			System.err.println( "Must specify " + DESCRIBE_PATH_FLAG + " flag" );
			System.exit( 1 );
		}

		if ( javaOut == null )
		{
			System.err.println( "Must specify " + JAVA_OUT_FLAG + " flag" );
			System.exit( 1 );
		}

		SqliteGenerator wireCompiler = new SqliteGenerator( describeFile );
		wireCompiler.compile( javaOut );
	}

	public SqliteGenerator( String describeFile )
	{
		this.mModel = new Model();
	}

	public void compile( String javaOut ) throws Exception
	{
		System.out.println( "-------------------------------" );
		System.out.println( " Android Sqlite Generator v" + VERSION );
		System.out.println( "-------------------------------" );

		System.out.println();

		parseConfig();
		parse();

		System.out.println();

		if ( !mModel.getTables().isEmpty() )
		{
			DatabaseWriter dbwriter = new DatabaseWriter( javaOut, mModel );
			dbwriter.compile();

			ContentProviderWriter cpwriter = new ContentProviderWriter( javaOut, mModel );
			cpwriter.compile();

			CRUDClientWriter crudClientWriter = new CRUDClientWriter( javaOut, mModel );
			crudClientWriter.compile();

			CRUDBatchClientWriter crudBatchClientWriter = new CRUDBatchClientWriter( javaOut, mModel );
			crudBatchClientWriter.compile();

			System.out.println();
			System.out.println( "Don't forget to add the following to your AndroidManifest.xml under the <application> tag." );
			System.out.println();
			System.out.println( "<provider android:name=\"" + mModel.getClassPackage() + "." + mModel.getDbClassName() + "\" android:authorities=\"" + mModel.getContentAuthority() + "\">" );
			System.out.println();
		}
	}

	private void parseConfig()
	{
		try
		{
			JsonObject config = Util.getJsonFromFile( configFile );

			mModel.setClassPackage( config.get( "package" ).asString() );
			mModel.setContentAuthority( config.get( "authority" ).asString() );
			mModel.setDbName( config.get( "databaseName" ).asString() );
			mModel.setDbVersion( config.get( "databaseVersion" ).asInt() );
			mModel.setContentProviderName( config.get( "contentproviderName" ).asString() );

		} catch ( IOException ex )
		{
			System.err.println( ex.getMessage() );
			System.exit( 1 );
		}
		catch ( com.eclipsesource.json.ParseException pex )
		{
			System.err.println( "Couldn't parse the config json file. Make sure it is valid json." );
			System.exit( 1 );
		}
		catch ( NullPointerException npex )
		{
			System.err.println( "A required value in the config file is missing." );
			System.exit( 1 );
		}
	}

	private void parse()
	{
		parseObjectsForFolder( new File( filepath ) );
	}

	public void parseObjectsForFolder( final File folder ) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				parseObjectsForFolder( fileEntry );
			} else {
				if (!fileEntry.getPath().equals( configFile ))
				{
					parseObjectFromFile( fileEntry );
				}
			}
		}
	}

	private void parseObjectFromFile(final File file)
	{
		int pos = file.getName().lastIndexOf( "." );
		String name = pos > 0 ? file.getName().substring( 0, pos ) : file.getName();

		try
		{
			JsonObject json = Util.getJsonFromFile( file.getPath() );


			boolean containsFields = json.names().contains( "fields" );
			boolean containsSelects = json.names().contains( "selects" );

			if (containsFields)
			{
				parseTableFromFile( json, name );
			}
			else if (containsSelects)
			{
				parseViewFromFile( json, name );
			}
			else
			{
				System.err.println( "The object "+name+" contains no valid data. I can not guess if this is a table or a view." );
				System.exit( 1 );
			}

		} catch ( IOException e )
		{
			System.err.println( e.getMessage() );
			System.exit( 1 );
		} catch ( NullPointerException npe )
		{
			System.err.println( "The object "+name+" is not the parsable structure i expect it to be, please see formatting guidelines." );
			System.exit( 1 );
		}
	}

	private void parseTableFromFile(final JsonObject jsontable, final String name)
	{
		System.out.println("found table > "+name);
		boolean containsFields = jsontable.names().contains( "fields" );
		if (!containsFields)
		{
			System.err.println( "This table contains no fields." );
			System.exit( 1 );
		}
		else
		{
			Table table = new Table();
			table.name = name;
			table.hasPrimaryKey( false );
			for (JsonValue jsoninfo : jsontable.get( "fields" ).asArray())
			{
				JsonObject info = (JsonObject) jsoninfo;
				String type = info.get( "type" ).asString();
				if (type.toLowerCase().equals( "autoincrement" ))
					table.hasPrimaryKey( true );

				table.addField( type, info.get( "name" ).asString() );
			}
			if (jsontable.names().contains( "constraints" ))
			{
				for (JsonValue jsoninfo : jsontable.get( "constraints" ).asArray())
				{
					JsonObject info = (JsonObject) jsoninfo;
					String definition = info.get( "definition" ).asString();
					if (definition.toLowerCase().contains( "primary key" ))
					{
						// skip if primary key already set
						if (table.hasPrimaryKey())
							continue;

						table.hasPrimaryKey( true );
					}
					table.addConstraint( info.get( "name" ).asString(), definition );
				}
			}
			mModel.addTable( table );
		}
	}

	private void parseViewFromFile(final JsonObject jsonview, final String name)
	{
		System.out.println("found view > "+name);
		boolean containsFields = jsonview.names().contains( "selects" );
		if (!containsFields)
		{
			System.err.println( "This view contains no selects." );
			System.exit( 1 );
		}
		else
		{
			View view = new View();
			view.name = name;
			for (JsonValue jsoninfo : jsonview.get( "selects" ).asArray())
			{
				JsonObject info = (JsonObject) jsoninfo;
				view.addSelect( (info.names().contains( "name" )) ? info.get( "name" ).asString() : null, info.get( "from" ).asString() );
			}
			for (JsonValue jsoninfo : jsonview.get( "from" ).asArray())
			{
				view.addFromTable( jsoninfo.asString() );
			}
			for (JsonValue jsoninfo : jsonview.get( "on" ).asArray())
			{
				view.addJoinOn( jsoninfo.asString() );
			}
			for (JsonValue jsoninfo : jsonview.get( "order" ).asArray())
			{
				JsonObject info = (JsonObject) jsoninfo;
				view.addOrder( info.get( "by" ).asString(), (info.names().contains( "sort" )) ? info.get( "sort" ).asString() : "ASC" );
			}
			for (JsonValue jsoninfo : jsonview.get( "group" ).asArray())
			{
				view.addGroup( jsoninfo.asString() );
			}
			if (jsonview.names().contains( "join" ))
			{
				view.jointype = jsonview.get( "join" ).asString();
			}
			mModel.addView( view );
		}
	}
}
