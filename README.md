# ObjectGraphDB
A multi-model java API for OrientDB built on top of OrientDB's Object API

## JPA & OrientDB's Object API
Start by reading [the orient db documentation for the object api](http://orientdb.com/docs/2.0/orientdb.wiki/Object-Database.html)

## ObjectGraphDB
ObjectGraphDb is an extension of OrientDb's Object and Document API. ObjectGraphDb primarily uses SQL to create edges, though we probably can theoretically use other APIs to create edges. 

The reason we created ObjectGraphDb was that we wanted a way to use a JPA-like way of creating edge relationships between objects without loosing the functionality of the current object API (in which relationsships are created as links). To put it in another way, ObjectGraphDb is the only Multi-Model API currently available for OrientDB (which is a Multi-Model DB).

The annotations used by ObjectGraphDb can be seen on OEdgeRelation, OVertexRelation and OGRelation. These classes are reasonably well documented so if you know how the different annotations and their parameters work, please go directly to the java code. OEdgeRelation is to create an edge between vertices and OGRelation binds a field to a class which performs action on load, save and delete of a POJO. These are most likely the annotations you will use 99 % of the time. OVertexRelation is read only and used to fetch vertices on edge pojos. 

If you wish to edit ObjectGraphDb you must be familiar with the java reflection API and understand how the database works on a document level. This is not necessary if you just want to use ObjectGraphDb to save and load content from the DB.

## Package Structure
All classes that you want to bind to the db should be put in a subpackage to some main package containing all DB classes. Going deeper down in the package structure you'll need to have at least two subpackages:
*  edge: all classes in this package will be edges (extend the orientdb class "E").
*  vertex: all classes in this package will be vertices (extend the orientdb class "V").


## Code Examples (including other useful JPA annotations)
***Example Vertex POJO class*** 
```Java
@Access(AccessType.FIELD)
public class Human {

    @Id //usual ID annotation used by the ObjectAPI
    protected String id;

    protected String name;

    //Outgoing edges created with the FriendTo edge class. The Friends themselves are not saved, only the edges to them.
    @OEdgeRelation(outGoing = true, edgeClass = FriendTo.class, saveable = false)
    protected transient List<Human> friends;

    //setters and getters go here....
}
```
**Example of a class which extends another class*** and adds an extra field
```Java
@Access(AccessType.FIELD)
public class Male extends Human {

    //the father object. ObjectGraphDB handles OneToMany relationships even though edges are ManyToMany in general. 
    @OEdgeRelation(outGoing = true, edgeClass = SonOfFather.class, edgeFactory = ManyToOneEdgeFactory.class, saveable = false)
    protected transient Human father;

    //The IDs of sons
    @OEdgeRelation(outGoing = false, edgeClass = SonOfFather.class, readOnly = true, idOnly = true)
    protected transient List<Object> sons;

    //Custom binding using the BrotherBinder class. 
    @OGRelation(binder = BrotherBinder.class)
    protected transient Collection<Object> brotherIds;

    //setters and getters go here...
}
```
***Saving POJOs***  
```Java
 //create one male
  Male pete = new Male();
  pete.setName("pete");
  pete = db.save(pete);
  
  //fetch the created POJO from a query
  pete = ((List<Male>) db.query(new OSQLSynchQuery("select from Human where @rid = ?"), pete.getId())).iterator().next();

  //create another male
  Male joe = new Male();
  joe.setName("joe");
  joe.setFather(pete);
  joe = db.save(joe);
  
  //reload Pete
  pete = db.reload(pete);
  //Pete's sons field will now contain the ID of Joe.  
```

You can also check out our DemoTest for more info. Unfortunately we are not able to publish many of the tests used during the development of ObjectGraphDB, so right now the test coverage is not that good.  

## Limitations
ObjectGraphDb is by no means complete. We recommend you to test the integration carefully before deploying your applications. 

 * Limited support for recursive relationships. 
 * No lazy loading.
 * Limited support for remote transactions (read more below).

### Note: 
 * Only use transaction with OEdgeRelation if you run your OrientDb database in embedded mode, otherwise the SQL will be executed outside of the transaction.
 * When you run transactions and use the idOnly annotation set to true, don't use a String as field type since that won't be updated to the new id automatically at the end of the transaction. Instead, simply use Object as field type. 
 * Mark your fields created as edges as transient. This is counter intuitive but it is the best way to prevent the ObjectApi from binding these fields to links. If they are not transient the values will be set to null but a schema will still be created for these properties. 
 * Same rules as for the Object API applies in general. You have to create getters/setters, the objects returned from the DB are proxied classes which extend your own classes. Read the OrientDb documentation for more info. 
 * All classes must be registered by OrientDBs entity manager before you can bind them. This should be done in OrientDbConnector.java. Note however that the schema is not updated automatically, so if you change the fields of a POJO, the db schema won't be updated accordingly by itself. Create some SQL to do this using the ```Create property...```keyword. 
 * There are also some promising projects such as spring-data-orientdb which might replace ObjectGraphDb in the future. 

Check out our issues for more info and planned features. 
 

 
