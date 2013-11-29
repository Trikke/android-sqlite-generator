package com.trikke.writer;

import com.trikke.data.Model;
import com.trikke.data.Pair;
import com.trikke.data.Table;
import com.trikke.data.View;
import com.trikke.util.SqlUtil;
import com.trikke.util.Util;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 17/10/13
 * Time: 16:22
 */
public class CRUDClientWriter extends Writer
{

	private final Model mModel;

	public CRUDClientWriter( String directory, Model model ) throws IOException
	{
		super( directory, model, model.getCRUDClientName() );
		this.mModel = model;
	}

	@Override
	public void compile() throws Exception
	{
		writer.emitPackage( mModel.getClassPackage() );

		emitImports();
		emitClass();
		emitMethods();
		writer.endType();

		writer.close();
	}

	private void emitImports() throws IOException
	{
		writer.emitImports( "android.content.*", "android.database.Cursor", "android.net.Uri", "java.util.ArrayList", "android.os.RemoteException" );
		writer.emitEmptyLine();
	}

	private void emitClass() throws IOException
	{
		writer.beginType( mModel.getClassPackage() + "." + mModel.getCRUDClientName(), "class", EnumSet.of( Modifier.PUBLIC, Modifier.FINAL ) );
	}

	private void emitMethods() throws Exception
	{
		for ( Table table : mModel.getTables() )
		{
			emitTableCRUD( table );
		}

		writer.emitEmptyLine();
		writer.emitJavadoc( "Simple get operations for Views" );

		for ( View view : mModel.getViews() )
		{
			emitViewCRUD( view );
		}
	}

	private void emitViewCRUD( View view ) throws Exception
	{
		ArrayList<String> paramsWhere = new ArrayList<String>();
		paramsWhere.add( "Context" );
		paramsWhere.add( "c" );
		paramsWhere.add( "String" );
		paramsWhere.add( "rowname" );
		paramsWhere.add( "Object" );
		paramsWhere.add( "queryvalue" );

		// Normal Get with UNIQUE
		writer.emitEmptyLine();
		writer.beginMethod( "Cursor", "get" + Util.capitalize( view.name ) + "With", EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), paramsWhere.toArray( new String[paramsWhere.size()] ) );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.query(" + mModel.getContentProviderName() + "." + SqlUtil.URI( view ) + ", null, rowname + \"=?\", new String[]{String.valueOf(queryvalue)},null)" );
		writer.endMethod();
	}

	private void emitTableCRUD( Table table ) throws Exception
	{
		// Default array params for all rows
		ArrayList<String> params = new ArrayList<String>();
		for ( Pair<String, String> row : table.fields )
		{

			params.add( row.fst );
			params.add( row.snd );
		}

		ArrayList<String> paramsWithContext = new ArrayList<String>();
		paramsWithContext.add( "Context" );
		paramsWithContext.add( "c" );
		paramsWithContext.addAll( params );

		ArrayList<String> paramsWithUnique = new ArrayList<String>();
		paramsWithUnique.add( "Context" );
		paramsWithUnique.add( "c" );
		paramsWithUnique.add( table.UNIQUEROWID().fst );
		paramsWithUnique.add( table.UNIQUEROWID().snd );

		ArrayList<String> updateParams = new ArrayList<String>();
		updateParams.add( "Context" );
		updateParams.add( "c" );
		// TODO : add unique id
		updateParams.addAll( params );


		ArrayList<String> updateWhereParams = new ArrayList<String>();
		updateWhereParams.add( "Context" );
		updateWhereParams.add( "c" );
		updateWhereParams.add( "String" );
		updateWhereParams.add( "rowname" );
		updateWhereParams.add( "Object" );
		updateWhereParams.add( "updatevalue" );
		updateWhereParams.addAll( params );

		writer.emitEmptyLine();
		writer.emitJavadoc( table.name + " OPERATIONS" );
		writer.emitEmptyLine();

		// Normal Get with UNIQUE
		writer.beginMethod( "Cursor", "get" + Util.capitalize( table.name ) + "With" + Util.capitalize( table.UNIQUEROWID().snd ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), paramsWithUnique.toArray( new String[paramsWithUnique.size()] ) );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.query(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", null, \"" + table.UNIQUEROWID().snd + "=?\", new String[]{String.valueOf(" + table.UNIQUEROWID().snd + ")},null)" );
		writer.endMethod();

		// Normal Add
		writer.emitEmptyLine();
		writer.beginMethod( "Uri", "add" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), paramsWithContext.toArray( new String[paramsWithContext.size()] ) );
		writer.emitStatement( "ContentValues contentValues = new ContentValues()" );
		for ( Pair<String, String> row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.snd + ")" );
		}
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.insert(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", contentValues)" );
		writer.endMethod();

		// Normal remove with UNIQUE
		writer.emitEmptyLine();
		writer.beginMethod( "int", "remove" + Util.capitalize( table.name ) + "With" + Util.capitalize( table.UNIQUEROWID().snd ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), paramsWithUnique.toArray( new String[paramsWithUnique.size()] ) );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.delete(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", \"" + table.UNIQUEROWID().snd + "=?\", new String[]{String.valueOf(" + table.UNIQUEROWID().snd + ")})" );
		writer.endMethod();

		// Remove All results
		writer.emitEmptyLine();
		writer.beginMethod( "int", "removeAll" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), "Context", "c" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.delete(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", null, null)" );
		writer.endMethod();

		// Get All results
		writer.emitEmptyLine();
		writer.beginMethod( "Cursor", "getAll" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), "Context", "c" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );

		String arrays = "";
		for ( Pair<String, String> row : table.fields )
		{
			arrays += mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + ",\n";
		}
		writer.emitStatement( "String[] result_columns = new String[]{\n" + arrays + "}" );
		writer.emitStatement( "String where = null" );
		writer.emitStatement( "String whereArgs[] = null" );
		writer.emitStatement( "String order = null" );
		writer.emitStatement( "Cursor resultCursor = cr.query(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", result_columns, where, whereArgs, order)" );
		writer.emitStatement( "return resultCursor" );
		writer.endMethod();

		// Normal Update
		writer.emitEmptyLine();
		writer.beginMethod( "int", "update" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), updateParams.toArray( new String[updateParams.size()] ) );
		writer.emitStatement( "ContentValues contentValues = new ContentValues()" );
		for ( Pair<String, String> row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.snd + ")" );
		}
		writer.emitStatement( "Uri rowURI = ContentUris.withAppendedId(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + "," + table.UNIQUEROWID().snd + ")" );
		writer.emitStatement( "String where = null" );
		writer.emitStatement( "String whereArgs[] = null" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.update(rowURI, contentValues, where, whereArgs)" );
		writer.endMethod();

		/*
		// Update with where clause
		writer.emitEmptyLine();
		writer.beginMethod( "int", "update" + Util.capitalize( table.name ) + "Where", EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), updateWhereParams.toArray( new String[updateWhereParams.size()] ) );
		writer.emitStatement( "ContentValues contentValues = new ContentValues()" );
		for ( Pair<String, String> row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.snd + ")" );
		}
		writer.emitStatement( "Uri rowURI = " + mModel.getContentProviderName() + "." + SqlUtil.URI( table ));
		writer.emitStatement( "String where = \"rowname=?\"" );
		writer.emitStatement( "String whereArgs[] = new String[]{String.valueOf(updatevalue)}" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.update(rowURI, contentValues, where, whereArgs)" );
		writer.endMethod();
		*/
	}

}
