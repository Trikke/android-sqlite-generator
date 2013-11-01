package com.trikke.writer;

import com.trikke.data.Model;
import com.trikke.data.Table;
import com.trikke.data.View;
import com.trikke.util.SqlUtil;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 22:18
 */
public class ContentProviderWriter extends Writer
{
	private final Model mModel;

	public ContentProviderWriter( String javaOut, Model model ) throws IOException
	{
		super( javaOut, model, model.getContentProviderName() );
		this.mModel = model;
	}

	@Override
	public void compile() throws IOException
	{
		writer.emitPackage( mModel.getClassPackage() );

		emitImports();
		emitClass();
		emitFields();
		emitTableConstants();
		emitURIs();
		emitURIMatcher();

		emitMethods();

		writer.endType();

		writer.close();
	}

	private void emitImports() throws IOException
	{
		writer.emitImports( "java.util.Map","java.util.ArrayList", "android.text.TextUtils", "android.content.ContentProvider", "android.content.ContentUris", "android.content.ContentValues", "android.content.UriMatcher", "android.database.Cursor", "android.database.sqlite.SQLiteConstraintException", "android.database.sqlite.SQLiteDatabase", "android.database.sqlite.SQLiteException", "android.database.sqlite.SQLiteQueryBuilder","android.database.sqlite.SQLiteStatement", "android.net.Uri", "android.text.TextUtils", "android.util.Log" );
		writer.emitEmptyLine();
	}

	private void emitClass() throws IOException
	{
		writer.beginType( mModel.getClassPackage() + "." + mModel.getContentProviderName() + " extends ContentProvider", "class", EnumSet.of( Modifier.PUBLIC, Modifier.FINAL ) );
	}

