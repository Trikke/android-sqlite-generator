package com.trikke.writer;

import com.trikke.data.*;
import com.trikke.util.SqlUtil;
import com.trikke.util.Util;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.EnumSet;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 20:25
 */
public class ContractWriter extends Writer
{

	private final Model mModel;

	public ContractWriter( String javaOut, Model model ) throws IOException
	{
		super( javaOut, model, model.getContractName() );
		this.mModel = model;
	}

	public void compile() throws IOException
	{
		writer.emitPackage( mModel.getClassPackage() );

		emitClass();
		emitTableConstants();

		writer.endType();

		writer.close();
	}

	private void emitClass() throws IOException
	{
		writer.beginType( mModel.getContractName(), "class", EnumSet.of( Modifier.PUBLIC, Modifier.FINAL ) );
	}

	private void emitTableConstants() throws IOException
	{
		for ( Table table : mModel.getTables() )
		{
			writer.emitJavadoc( "TABLE \n" + table.name + " constants " );
			writer.emitField( "String", SqlUtil.IDENTIFIER( table ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + table.name + "\"" );

			int index = 0;

			// default _ID field
			Field defaultrow = Table.getDefaultAndroidIdField();
			writer.emitField( "String", SqlUtil.ROW_COLUMN( table, defaultrow ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + defaultrow.name + "\"" );
			writer.emitField( "int", SqlUtil.ROW_COLUMN_POSITION( table, defaultrow ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
			index++;

			for ( Field row : table.fields )
			{
				writer.emitField( "String", SqlUtil.ROW_COLUMN( table, row ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + row.name + "\"" );
				writer.emitField( "int", SqlUtil.ROW_COLUMN_POSITION( table, row ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
				index++;
			}
			writer.emitEmptyLine();
		}

		for ( View view : mModel.getViews() )
		{
			writer.emitJavadoc( "VIEW \n" + view.name + " constants " );
			int index = 0;
			for ( Pair<String, String> select : view.fields )
			{
				writer.emitField( "String", SqlUtil.ROW_COLUMN( view, (select.snd == null) ? select.fst : select.snd ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "\"" + Util.sanitize( select.snd, false ) + "\"" );
				writer.emitField( "int", SqlUtil.ROW_COLUMN_POSITION( view, (select.snd == null) ? select.fst : select.snd ), EnumSet.of( Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL ), "" + index );
				index++;
			}
			writer.emitEmptyLine();
		}
	}
}
