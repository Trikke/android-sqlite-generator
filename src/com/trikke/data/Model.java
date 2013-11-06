package com.trikke.data;

import java.util.ArrayList;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 17/10/13
 * Time: 21:21
 */
public class Model
{
	private static final ArrayList<String> validStrategies = new ArrayList<String>(  );

	private String classPackage;
	private String contentAuthority;
	private String dbName;
	private String dbVersion;
	private String contentProviderName;
	private String conflictStrategy = "REPLACE";
	private ArrayList<Table> tables = new ArrayList<Table>();
	private ArrayList<View> views = new ArrayList<View>();

	public Model()
	{
		validStrategies.add( "ROLLBACK" );
		validStrategies.add( "ABORT" );
		validStrategies.add( "FAIL" );
		validStrategies.add( "IGNORE" );
		validStrategies.add( "REPLACE" );
	}

	public void addTable( Table table )
	{
		tables.add( table );
	}

	public void addView( View view )
	{
		views.add( view );
	}

	public String getDbClassName()
	{
		return dbName + "DB";
	}

	public String getCRUDClientName()
	{
		return contentProviderName + "Client";
	}

	public String getCRUDBatchClientName()
	{
		return contentProviderName + "BatchClient";
	}

	public String getDbName()
	{
		return dbName;
	}

	public void setDbName( String dbName )
	{
		this.dbName = dbName;
	}

	public String getContentAuthority()
	{
		return contentAuthority;
	}

	public void setContentAuthority( String contentAuthority )
	{
		this.contentAuthority = contentAuthority;
	}

	public String getDbVersion()
	{
		return dbVersion;
	}

	public void setDbVersion( String dbVersion )
	{
		this.dbVersion = dbVersion;
	}

	public String getContentProviderName()
	{
		return contentProviderName;
	}

	public void setContentProviderName( String contentProviderName )
	{
		this.contentProviderName = contentProviderName;
	}

	public String getClassPackage()
	{
		return classPackage;
	}

	public void setClassPackage( String classPackage )
	{
		this.classPackage = classPackage;
	}

	public ArrayList<Table> getTables()
	{
		return tables;
	}

	public void setTables( ArrayList<Table> tables )
	{
		this.tables = tables;
	}

	public ArrayList<View> getViews()
	{
		return views;
	}

	public void setViews( ArrayList<View> views )
	{
		this.views = views;
	}

	public String getConflictStrategy()
	{
		return conflictStrategy;
	}

	public void setConflictStrategy( String conflictStrategy )
	{
		if (validStrategies.contains( conflictStrategy.toUpperCase() ))
		{
			this.conflictStrategy = conflictStrategy.toUpperCase();
		}
		else
		{
			throw new RuntimeException( conflictStrategy.toUpperCase() + " is not a valid conflict strategy." );
		}
	}
}
