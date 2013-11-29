package com.trikke.data;

public class Triple<T, U, V>
{
	public final T fst;
	public final U snd;
	public final V lst;

	Triple( T fst, U snd, V lst )
	{
		this.fst = fst;
		this.snd = snd;
		this.lst = lst;
	}

	public static <T, U, V> Triple<T, U, V> of( T t, U u, V v )
	{
		return new Triple<T, U, V>( t, u, v );
	}

	public String toString()
	{
		return "Triple[" + fst + "," + snd + "," + lst + "]";
	}
}