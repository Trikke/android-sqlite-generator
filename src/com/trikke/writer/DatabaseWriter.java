package com.trikke.writer;

import com.trikke.data.Model;
import com.trikke.data.Table;
import com.trikke.data.View;
import com.trikke.util.SqlUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 20:25
 */
public class DatabaseWriter extends Writer
{

	private final Model mModel;

	public DatabaseWriter( String javaOut, Model model ) throws IOException
	{
		super( javaOut, model, model.getDbClassName() );
		this.mModel = model;
	}

	public void compile() throws IOException
	{
		writer.emitPackage( mModel.getClassPackage() );

		emitImports();
		emitClass();
		emitFields();

		emitTableCreateSQL();
		emitViewCreateSQL();

		emitConstructor();
		emitMethods();

		writer.endType();

		writer.close();
	}

	private void emitImports() throws IOException
	{
		writer.emitImports( "android.util.Log", "android.content.Context", "android.database.sqlite.SQLiteDatabase", "android.database.sqlite.SQLiteOpenHelper" );
		writer.emitEmptyLine();
	}

	private void emitClass() throws IOException
	{
		writer.beginType( mModel.getDbClassName() + " extends SQLiteOpenHelper", "class", EnumSet.of( Modifier.PUBLIC, Modifier.FINAL ) );
	}

	private void emitFields() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitField( "String", "TAG", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + mModel.getDbClassName() + "\"" );
		writer.emitField( "String", "DATABASE_NAME", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + mModel.getDbClassName() + ".db\"" );
		writer.emitField( "int", "DATABASE_VERSION", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), String.valueOf( mModel.getDbVersion() ) );
		writer.emitEmptyLine();
	}

	private void emitConstructor() throws IOException
	{
		writer.emitEmptyLine();
		writer.beginMethod( "", mModel.getDbClassName(), EnumSet.of( Modifier.PUBLIC ), "Context", "context" );
		writer.emitStatement( "super( context, DATABASE_NAME, null, DATABASE_VERSION )" );
		writer.endMethod();
		writer.emitEmptyLine();
	}

	private void emitTableCreateSQL() throws IOException
	{
		for ( Table table : mModel.getTables() )
		{
			writer.emitSingleLineComment( table.name + " create statement" );
			writer.emitField( "String", "DATABASE_" + table.name.toUpperCase() + "_CREATE", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + SqlUtil.generateCreateStatement( mModel, table ) + "\"" );
			writer.emitEmptyLine();
		}
	}

	private void emitViewCreateSQL() throws IOException
	{
		for ( View view : mModel.getViews() )
		{
			writer.emitSingleLineComment( view.name + " view create statement" );
			writer.emitField( "String", "VIEW_" + view.name.toUpperCase() + "_CREATE", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + SqlUtil.generateCreateStatement( view ) + "\"" );
			writer.emitEmptyLine();
		}
	}

	private void emitMethods() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitSingleLineComment( "Called when no database exists on disk we need to create a new one." );
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "void", "onCreate", EnumSet.of( Modifier.PUBLIC ), "SQLiteDatabase", "db" );
		writer.emitStatement( "Log.d(TAG, \"Creating a new Database. Current version \" + DATABASE_VERSION)" );
		writer.emitEmptyLine();
		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "db.execSQL(" + "DATABASE_" + table.name.toUpperCase() + "_CREATE" + ")" );
		}
		for ( View view : mModel.getViews() )
		{
			writer.emitStatement( "db.execSQL(" + "VIEW_" + view.name.toUpperCase() + "_CREATE" + ")" );
		}

		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitSingleLineComment( "Called when there is a database version mismatch meaning that the version of the database on disk needs to be upgraded to the current version." );
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "void", "onUpgrade", EnumSet.of( Modifier.PUBLIC ), "SQLiteDatabase", "db", "int", "oldVersion", "int", "newVersion" );
		writer.emitStatement( "Log.w(TAG, \"Upgrading from version \" + oldVersion + \" to \" + newVersion + \", which will destroy all old data\")" );

		writer.emitEmptyLine();
		writer.emitSingleLineComment( "The simplest case is to drop the old database and create a new one." );
		writer.emitStatement( "clearDB(db)" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "void", "clearDB", EnumSet.of( Modifier.PUBLIC ), "SQLiteDatabase", "db" );
		writer.emitStatement( "Log.w(TAG, \"Clearing local database...\")" );
		writer.emitEmptyLine();
		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "db.execSQL(\"DROP TABLE IF EXISTS " + table.name + ";\")" );
		}
		for ( View view : mModel.getViews() )
		{
			writer.emitStatement( "db.execSQL(\"DROP VIEW IF EXISTS " + view.name + ";\")" );
		}
		writer.emitStatement( "onCreate( db )" );
		writer.endMethod();
	}
}
