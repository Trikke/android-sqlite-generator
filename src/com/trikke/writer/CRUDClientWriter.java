package com.trikke.writer;

import com.trikke.data.*;
import com.trikke.util.SqlUtil;
import com.trikke.util.Util;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

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
		for ( Field row : table.fields )
		{
			params.add( SqlUtil.getJavaTypeFor( row.type ) );
			params.add( row.name );
		}

		ArrayList<String> paramsJustContext = new ArrayList<String>();
		paramsJustContext.add( "Context" );
		paramsJustContext.add( "c" );

		ArrayList<String> paramsWithContext = new ArrayList<String>();
		paramsWithContext.addAll( paramsJustContext );
		paramsWithContext.addAll( params );

		ArrayList<String> updateParams = new ArrayList<String>();
		updateParams.add( "Context" );
		updateParams.add( "c" );
		updateParams.add( SqlUtil.getJavaTypeFor( table.getPrimaryKey().type ) );
		updateParams.add( table.getPrimaryKey().name );
		updateParams.addAll( params );


		writer.emitEmptyLine();
		writer.emitJavadoc( table.name + " OPERATIONS" );
		writer.emitEmptyLine();

		Iterator<Constraint> constraintiter;

		// Gets
		writeGetWith( table, table.getPrimaryKey() );

		constraintiter = table.constraints.iterator();

		while ( constraintiter.hasNext() )
		{
			Constraint constraint = constraintiter.next();
			if ( constraint.type.equals( Constraint.Type.UNIQUE ) )
			{
				final String[] fields = SqlUtil.getFieldsFromConstraint( constraint );
				for ( int i = 0; i < fields.length; i++ )
				{
					writeGetWith( table, table.getFieldByName( fields[i] ) );
				}
			}
		}

		// Normal Add
		writer.emitEmptyLine();
		writer.beginMethod( "Uri", "add" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), paramsWithContext.toArray( new String[paramsWithContext.size()] ) );
		writer.emitStatement( "ContentValues contentValues = new ContentValues()" );
		for ( Field row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.name + ")" );
		}
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.insert(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", contentValues)" );
		writer.endMethod();

		// Removes
		writeRemoveWith( table, table.getPrimaryKey() );

		constraintiter = table.constraints.iterator();

		while ( constraintiter.hasNext() )
		{
			Constraint constraint = constraintiter.next();
			if ( constraint.type.equals( Constraint.Type.UNIQUE ) )
			{
				final String[] fields = SqlUtil.getFieldsFromConstraint( constraint );
				for ( int i = 0; i < fields.length; i++ )
				{
					writeRemoveWith( table, table.getFieldByName( fields[i] ) );
				}
			}
		}

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
		for ( Field row : table.fields )
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
		for ( Field row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.name + ")" );
		}
		writer.emitStatement( "Uri rowURI = ContentUris.withAppendedId(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + "," + table.getPrimaryKey().name + ")" );
		writer.emitStatement( "String where = null" );
		writer.emitStatement( "String whereArgs[] = null" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.update(rowURI, contentValues, where, whereArgs)" );
		writer.endMethod();

		// Update where
		writeUpdateWhere( table, table.getPrimaryKey() );

		constraintiter = table.constraints.iterator();

		while ( constraintiter.hasNext() )
		{
			Constraint constraint = constraintiter.next();
			if ( constraint.type.equals( Constraint.Type.UNIQUE ) )
			{
				final String[] fields = SqlUtil.getFieldsFromConstraint( constraint );
				for ( int i = 0; i < fields.length; i++ )
				{
					writeUpdateWhere( table, table.getFieldByName( fields[i] ) );
				}
			}
		}
	}


	private void writeGetWith( Table table, Field field ) throws IOException
	{
		ArrayList<String> params = new ArrayList<String>();
		params.add( "Context" );
		params.add( "c" );
		params.add( SqlUtil.getJavaTypeFor( field.type ) );
		params.add( field.name );

		writer.emitEmptyLine();
		writer.beginMethod( "Cursor", "get" + Util.capitalize( table.name ) + "With" + Util.capitalize( field.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), params.toArray( new String[params.size()] ) );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.query(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", null, " + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, field ) + " + \"=?\", new String[]{String.valueOf(" + field.name + ")},null)" );
		writer.endMethod();
	}

	private void writeRemoveWith( Table table, Field field ) throws IOException
	{
		ArrayList<String> params = new ArrayList<String>();
		params.add( "Context" );
		params.add( "c" );
		params.add( SqlUtil.getJavaTypeFor( field.type ) );
		params.add( field.name );

		// Normal remove with primary
		writer.emitEmptyLine();
		writer.beginMethod( "int", "remove" + Util.capitalize( table.name ) + "With" + Util.capitalize( field.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), params.toArray( new String[params.size()] ) );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.delete(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ", " + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, field ) + " + \"=?\", new String[]{String.valueOf(" + field.name + ")})" );
		writer.endMethod();
	}

	private void writeUpdateWhere( Table table, Field field ) throws IOException
	{
		ArrayList<String> params = new ArrayList<String>();
		params.add( "Context" );
		params.add( "c" );
		params.add( SqlUtil.getJavaTypeFor( field.type ) );
		params.add( field.name );

		// Default array params for all rows
		for ( Field row : table.fields )
		{
			if ( row.equals( field ) )
			{
				continue;
			}

			params.add( SqlUtil.getJavaTypeFor( row.type ) );
			params.add( row.name );
		}

		// Update with where clause
		writer.emitEmptyLine();
		writer.beginMethod( "int", "update" + Util.capitalize( table.name ) + "Where" + Util.capitalize( field.name ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC ), params.toArray( new String[params.size()] ) );
		writer.emitStatement( "ContentValues contentValues = new ContentValues()" );
		for ( Field row : table.fields )
		{
			writer.emitStatement( "contentValues.put(" + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.name + ")" );
		}
		writer.emitStatement( "Uri rowURI = " + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) );
		writer.emitStatement( "String where = " + mModel.getDbClassName() + "." + SqlUtil.ROW_COLUMN( table, field ) + " + \"=?\"" );
		writer.emitStatement( "String whereArgs[] = new String[]{String.valueOf(" + field.name + ")}" );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "return cr.update(rowURI, contentValues, where, whereArgs)" );
		writer.endMethod();
	}
}
