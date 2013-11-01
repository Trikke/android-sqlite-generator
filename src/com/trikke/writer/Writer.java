package com.trikke.writer;

import com.squareup.javawriter.JavaWriter;
import com.trikke.data.Model;
import com.trikke.util.Util;

import java.io.File;
import java.io.IOException;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 22:18
 */
public abstract class Writer
{
	protected final JavaWriter writer;
	private final File directory;
	private final Model mModel;

	public Writer( String directory, Model model, String className ) throws IOException
	{
		mModel = model;
		this.directory = new File( directory );
		this.writer = Util.getJavaWriter( directory, mModel.getClassPackage(), className );
	}

	public abstract void compile() throws Exception;
}
