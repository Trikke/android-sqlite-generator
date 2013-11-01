package com.trikke.util;

import com.squareup.javawriter.JavaWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 16/10/13
 * Time: 20:32
 */
public class Util
{
	private static final Charset UTF_8 = Charset.forName( "UTF8" );

	public static JavaWriter getJavaWriter( String javaOut, String classPackage, String className ) throws IOException
	{
		String directory = javaOut + "/" + classPackage.replace( ".", "/" );
		boolean created = new File( directory ).mkdirs();
		if ( created )
		{
			System.out.println( "Created output directory " + directory );
		}

		String fileName = directory + "/" + className + ".java";
		System.out.println( "Writing generated code to " + fileName );
		return new JavaWriter( new OutputStreamWriter( new FileOutputStream( fileName ), UTF_8 ) );
	}

	public static String capitalize( String line )
	{
		return Character.toUpperCase( line.charAt( 0 ) ) + line.substring( 1 ).toLowerCase();
	}

	public static String sanitize( String string, boolean allowWhitespace )
	{
		if ( allowWhitespace )
		{
			return string.replaceAll( "[^A-Za-z0-9_ ]", "" );
		}

		return string.replaceAll( "[^A-Za-z0-9_]", "" );
	}

	public static String clean( String string )
	{
		return string.replaceAll( "\\s+", "" );
	}

	public static String concat( List<String> strings, String separator )
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for ( String s : strings )
		{
			sb.append( sep ).append( s );
			sep = separator;
		}
		return sb.toString();
	}

	public static String[] splitParts( String string )
	{
		String[] parts = string.split( "\\t" );
		List<String> dirty = new ArrayList<String>( Arrays.asList( parts ) );
		List<String> clean = new ArrayList<String>();
		for ( String part : dirty )
		{
			if ( part != null && !part.equals( "" ) )
			{
				clean.add( Util.clean( part ) );
			}
		}

		return clean.toArray( new String[clean.size()] );
	}
}
