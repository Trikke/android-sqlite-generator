package com.trikke;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.trikke.data.*;
import com.trikke.exception.ParserException;
import com.trikke.util.Util;
import com.trikke.writer.CRUDBatchClientWriter;
import com.trikke.writer.CRUDClientWriter;
import com.trikke.writer.ContentProviderWriter;
import com.trikke.writer.DatabaseWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SqliteGenerator
{
	private static final String VERSION = "2.0";

	private static final String DESCRIBE_PATH_FLAG = "--describe=";
	private static final String CONFIG_PATH_FLAG = "--config=";
	private static final String JAVA_OUT_FLAG = "--java_out=";

	private String describePath;
	private String configFile;
	private Model mModel;

	public static void main( String[] args ) throws Exception
	{
		String describePath = null;
		String configPath = null;
		String javaOut = null;

		int index = 0;
		while ( index < args.length )
		{
			if ( args[index].startsWith( DESCRIBE_PATH_FLAG ) )
			{
				describePath = args[index].substring( DESCRIBE_PATH_FLAG.length() );
			} else if ( args[index].startsWith( JAVA_OUT_FLAG ) )
			{
				javaOut = args[index].substring( JAVA_OUT_FLAG.length() );
			} else if ( args[index].startsWith( CONFIG_PATH_FLAG ) )
			{
				configPath = args[index].substring( CONFIG_PATH_FLAG.length() );
			}
			index++;
		}

		if ( describePath == null )
		{
			System.err.println( "Must specify " + DESCRIBE_PATH_FLAG + " flag" );
			System.exit( 1 );
		}

		if ( configPath == null )
		{
			System.err.println( "Must specify " + CONFIG_PATH_FLAG + " flag" );
			System.exit( 1 );
		}

		if ( javaOut == null )
		{
			System.err.println( "Must specify " + JAVA_OUT_FLAG + " flag" );
			System.exit( 1 );
		}

		SqliteGenerator wireCompiler = new SqliteGenerator( describePath, configPath );
		wireCompiler.compile( javaOut );
	}

	public SqliteGenerator( String describePath, String configPath )
	{
		this.mModel = new Model();
		this.describePath = describePath;
		this.configFile = configPath;
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
			System.out.println( "Using config : " + new File( configFile ).getName() );

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
		} catch ( com.eclipsesource.json.ParseException pex )
		{
			System.err.println( "Couldn't parse the config json file. Make sure it is valid json." );
			System.exit( 1 );
		} catch ( NullPointerException npex )
		{
			System.err.println( "A required value in the config file is missing." );
			System.exit( 1 );
		}
	}

	private void parse()
	{
		parseObjectsForFolder( new File( describePath ) );
	}

	public void parseObjectsForFolder( final File folder )
	{
		for ( final File fileEntry : folder.listFiles() )
		{
			if ( fileEntry.isDirectory() )
			{
				parseObjectsForFolder( fileEntry );
			} else
			{
				if ( !fileEntry.getPath().equals( configFile ) )
				{
					parseObjectFromFile( fileEntry );
				}
			}
		}
	}

	private void parseObjectFromFile( final File file )
	{
		int pos = file.getName().lastIndexOf( "." );
		String name = pos > 0 ? file.getName().substring( 0, pos ) : file.getName();

		try
		{
			JsonObject json = Util.getJsonFromFile( file.getPath() );

			boolean containsFields = json.names().contains( "fields" );
			boolean containsSelects = json.names().contains( "selects" );

			if ( containsFields )
			{
				parseTableFromFile( json, name );
			} else if ( containsSelects )
			{
				parseViewFromFile( json, name );
			} else
			{
				System.err.println( "The object " + name + " contains no valid data. I can not guess if this is a table or a view." );
				System.exit( 1 );
			}
		} catch ( ParserException pex )
		{
			System.err.println( "The object " + name + " is not the parsable structure i expect it to be. Reason : " + pex.getMessage() );
			System.exit( 1 );
		} catch ( UnsupportedOperationException uoex )
		{
			System.err.println( "The object " + name + " is not the parsable structure i expect it to be. Reason : " + uoex.getMessage() );
			System.exit( 1 );
		} catch ( Exception ex )
		{
			ex.printStackTrace();
			System.err.println( "The object " + name + " is not the parsable structure i expect it to be, please make sure it's valid json, and see formatting guidelines." );
			System.exit( 1 );
		}
	}

	private void parseTableFromFile( final JsonObject jsontable, final String name ) throws ParserException
	{
		System.out.println( "found table > " + name );
		boolean containsFields = jsontable.names().contains( "fields" );
		if ( !containsFields )
		{
			System.err.println( "This table contains no fields." );
			System.exit( 1 );
		} else
		{
			Table table = new Table();
			table.name = name.toLowerCase();
			try
			{
				for ( JsonValue jsoninfo : jsontable.get( "fields" ).asArray() )
				{
					JsonObject info = (JsonObject) jsoninfo;
					String type = info.get( "type" ).asString().toLowerCase();
					if ( type.equals( "autoincrement" ) )
					{
						table.setPrimaryKey( type, info.get( "name" ).asString() );
					}

					ArrayList<Constraint> constraints = new ArrayList<Constraint>();
					if ( info.names().contains( "constraints" ) )
					{
						for ( JsonValue jsonconstaint : info.get( "constraints" ).asArray() )
						{
							JsonObject constraint = (JsonObject) jsonconstaint;
							String definition = constraint.get( "definition" ).asString();
							if ( definition.toLowerCase().contains( "primary key" ) )
							{
								// skip if primary key already set
								if ( table.hasPrimaryKey() )
								{
									continue;
								}

								final Field field = table.getFieldByName( constraint.get( "name" ).asString() );
								if ( field != null )
								{
									table.setPrimaryKey( field );
								}
							}
							constraints.add( new Constraint( constraint.names().contains( "name" ) ? constraint.get( "name" ).asString() : null, definition ) );
						}
					}
					table.addField( type, info.get( "name" ).asString(), constraints );
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the fields in this table." );
			}
			if ( jsontable.names().contains( "constraints" ) )
			{
				try
				{
					for ( JsonValue jsoninfo : jsontable.get( "constraints" ).asArray() )
					{
						JsonObject info = (JsonObject) jsoninfo;
						String definition = info.get( "definition" ).asString();
						if ( definition.toLowerCase().contains( "primary key" ) )
						{
							// skip if primary key already set
							if ( table.hasPrimaryKey() )
							{
								continue;
							}

							final Field field = table.getFieldByName( info.get( "name" ).asString() );
							if ( field != null )
							{
								table.setPrimaryKey( field );
							}
						}
						table.addConstraint( info.names().contains( "name" ) ? info.get( "name" ).asString() : null, definition );
					}
				} catch ( NullPointerException e )
				{
					throw new ParserException( "Couldn't parse the constraints in this table." );
				}
			}
			mModel.addTable( table );
		}
	}

	private void parseViewFromFile( final JsonObject jsonview, final String name ) throws ParserException
	{
		System.out.println( "found view > " + name );
		boolean containsFields = jsonview.names().contains( "selects" );
		if ( !containsFields )
		{
			System.err.println( "This view contains no selects." );
			System.exit( 1 );
		} else
		{
			View view = new View();
			view.name = name.toLowerCase();
			try
			{
				for ( JsonValue jsoninfo : jsonview.get( "selects" ).asArray() )
				{
					JsonObject info = (JsonObject) jsoninfo;
					view.addSelect( (info.names().contains( "as" )) ? info.get( "as" ).asString() : null, info.get( "select" ).asString() );
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the selects in this view." );
			}

			try
			{
				for ( JsonValue jsoninfo : jsonview.get( "from" ).asArray() )
				{
					view.addFromTable( jsoninfo.asString() );
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the from tables in this view." );
			}

			try
			{
				for ( JsonValue jsoninfo : jsonview.get( "on" ).asArray() )
				{
					view.addJoinOn( jsoninfo.asString() );
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the fields on which to join in this view." );
			}
			try
			{
				if ( jsonview.names().contains( "order" ) )
				{
					for ( JsonValue jsoninfo : jsonview.get( "order" ).asArray() )
					{
						JsonObject info = (JsonObject) jsoninfo;
						view.addOrder( info.get( "by" ).asString(), (info.names().contains( "sort" )) ? info.get( "sort" ).asString() : "ASC" );
					}
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the ordering in this view." );
			}
			try
			{
				if ( jsonview.names().contains( "group" ) )
				{
					for ( JsonValue jsoninfo : jsonview.get( "group" ).asArray() )
					{
						view.addGroup( jsoninfo.asString() );
					}
				}
			} catch ( NullPointerException e )
			{
				throw new ParserException( "Couldn't parse the grouping in this view." );
			}
			if ( jsonview.names().contains( "join" ) )
			{
				view.jointype = jsonview.get( "join" ).asString();
			}
			mModel.addView( view );
		}
	}
}