	private void emitFields() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitField( "String", "TAG", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + mModel.getContentProviderName() + "\"" );
		writer.emitField( "String", "DATABASE_NAME", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + mModel.getDbClassName() + ".db\"" );
		writer.emitField( "int", "DATABASE_VERSION", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), mModel.getDbVersion() );
		writer.emitField( "String", "ROW_ID", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ), "\"" + Table.ANDROID_ID + "\"" );
		writer.emitEmptyLine();
		writer.emitField( "String", "AUTHORITY", EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + mModel.getContentAuthority() + "\"" );
		writer.emitEmptyLine();
		writer.emitField( mModel.getDbClassName(), "mLocalDatabase", EnumSet.of( Modifier.PRIVATE ) );
		writer.emitEmptyLine();
	}

	private void emitTableConstants() throws IOException
	{
		int index = 1;
		for ( Table table : mModel.getTables() )
		{
			writer.emitSingleLineComment( table.name + " constants" );
			writer.emitField( "String", SqlUtil.IDENTIFIER( table ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + table.name + "\"" );
			writer.emitField( "int", table.getAllName(), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
			index++;
			writer.emitField( "int", table.getSingleName(), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
			index++;
		}
		writer.emitEmptyLine();
		writer.emitSingleLineComment( "views constants" );
		for ( View view : mModel.getViews() )
		{
			writer.emitField( "String", view.name, EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + view.name + "\"" );
			writer.emitField( "int", SqlUtil.IDENTIFIER( view ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
			index++;
		}
	}

	private void emitURIs() throws IOException
	{
		writer.emitEmptyLine();
		for ( Table table : mModel.getTables() )
		{
			writer.emitField( "Uri", SqlUtil.URI( table ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "Uri.parse(\"content://" + mModel.getContentAuthority() + "/" + table.name.toLowerCase() + "\")" );
		}
		for ( View view : mModel.getViews() )
		{
			writer.emitField( "Uri", SqlUtil.URI( view ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "Uri.parse(\"content://" + mModel.getContentAuthority() + "/" + view.name.toLowerCase() + "\")" );
		}
		writer.emitEmptyLine();
	}

	private void emitURIMatcher() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitField( "UriMatcher", "uriMatcher", EnumSet.of( Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL ) );
		writer.beginInitializer( true );
		writer.emitStatement( "uriMatcher = new UriMatcher(UriMatcher.NO_MATCH)" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "uriMatcher.addURI(\"" + mModel.getContentAuthority() + "\", \"" + table.name.toLowerCase() + "\", " + table.getAllName() + ")" );
			writer.emitStatement( "uriMatcher.addURI(\"" + mModel.getContentAuthority() + "\", \"" + table.name.toLowerCase() + "/#\", " + table.getSingleName() + ")" );
		}

		for ( View view : mModel.getViews() )
		{
			writer.emitStatement( "uriMatcher.addURI(\"" + mModel.getContentAuthority() + "\", \"" + view.name.toLowerCase() + "\", " + SqlUtil.IDENTIFIER( view ) + ")" );
		}

		writer.endInitializer();
	}

	private void emitMethods() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "boolean", "onCreate", EnumSet.of( Modifier.PUBLIC ) );
		writer.emitStatement( "mLocalDatabase = new " + mModel.getDbClassName() + " (getContext())" );
		writer.emitStatement( "return true" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "String", "getTableNameFromUri", EnumSet.of( Modifier.PRIVATE ), "Uri", "uri" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getAllName() + ":\ncase " + table.getSingleName() + ":\n\treturn " + SqlUtil.IDENTIFIER( table ) );
		}

		for ( View view : mModel.getViews() )
		{
			writer.emitStatement( "\tcase " + SqlUtil.IDENTIFIER( view ) + ":\n\treturn " + view.name );
		}


		writer.emitStatement( "default: break" );
		writer.endControlFlow();
		writer.emitEmptyLine();
		writer.emitStatement( "return null" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "String", "getUniqueKey", EnumSet.of( Modifier.PRIVATE ), "Uri", "uri" );
		writer.emitSingleLineComment( "Only for actual tables" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getAllName() + ":\ncase " + table.getSingleName() + ":\n\treturn \"" + table.UNIQUEROWID().snd + "\"");
		}

		writer.emitStatement( "default: break" );
		writer.endControlFlow();
		writer.emitEmptyLine();
		writer.emitStatement( "return null" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "boolean", "containsUnique", EnumSet.of( Modifier.PRIVATE ), "Uri", "uri", "ContentValues", "contentvalues" );
		writer.emitStatement( "String unique = getUniqueKey(uri)" );
		writer.beginControlFlow( "for (String key : contentvalues.keySet())" );
		writer.beginControlFlow( "if (key.equals(unique))" );
		writer.emitStatement( "return true" );
		writer.endControlFlow();
		writer.endControlFlow();
		writer.emitStatement( "return false" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "Uri", "getContentUriFromUri", EnumSet.of( Modifier.PRIVATE ), "Uri", "uri" );
		writer.emitSingleLineComment( "Only for actual tables" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getAllName() + ":\ncase " + table.getSingleName() + ":\n\treturn " + SqlUtil.URI( table ) );
		}

		writer.emitStatement( "default: break" );
		writer.endControlFlow();
		writer.emitEmptyLine();
		writer.emitStatement( "return null" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.beginMethod( "ArrayList<Uri>", "getAssociatedViewUris", EnumSet.of( Modifier.PRIVATE ), "Uri", "uri" );
		writer.emitSingleLineComment( "Only for actual views" );
		writer.emitStatement( "ArrayList<Uri> viewUris = null" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );
		for ( Table table : mModel.getTables() )
		{
			String statement = "\tcase " + table.getAllName() + ":\ncase " + table.getSingleName() + ":\n\t";
			ArrayList<String> associatedviews = new ArrayList<String>();
			for ( View view : mModel.getViews() )
			{
				if ( view.getFromtables().contains( table.name ) )
				{
					associatedviews.add( SqlUtil.URI( view ) );
				}
			}

			if ( !associatedviews.isEmpty() )
			{
				statement += "viewUris = new ArrayList<Uri>();\n\t";
			}

			for ( String string : associatedviews )
			{
				statement += "viewUris.add(" + string + ");\n\t";
			}

			statement += "break";

			writer.emitStatement( statement );
		}

		writer.emitStatement( "default: break" );
		writer.endControlFlow();
		writer.emitEmptyLine();
		writer.emitStatement( "return viewUris" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "String", "getType", EnumSet.of( Modifier.PUBLIC ), "Uri", "uri" );
		writer.emitSingleLineComment( "Return a string that identifies the MIME type for a Content Provider URI" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getAllName() + ":\n\treturn \"vnd.android.cursor.dir/vnd." + mModel.getClassPackage() + "." + table.name.toLowerCase() + "\"" );
			writer.emitStatement( "\tcase " + table.getSingleName() + ":\n\treturn \"vnd.android.cursor.dir/vnd." + mModel.getClassPackage() + "." + table.name.toLowerCase() + "\"" );
		}

		for ( View view : mModel.getViews() )
		{
			writer.emitStatement( "\tcase " + SqlUtil.IDENTIFIER( view ) + ":\n\treturn \"vnd.android.cursor.dir/vnd." + mModel.getClassPackage() + "." + view.name.toLowerCase() + "\"" );
		}

		writer.emitStatement( "default:\n throw new IllegalArgumentException(\"Unsupported URI: \" + uri)" );
		writer.endControlFlow();
		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "Cursor", "query", EnumSet.of( Modifier.PUBLIC ), "Uri", "uri", "String[]", "projection", "String", "selection", "String[]", "selectionArgs", "String", "sortOrder" );
		writer.emitSingleLineComment( "Open database" );
		writer.emitStatement( "SQLiteDatabase db" );
		writer.beginControlFlow( "try" );
		writer.emitStatement( "db = mLocalDatabase.getWritableDatabase()" );
		writer.nextControlFlow( "catch (SQLiteException ex)" );
		writer.emitStatement( "db = mLocalDatabase.getReadableDatabase()" );
		writer.endControlFlow();

		writer.emitSingleLineComment( "Replace these with valid SQL statements if necessary." );
		writer.emitStatement( "String groupBy = null" );
		writer.emitStatement( "String having = null" );
		writer.emitStatement( "SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder()" );

		writer.emitSingleLineComment( "If this is a row query, limit the result set to the passed in row." );

		writer.emitStatement( "String rowID" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "case " + table.getSingleName() + ":\n\trowID = uri.getPathSegments().get(1);\n\tqueryBuilder.appendWhere(ROW_ID + \"=\" + rowID);\n\tbreak" );
		}

		writer.emitStatement( "default: break" );
		writer.endControlFlow();

		writer.emitSingleLineComment( "Specify the table on which to perform the query. This can be a specific table or a join as required." );
		writer.emitStatement( "queryBuilder.setTables(getTableNameFromUri(uri))" );

		writer.emitEmptyLine();
		writer.emitSingleLineComment( "Execute..." );
		writer.emitStatement( "Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, groupBy, having, sortOrder)" );
		writer.emitStatement( "cursor.setNotificationUri(getContext().getContentResolver(), uri)" );
		writer.emitStatement( "return cursor" );

		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "int", "delete", EnumSet.of( Modifier.PUBLIC ), "Uri", "uri", "String", "selection", "String[]", "selectionArgs" );
		writer.emitSingleLineComment( "Open database" );
		writer.emitStatement( "SQLiteDatabase db = mLocalDatabase.getWritableDatabase()" );

		writer.emitStatement( "String rowID" );
		writer.emitStatement( "String UNIQUEID" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getSingleName() + ":" + "\n\tUNIQUEID = \"" + table.UNIQUEROWID().snd + "\";\n\trowID = uri.getPathSegments().get(1);\n\tselection = UNIQUEID + \"=\" + rowID + (!TextUtils.isEmpty(selection) ? \" AND (\" + selection + ')' : \"\")" );
		}

		writer.emitStatement( "default: break" );
		writer.endControlFlow();

		writer.emitEmptyLine();
		writer.beginControlFlow( "if (selection == null)" );
		writer.emitStatement( "selection = \"1\"" );
		writer.endControlFlow();
		writer.emitEmptyLine();
		writer.emitStatement( "int deleteCount = db.delete(getTableNameFromUri(uri), selection, selectionArgs)" );
		insertNotifyBlock();
		writer.emitEmptyLine();
		writer.emitStatement( "return deleteCount" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "Uri", "insert", EnumSet.of( Modifier.PUBLIC ), "Uri", "uri", "ContentValues", "values" );
		writer.emitSingleLineComment( "Open database" );
		writer.emitStatement( "SQLiteDatabase db = mLocalDatabase.getWritableDatabase()" );
		writer.emitStatement( "String nullColumnHack = null" );
		writer.emitStatement( "long id = db.insert(getTableNameFromUri(uri), nullColumnHack, values)" );
		writer.emitEmptyLine();
		writer.beginControlFlow( "if (id > -1)" );
		writer.emitSingleLineComment( "the insert was successful" );
		writer.emitStatement( "Uri insertedId = ContentUris.withAppendedId(getContentUriFromUri(uri), id)" );
		insertNotifyBlock();
		writer.emitEmptyLine();
		writer.emitStatement( "return insertedId" );
		writer.endControlFlow();
		writer.emitStatement( "return null" );
		writer.endMethod();


		writer.emitEmptyLine();
		writer.emitAnnotation( "Override" );
		writer.beginMethod( "int", "update", EnumSet.of( Modifier.PUBLIC ), "Uri", "uri", "ContentValues", "values", "String", "selection", "String[]", "selectionArgs" );
		writer.emitSingleLineComment( "Open database" );
		writer.emitStatement( "SQLiteDatabase db = mLocalDatabase.getWritableDatabase()" );
		writer.emitSingleLineComment( "If this is a row URI, limit the deletion to the specified row." );

		writer.emitStatement( "String rowID" );
		writer.emitStatement( "String UNIQUEID" );
		writer.beginControlFlow( "switch(uriMatcher.match(uri))" );

		for ( Table table : mModel.getTables() )
		{
			writer.emitStatement( "\tcase " + table.getSingleName() + ":" + "\n\tUNIQUEID = \"" + table.UNIQUEROWID().snd + "\";\n\trowID = uri.getPathSegments().get(1);\n\tselection = UNIQUEID + \"=\" + rowID + (!TextUtils.isEmpty(selection) ? \" AND (\" + selection + ')' : \"\")" );
		}
		writer.emitStatement( "default: break" );
		writer.endControlFlow();

		writer.emitSingleLineComment( "Perform update" );
		writer.emitStatement( "int updateCount = db.update(getTableNameFromUri(uri), values, selection, selectionArgs)" );
		insertNotifyBlock();
		writer.emitEmptyLine();
		writer.emitStatement( "return updateCount" );
		writer.endMethod();
	}


	private void insertNotifyBlock() throws IOException
	{
		writer.emitStatement( "getContext().getContentResolver().notifyChange(uri, null)" );
		writer.emitEmptyLine();
		writer.emitSingleLineComment( "For non-query statements, we also check if we need to notify any view urls. If we update/insert/remove something from a table used by a view, the view must know." );
		writer.emitStatement( "ArrayList<Uri> viewUris = getAssociatedViewUris(uri)" );
		writer.beginControlFlow( "if (viewUris != null)" );
		writer.beginControlFlow( "for (Uri viewUri : viewUris)" );
		writer.emitStatement( "getContext().getContentResolver().notifyChange(viewUri, null)" );
		writer.endControlFlow();
		writer.endControlFlow();
	}
}
