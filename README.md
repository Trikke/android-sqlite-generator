android-sqlite-generator
========================



# What

This compiler will generate you db class, content provider and several helper class for you from a describing file. You can set several options like package name, database name, database version, content authority and others.

By default, the following files will be generated.
#####[name]DB.java
- This is the main database. It extends `SQLiteOpenHelper` and contains all necessary code for maintaining the database and column names and positions.

#####[contentprovidername].java
- This class is the content provider to use. It's your run of the mill android `ContentProvider`.

#####[contentprovidername]Client.java
- The client provides easy access to CRUD operations on any described table.

#####[contentprovidername]BatchClient.java
- The batch client works the same as above, but is used for batched `ContentProviderOperation`. It contains methods to easily start a batch of `ContentProviderOperation` and commit it with the correct authority.

# How

	java -jar <generator>.jar --describe=<describe file> --java_out=<relative path to put files>


Run the above command. There are only 2 parameters.

- **describe** : path to the text file describing the DB
- **java_out** : path to put generated files. Please note that packagename will be used to generate subfolders

Don't forget to add the content provider to the AndroidManifest.

```xml
<provider android:name="PACKAGE.NAMEDB" android:authorities="AUTHORITY">
```

# Download

You can find releases here on [GitHub][3].

[3]: https://github.com/Trikke/android-sqlite-generator/releases

# The describe file

The describe file is a text file in which you can describe your tables and view, together with unique keys, groups, sorts and orders.

```

//Database is described here, this will auto-generate a db and necessary provider
//remember, everything is tabbed, no spaces!

PACKAGE			com.example.sql		// the package in which to save everything.
AUTHORITY		com.example.dbprovider	// the authority to use
NAME			Example		// basic name to use
VERSION			1	// the current version of the DB, bump this when changes are made below
CONTENTPROVIDERNAME	ExampleContentProvider	// name of the content provider

//Main tables

// Every table is defined between TABLE and ENDTABLE
// you can define a unique key with (UNIQUE=xxx)
TABLE BaseUser	(UNIQUE=userid)
int			userid
String		name
int			age
boolean		isonline
ENDTABLE

TABLE Avatar	(UNIQUE=userid)
int			userid
String		fullUrl
String		thumbUrl
ENDTABLE

//Views

// Every view is defined between VIEW and ENDVIEW
// you can define join type with (TYPE=xxx)
// only "select" and "from" are mandatory. Other configurations translate directly to their SQL counterparts.
// a select is defined in 2 or 3 columns with "select" "tablename.columnname" "AS name"
// join values from tables on the same column in all tables
// order by is defined in 2 or 3 columns with "order" "tablename.columnname" "ASC/DESC"
// grouping is defined in 2 columns with "group" "tablename.columnname"
VIEW User	(TYPE=INNER JOIN)
select		BaseUser.userid		_id
select		BaseUser.userid
select		BaseUser.name
select		BaseUser.gender
select		Avatar.fullUrl
from		BaseUser, Avatar
on			userid
order		BaseUser.name
order		BaseUser.userid	ASC
group		BaseUser.userid
ENDVIEW
```

# Know issues / limitations

###issues
- only one unique key can be set
- default conflict strategy is `ON CONFLICT REPLACE`

###supported data types
```
- Float
- Double
- Long
- Integer
- Boolean
- float
- double
- long
- int
- boolean
```

# Upcoming features
- joining on different fields
- multiple unqiue keys
- foreign keys
- "upsert"

# acknowledgements

Thanks in part to [Square][1], for their excellent [Javawriter] [2]

[1]:http://square.github.io/
[2]:https://github.com/square/javawriter
