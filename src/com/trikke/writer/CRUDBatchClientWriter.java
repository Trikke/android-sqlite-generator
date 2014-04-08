package com.trikke.writer;

import com.trikke.data.Constraint;
import com.trikke.data.Field;
import com.trikke.data.Model;
import com.trikke.data.Table;
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
public class CRUDBatchClientWriter extends Writer
{

	private final Model mModel;

	public CRUDBatchClientWriter( String directory, Model model ) throws IOException
	{
		super( directory, model, model.getCRUDBatchClientName() );
		this.mModel = model;
	}

	@Override
	public void compile() throws Exception
	{
		writer.emitPackage( mModel.getClassPackage() );

		emitImports();
		emitClass();
		emitFields();
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
		writer.beginType( mModel.getClassPackage() + "." + mModel.getCRUDBatchClientName(), "class", EnumSet.of( Modifier.PUBLIC, Modifier.FINAL ) );
	}

	private void emitMethods() throws Exception
	{
		emitContentProviderOperationMethods();

		for ( Table table : mModel.getTables() )
		{
			emitTableCRUD( table );
		}
	}

	private void emitFields() throws IOException
	{
		writer.emitEmptyLine();
		writer.emitField( "ArrayList<ContentProviderOperation>", "batchOperations", EnumSet.of( Modifier.PRIVATE ) );
		writer.emitEmptyLine();
	}

	private void emitContentProviderOperationMethods() throws IOException
	{
		ArrayList<String> throwing = new ArrayList<String>();
		throwing.add( "OperationApplicationException" );
		throwing.add( "RemoteException" );

		ArrayList<String> batchparams = new ArrayList<String>();
		batchparams.add( "Context" );
		batchparams.add( "c" );

		writer.emitEmptyLine();
		writer.emitJavadoc( "Starts a new batch of operations to run on commit." );
		writer.beginMethod( "void", "start", EnumSet.of( Modifier.PUBLIC ) );
		writer.emitStatement( "batchOperations = new ArrayList<ContentProviderOperation>()" );
		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitJavadoc( "Commits a started batch of operations, this can include any variety of operations." );
		writer.beginMethod( "ContentProviderResult[]", "commit", EnumSet.of( Modifier.PUBLIC ), batchparams, throwing );
		writer.beginControlFlow( "if (batchOperations != null) " );
		writer.emitStatement( "ContentResolver cr = c.getContentResolver()" );
		writer.emitStatement( "ContentProviderResult[] result = cr.applyBatch( " + mModel.getContentProviderName() + ".AUTHORITY, batchOperations )" );
		writer.emitStatement( "batchOperations.clear()" );
		writer.emitStatement( "return result" );
		writer.nextControlFlow( "else" );
		writer.emitStatement( "throw new RuntimeException(\"" + mModel.getCRUDBatchClientName() + ".start() needs to be called first!\")" );
		writer.endControlFlow();
		writer.endMethod();

		writer.emitEmptyLine();
		writer.emitJavadoc( "add an operation to the batch of operations, if this client was started." );
		writer.beginMethod( "void", "add", EnumSet.of( Modifier.PUBLIC ), "ContentProviderOperation", "operation" );
		writer.beginControlFlow( "if (batchOperations != null) " );
		writer.emitStatement( "batchOperations.add(operation)" );
		writer.nextControlFlow( "else" );
		writer.emitStatement( "throw new RuntimeException(\"" + mModel.getCRUDBatchClientName() + ".start() needs to be called first!\")" );
		writer.endControlFlow();
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

		ArrayList<String> paramsWithUnique = new ArrayList<String>();
		paramsWithUnique.add( SqlUtil.getJavaTypeFor( table.getPrimaryKey().type ) );
		paramsWithUnique.add( table.getPrimaryKey().name );

		ArrayList<String> updateParams = new ArrayList<String>();
		// TODO : add unique id
		updateParams.addAll( params );

		Iterator<Constraint> constraintiter;

		writer.emitEmptyLine();
		writer.emitJavadoc( table.name + " OPERATIONS\nall operations require this client to first run start" );
		writer.emitEmptyLine();

		// Add through ContentProviderOperation
		writer.beginMethod( "void", "add" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC ), params.toArray( new String[params.size()] ) );
		writer.emitStatement( "ContentProviderOperation.Builder operationBuilder = ContentProviderOperation.newInsert(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ")" );
		for ( Field row : table.fields )
		{
			writer.emitStatement( "operationBuilder.withValue(" + mModel.getContractName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.name + ")" );
		}
		insertAddOpBlock();
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
		writer.beginMethod( "void", "removeAll" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC ) );
		writer.emitStatement( "ContentProviderOperation.Builder operationBuilder = ContentProviderOperation.newDelete(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ")" );
		insertAddOpBlock();
		writer.endMethod();

		// Update through ContentProviderOperation
		writer.emitEmptyLine();
		writer.beginMethod( "void", "update" + Util.capitalize( table.name ), EnumSet.of( Modifier.PUBLIC ), updateParams.toArray( new String[updateParams.size()] ) );
		writer.emitStatement( "ContentProviderOperation.Builder operationBuilder = ContentProviderOperation.newUpdate(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ")" );
		for ( Field row : table.fields )
		{
			writer.emitStatement( "operationBuilder.withValue(" + mModel.getContractName() + "." + SqlUtil.ROW_COLUMN( table, row ) + "," + row.name + ")" );
		}
		insertAddOpBlock();
		writer.endMethod();
	}

	private void insertAddOpBlock() throws IOException
	{
		writer.beginControlFlow( "if (batchOperations != null)" );
		writer.emitStatement( "batchOperations.add(operationBuilder.build())" );
		writer.nextControlFlow( "else" );
		writer.emitStatement( "throw new RuntimeException(\"" + mModel.getCRUDBatchClientName() + ".start() needs to be called first!\")" );
		writer.endControlFlow();
	}

	private void writeRemoveWith( Table table, Field field ) throws IOException
	{
		ArrayList<String> params = new ArrayList<String>();
		params.add( SqlUtil.getJavaTypeFor( field.type ) );
		params.add( field.name );

		// remove with UNIQUE
		writer.emitEmptyLine();
		writer.beginMethod( "void", "remove" + Util.capitalize( table.name ) + "With" + Util.capitalize( field.name ), EnumSet.of( Modifier.PUBLIC ), params.toArray( new String[params.size()] ) );
		writer.emitStatement( "ContentProviderOperation.Builder operationBuilder = ContentProviderOperation.newDelete(" + mModel.getContentProviderName() + "." + SqlUtil.URI( table ) + ")" );
		writer.emitStatement( "operationBuilder.withSelection(" + mModel.getContractName() + "." + SqlUtil.ROW_COLUMN( table, field ) + " + \"=?\", new String[]{String.valueOf(" + field.name + ")})" );
		insertAddOpBlock();
		writer.endMethod();
	}

}
