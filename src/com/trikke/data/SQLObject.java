package com.trikke.data;

import java.util.ArrayList;

/**
 * Created by the awesome :
 * User: trikke
 * Date: 29/10/13
 * Time: 09:22
 */
public abstract class SQLObject
{
	public String name;
	public ArrayList<Pair<String, String>> fields = new ArrayList<Pair<String, String>>();
}
