android-sqlite-generator
========================

# What

This compiler will generate you db class, content provider and several helper class for you from a describing file. You can set several options like package name, database name, database version, content authority and others.

# Download

You can find releases here on [GitHub][3].

# How to run

	java -jar <generator>.jar --in=<folder of json files> --config=<path to config json> --out=<output folder>


Run the above command. There are only 2 parameters.

- **in** : folder which contains json files that describe the database
- **out** : path to put generated files. Please note that packagename will be used to generate subfolders
- **config** : a json file which holds the configuration of the database. It can reside in the specified "**in**" folder.

Don't forget to add the content provider to the AndroidManifest.

```xml
<provider android:name="PACKAGE.CONTENT_PROVIDER_NAME" android:authorities="AUTHORITY">
```

### Gradle configuration

On Android Studio, you can use Gradle to generate files. Just put downloaded sqlitegenerator.jar on your app/libs folder and create a new task on build.gradle:

```groovy

android {

    // Here are your default android configs
    // (...)

    sourceSets {
        main {
            java.srcDir("src-gen/main/java/db")
        }
    }
}

task buildDb(type: Exec) {
    def config = "src/main/db-gen/db.json"
    def definitions = "src/main/db-gen/tables"
    def out = "src-gen/main/java/db"

    commandLine 'java', '-jar', 'libs/sqlitegenerator.jar', '--in=' + definitions   \
    , '--config=' + config, '--out=' + out
}


task cleanDb() {
    delete "src-gen/main/java/db/"
}

clean.dependsOn cleanDb
preBuild.dependsOn buildDb
```

# Generated Classes

By default, the following files will be generated.
#####[name]DB.java
- This is the main database. It extends `SQLiteOpenHelper` and contains all necessary code for maintaining the database and column names and positions.

#####[contentprovidername].java
- This class is the content provider to use. It's your run of the mill android `ContentProvider`.

#####[contentprovidername]Client.java
- The client provides easy access to CRUD operations on any described table. Below are the methods provided

```
- get[tablename]With[Unique]
- getAll[tablename]
- add[tablename]
- remove[tablename]With[Unique]
- removeAll[tablename]
- update[tablename]
- removee[tablename]With[Unique]
```

#####[contentprovidername]BatchClient.java
- The batch client works the same as above, but is used for batched `ContentProviderOperation`. It contains methods to easily start a batch of `ContentProviderOperation` and commit it with the correct authority. It contains basically the same methods as above.


# Describing your database

It is up to you how you sort your json files inside of the path you pass to the generator. Tables and views are only recognized based on the fact that tables contain "**fields**" and views contains "**selects**".

A configuration can be specified in your config.json file (or however you wish to call it). It should however contain the following details

```
{
    "package": "your.package.name.where.generated.classes.live",
    "contentproviderName": "YourNameContentProvider",
    "authority": "your.package.name",
    "databaseName": "YourDatabaseName",
    "databaseVersion" : 1	// increment this if you make changes
}
```

## Tables

A table consists of fields and constraints (on a table-level). Each field can also have constraints (on a field-level). By default, the primary key is the "**_id**" field, which is used by Android's adapters. You can override this by setting a field as "**autoincrement**", or creating a constraint.

Consider the following example of a User table with just an id and a name.
```
{
	"fields": [
	{
		"name": "userid",
		"type": "int",
		"constraints": [
		{
			"name": "userid",
			"definition": "default 10"
		},
		{
			"name": "idnotnull",
			"definition": "not null"
		}]
	},
	{
		"name": "username",
		"type": "string"
	}],
	"constraints": [
	{
		"name": "userid",
		"definition": "unique ( userid, username ) on conflict replace"
	}]
}
```
There are 2 fields specified under the "**fields**" array,and one constraint, which specifies that the combination of userid and username should be unique and on a conflict, is replaced. There are also constraints on the userid field, which state that the default value should be "10" and cannot be null. See this [SQLite documentation][4] on sqlite.org on more info on constraints.

## Views

A view consists of selects across one or more tables, which can be grouped and ordered, and a join type can be specified.

Consider the following example of a View which combines our above user table with a table containing avatars of users, a table containing locations from users and a table which contains other photos of users.

```
{
   "selects":[
      {
         "select":"user.userid"
      },
      {
         "select":"user.name"
      },
      {
         "select":"avatar.url"
      },
      {
         "select":"location.city"
      },
      {
         "select":"location.country"
      },
      {
         "select":"count(photo.photoid)"
      }
   ],
   "from":[
      "user",
      "avatar",
      "location",
      "Photo"
   ],
   "on":[
      "userid",
      "userid",
      "userid"
   ],
   "group":[
      "user.userid"
   ],
   "order":[
	 {
		"by":"basemessages.messageid",
		"sort":"asc"
	 }
  ],
   "join":"inner join"
}
```

We selected a few user details, the avatar's url, important location details and then a count on the user's photos to know how many he has. Next within "**from**", we defined the tables we select from. Within "**on**" we define the field we join on, in this case, all tables contain the userid field. You can also specify grouping and ordering by a field, and also what type of join you prefer.

###supported data types

These are just types you can use in your json schemes, internally these are converted to the ones [SQlite][5] supports. Everything else would be converted to a blob.

```
- Float/float
- Double/double
- Long/long
- Integer/integer
- Boolean/boolean
- real
- text
- blob
- autoincrement (special case, the field becomes an integer primary key)
```

# Acknowledgements

Thanks in part to [Square][1], for their excellent [Javawriter][2]

[1]:http://square.github.io/
[2]:https://github.com/square/javawriter
[3]:https://github.com/Trikke/android-sqlite-generator/releases
[4]:http://www.sqlite.org/lang_createtable.html
[5]:http://www.sqlite.org/datatype3.html