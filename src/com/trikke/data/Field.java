package com.trikke.data;

import java.util.List;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 04/12/13
 * Time: 15:52
 */
public class Field extends SQLObject
{
	public final String type;
	public final List<Constraint> constraints;

	public Field( String type, String name, List<Constraint> constraints )
	{
		this.type = type;
		this.name = name;
		this.constraints = constraints;
	}
}
