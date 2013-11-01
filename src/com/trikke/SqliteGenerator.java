package com.trikke;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class SqliteGenerator
{
	private static final String VERSION = "1.0";

	private static final String DESCRIBE_PATH_FLAG = "--describe=";
	private static final String JAVA_OUT_FLAG = "--java_out=";

	private static final String DB_NAME = "NAME";
	private static final String DB_VERSION = "VERSION";
	private static final String DB_CONTENTPROVIDER = "CONTENTPROVIDERNAME";
	private static final String PACKAGE = "PACKAGE";
	private static final String AUTHORITY = "AUTHORITY";

	private static final String TABLE_TAG = "TABLE";
	private static final String TABLE_END_TAG = "ENDTABLE";

	private static final String VIEW_TAG = "VIEW";
	private static final String VIEW_END_TAG = "ENDVIEW";

	private static final String UNIQUE_TAG = "UNIQUE=";
	private static final String TYPE_TAG = "TYPE=";

	private final String describeFile;
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
		this.describeFile = describeFile;
		this.mModel = new Model();
	}

	public void compile( String javaOut ) throws Exception
	{
		System.out.println("-------------------------------");
		System.out.println(" Android Sqlite Generator v" + VERSION);
		System.out.println("-------------------------------");

		parseDbDetails();
		parseTables();
		parseViews();

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
			System.out.println("Don't forget to add the following to your AndroidManifest.xml under the <application> tag.");
			System.out.println();
			System.out.println("<provider android:name=\""+mModel.getClassPackage()+"."+mModel.getDbClassName()+"\" android:authorities=\""+mModel.getContentAuthority()+"\">");
			System.out.println();
		}
	}

	private void parseDbDetails() throws IOException
	{
		File file = new File( describeFile );

		String line;
		Scanner in = new Scanner( file );
		while ( in.hasNextLine() )
		{
			line = in.nextLine();
			if ( !line.startsWith( "//" ) )
			{
				String[] parts = Util.splitParts( line );
				if ( line.startsWith( PACKAGE ) )
				{
					mModel.setClassPackage( parts[1] );
					System.out.println( "Package Found > " + mModel.getClassPackage() );
				}

				if ( line.startsWith( AUTHORITY ) )
				{
					mModel.setContentAuthority( parts[1] );
					System.out.println( "Authority Found > " + mModel.getContentAuthority() );
				}

				if ( line.startsWith( DB_NAME ) )
				{
					mModel.setDbName( parts[1] );
					System.out.println( "Name Found > " + mModel.getDbName() );
				}

				if ( line.startsWith( DB_VERSION ) )
				{
					mModel.setDbVersion( parts[1] );
					System.out.println( "version Found > " + mModel.getDbVersion() );
				}

				if ( line.startsWith( DB_CONTENTPROVIDER ) )
				{
					mModel.setContentProviderName( parts[1] );
					System.out.println( "provider Found > " + mModel.getContentProviderName() );
				}
			}
		}

		if ( mModel.getClassPackage() == null )
		{
			System.err.println( "Must specify " + PACKAGE + " in your describing file" );
			System.exit( 1 );
		}

		if ( mModel.getContentAuthority() == null )
		{
			System.err.println( "Must specify " + AUTHORITY + " in your describing file" );
			System.exit( 1 );
		}

		if ( mModel.getDbName() == null )
		{
			System.err.println( "Must specify " + DB_NAME + " in your describing file" );
			System.exit( 1 );
		}

		if ( mModel.getDbVersion() == null )
		{
			System.err.println( "Must specify " + DB_VERSION + " in your describing file" );
			System.exit( 1 );
		}

		if ( mModel.getContentProviderName() == null )
		{
			System.err.println( "Must specify " + DB_CONTENTPROVIDER + " in your describing file" );
			System.exit( 1 );
		}
	}

	private void parseTables() throws IOException
	{
		File file = new File( describeFile );

		String line;
		List<String> tablelines = null;
		Scanner in = new Scanner( file );
		while ( in.hasNextLine() )
		{
			line = in.nextLine();
			if ( !line.startsWith( "//" ) )
			{
				if ( line.startsWith( TABLE_TAG ) )
				{
					tablelines = new ArrayList<String>();
				}

				if ( tablelines != null )
				{
					tablelines.add( line );
				}

				if ( line.startsWith( TABLE_END_TAG ) )
				{
					if ( tablelines != null )
					{
						parseTable( tablelines );
					} else
					{
						System.err.println( "Found a " + TABLE_END_TAG + " before a " + TABLE_TAG );
						System.exit( 1 );
						break;
					}

					tablelines = null;
				}
			}
		}
	}

	private void parseTable( List<String> tablelines )
	{
		Table table = new Table();
		for ( String line : tablelines )
		{
			if ( line.startsWith( TABLE_END_TAG ) )
			{
				break;
			}

			if ( line.startsWith( TABLE_TAG ) )
			{
				setTableProperties( table, line );
			} else
			{
				String[] parts = Util.splitParts( line );
				// normal fields here
				table.addField( parts[0], parts[parts.length - 1] );
			}
		}
		mModel.addTable( table );
	}

	private void parseViews() throws IOException
	{
		File file = new File( describeFile );

		String line;
		List<String> viewlines = null;
		Scanner in = new Scanner( file );
		while ( in.hasNextLine() )
		{
			line = in.nextLine();
			if ( !line.startsWith( "//" ) )
			{
				if ( line.startsWith( VIEW_TAG ) )
				{
					viewlines = new ArrayList<String>();
				}

				if ( viewlines != null )
				{
					viewlines.add( line );
				}

				if ( line.startsWith( VIEW_END_TAG ) )
				{
					if ( viewlines != null )
					{
						parseView( viewlines );
					} else
					{
						System.err.println( "Found a " + VIEW_END_TAG + " before a " + VIEW_TAG );
						System.exit( 1 );
						break;
					}

					viewlines = null;
				}
			}
		}
	}

	private void parseView( List<String> viewlines )
	{
		View view = new View();
		for ( String line : viewlines )
		{
			if ( line.startsWith( VIEW_END_TAG ) )
			{
				break;
			}
			if ( line.startsWith( VIEW_TAG ) )
			{
				setViewProperties( view, line );
			} else
			{
				// normal fields here
				view.addField( Util.splitParts( line ) );
			}
		}
		mModel.addView( view );
	}

	private void setViewProperties( View view, String line )
	{
		int start = VIEW_TAG.length() + 1;
		int next = line.indexOf( "\t", start );
		if ( next < 0 ) next = line.length();

		view.name = line.substring( start, next ).toUpperCase();

		if ( line.contains( TYPE_TAG ) )
		{
			start = line.indexOf( TYPE_TAG ) + TYPE_TAG.length();
			view.jointype = Util.sanitize( line.substring( start ), true );
		}
	}

	private void setTableProperties( Table table, String line )
	{
		int start = TABLE_TAG.length() + 1;
		int next = line.indexOf( "\t", start );
		if ( next < 0 ) next = line.length();

		table.name = line.substring( start, next ).toUpperCase();

		if ( line.contains( UNIQUE_TAG ) )
		{
			start = line.indexOf( UNIQUE_TAG ) + UNIQUE_TAG.length();
			table.uniqueKey = Util.sanitize( line.substring( start ), false );
		}
	}
}
