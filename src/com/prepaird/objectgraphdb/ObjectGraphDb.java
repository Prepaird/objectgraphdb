/* 
 * Copyright 2016 Prepaird AB (556983-5688).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.prepaird.objectgraphdb;

import com.google.common.reflect.ClassPath;
import com.prepaird.objectgraphdb.annotations.OEdgeRelation;
import com.prepaird.objectgraphdb.annotations.OGRelation;
import com.prepaird.objectgraphdb.annotations.OVertexRelation;
import com.prepaird.objectgraphdb.edgefactories.EdgeFactoryInterface;
import com.prepaird.objectgraphdb.exceptions.OGDBException;
import com.prepaird.objectgraphdb.queryrunners.OGBinder;
import com.prepaird.objectgraphdb.utils.Log;
import com.prepaird.objectgraphdb.utils.Utils;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.storage.ORecordCallback;
import com.orientechnologies.orient.core.version.ORecordVersion;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author Oliver Fleetwood
 */
public class ObjectGraphDb extends OObjectDatabaseTx {

    private static Set<Class> edgeClasses = new TreeSet<>(), vertexClasses = new TreeSet<>();
    private boolean loadRelatedPojos = true;

    public static void main(String[] args) throws Exception {
    }

    public ObjectGraphDb(ODatabaseDocumentTx iDatabase) {
        super(iDatabase);
        //graphDb = new OrientGraph(iDatabase);
    }

    @Override
    public <RET> RET save(final Object iContent) {
        return (RET) save(iContent, (String) null, OPERATION_MODE.SYNCHRONOUS, false, null, null);
    }

    @Override
    public <RET> RET save(final Object iPojo, final String iClusterName) {
        return (RET) save(iPojo, iClusterName, OPERATION_MODE.SYNCHRONOUS, false, null, null);
    }

    @Override
    public <RET> RET save(final Object iContent, OPERATION_MODE iMode, boolean iForceCreate,
            final ORecordCallback<? extends Number> iRecordCreatedCallback, ORecordCallback<ORecordVersion> iRecordUpdatedCallback) {
        return (RET) save(iContent, null, iMode, false, iRecordCreatedCallback, iRecordUpdatedCallback);
    }

    @Override
    public <RET> RET save(Object iPojo, final String iClusterName, OPERATION_MODE iMode, boolean iForceCreate,
            final ORecordCallback<? extends Number> iRecordCreatedCallback, ORecordCallback<ORecordVersion> iRecordUpdatedCallback) {
        Map<Method, AnnotationFieldValueWrapper> savedRelations = new HashMap<>();
        registerEdgeRelations(iPojo, savedRelations); //find edgeRelations and save them here.
        RET r = super.save(iPojo, iClusterName, iMode, iForceCreate, iRecordCreatedCallback, iRecordUpdatedCallback); //save regular field
        r = updateEdgeRelations(r, savedRelations, false); //cascade edges and update object
        return r;
    }

    /**
     * reloads this object but not any related objects
     *
     * @param <RET>
     * @param iPojo
     * @return
     */
    protected <RET> RET reloadNonRecursive(Object iPojo) {
        Map<Method, AnnotationFieldValueWrapper> savedRelations = new HashMap<>();
        registerEdgeRelations(iPojo, savedRelations); //find edgeRelations and save them here.
        RET r = super.reload(iPojo, null, true, true);
        //reload this pojo but not related pojos
        //set values
        setValues(r, savedRelations);
        return r;
    }

    @Override
    public <RET> RET reload(final Object iPojo) {
        return (RET) reload(iPojo, null, true);
    }

    @Override
    public <RET> RET reload(final Object iPojo, final boolean iIgnoreCache) {
        return (RET) reload(iPojo, null, iIgnoreCache);
    }

    @Override
    public <RET> RET reload(Object iPojo, final String iFetchPlan, final boolean iIgnoreCache) {
        return reload(iPojo, iFetchPlan, iIgnoreCache, true);
    }

    @Override
    public <RET> RET reload(Object iObject, String iFetchPlan, boolean iIgnoreCache, boolean force) {
        iObject = super.reload(iObject, iFetchPlan, iIgnoreCache, force);
        return (RET) loadRelatedPojos(iObject, iFetchPlan);

    }

    private ODatabaseObject deleteHelper(Object o, ORecordVersion version) {
        try {
            List list = super.command(new OSQLSynchQuery<>("Select from " + Utils.getRecordId(o))).execute();
            if (list.isEmpty()) {
                Log.error("cannot delete record " + Utils.getRecordId(o) + ". Record not found.");
                return this;
            } else {
                o = list.get(0);
            }
            boolean isVertex = isVertex(o);
            if (isVertex) {
                Map<Method, AnnotationFieldValueWrapper> savedRelations = new HashMap<>();
                registerEdgeRelations(o, savedRelations); //find edgeRelations and save them here.
                //set all relations to null.
                savedRelations.values().stream().forEach(a -> {
                    if (a.relation instanceof OEdgeRelation && ((OEdgeRelation) a.relation).orphanRemoval()) {
                        //force remove this edge.
                        a.value = null;
                    }
                });
                updateEdgeRelations(o, savedRelations, true); //cascade edges and update object
            }
            String deleteQuery;// = "DELETE" + (isVertex ? " Vertex " : " Edge ") + Utils.escapeRecordId(Utils.getRecordId(o));
            //attempt below using lock record...
            if (isVertex) {
                deleteQuery = "DELETE Vertex from (select from " + Utils.escapeRecordId(o) + " LOCK RECORD)";
            } else {
                //TODO: uncomment the below query after https://github.com/orientechnologies/orientdb/issues/5874 has been fixed
//                deleteQuery = "DELETE Edge E where @rid in (select from " + Utils.escapeRecordId(Utils.getRecordId(o)) + " LOCK RECORD)";
                deleteQuery = "DELETE Edge " + Utils.escapeRecordId(o);

            }
            super.command(new OCommandSQL(deleteQuery)).execute();
        } catch (RuntimeException ex) {
            Log.error(ex);
            throw ex;
        } catch (Exception ex) {
            Log.error(ex);
        }
        return this;
    }

    @Override
    public ODatabaseObject delete(ORID rid, ORecordVersion version) {
        return deleteHelper(rid, null);
    }

    @Override
    public ODatabaseObject delete(ORID rid) {
        return delete(rid, null);
    }

    @Override
    public ODatabaseObject delete(ORecord record) {
        return deleteHelper(record);
    }

    @Override
    public ODatabaseObject delete(Object iPojo) {
        return deleteHelper(iPojo);
    }

    private ODatabaseObject deleteHelper(Object o) {
        return deleteHelper(o, null);
    }

    @Override
    public Object getUserObjectByRecord(final OIdentifiable iRecord, final String iFetchPlan, final boolean iCreate) {
        Object o = super.getUserObjectByRecord(iRecord, iFetchPlan, iCreate);
        return loadRelatedPojos(o, iFetchPlan);
    }

    /**
     *
     * @param o          should be an entity attached to the db
     * @param iFetchPlan
     * @param iCreate
     * @return
     */
    public Object loadRelatedPojos(final Object o, final String iFetchPlan) {
        if (!isLoadRelatedPojos()) {
            return o;
        }
        //the objects returned here are proxy object custom to orientdb. The actual class is just the superclass of the object returned
        Class cl = o.getClass();
        while (cl.getSuperclass() != null) {
            for (Field f : cl.getDeclaredFields()) {
                f.setAccessible(true);
                //TODO use another method to get the specific annotations
                for (Annotation a : f.getAnnotations()) {
                    if (isOGAnnotation(a.annotationType())) {
                        try {
                            Method setterMethod = Utils.getSetterMethod(cl, f);
                            if (setterMethod != null) {
                                if (a.annotationType() == OGRelation.class) {
                                    OGRelation rel = (OGRelation) a;
                                    OGBinder ogb = (OGBinder) rel.binder().newInstance();
                                    setterMethod.invoke(o, ogb.onLoad(this, getRecordByUserObject(o, false)));
                                } else {
                                    boolean idOnly;
                                    Collection relations;

                                    if (a.annotationType() == OEdgeRelation.class) {
                                        OEdgeRelation rel = (OEdgeRelation) a;
                                        idOnly = rel.idOnly();
                                        relations = getExistingVertices(o, f, rel);
                                        sortCollectionIfNecessary(relations, rel);

                                    } else { // (a.annotationType() == OVertexRelation.class)
                                        //load the vertices connected to this edge
                                        OVertexRelation rel = (OVertexRelation) a;
                                        idOnly = rel.idOnly();
                                        relations = getExistingVertices(o, f, rel);
                                    }
                                    Object[] setterParam = new Object[1];
                                    boolean isCollection = Iterable.class.isAssignableFrom(f.getType());
                                    if (isCollection) {
                                        setterParam[0] = relations;
                                    } else if (relations.iterator().hasNext()) {
                                        setterParam[0] = relations.iterator().next();
                                    } else {
                                        setterParam[0] = null;
                                    }
                                    if (idOnly) {
                                        setterParam[0] = convertToIdType(f, setterParam[0]);
                                    }
                                    setterMethod.invoke(o, setterParam);
                                }
                            } else {
                                Log.error("Cannot set " + f.getName() + " for class " + cl.getName());
                            }
                        } catch (RuntimeException ex) {
                            Log.error(ex);
                            throw ex;
                        } catch (Exception ex) {
                            Log.error(ex);
                        }

                    }
                }
            }
            cl = cl.getSuperclass();
        }
        return o;
    }

    private void registerEdgeRelations(Object o, Map<Method, AnnotationFieldValueWrapper> savedRelations) {
        Class cl = o.getClass();
        // Checking all the fields for annotations

        while (cl.getSuperclass() != null) {
            for (Field f : cl.getDeclaredFields()) {
                try {
                    // Processing all the annotations on a single field
                    for (Annotation a : f.getAnnotations()) {
                        if (a.annotationType() == OEdgeRelation.class || a.annotationType() == OGRelation.class) {
                            // Setting the field to be accessible from our class
                            // is it is a private member of the class under processing
                            // (which its most likely going to be)
                            // The setAccessible method will not work if you have
                            // Java SecurityManager configured and active.
                            f.setAccessible(true);
                            //this is assumed to be a pojo. call the getter method.
                            //captialize the first letter and add "get"
                            Method getterMethod = Utils.getGetterMethod(cl, f);
                            if (getterMethod == null) {
                                //both methods are null. no good. cannot get value
                                Log.error("could not find getter method for " + f.getName() + " on object of class " + cl.getSimpleName() + ". Cannot save edgerelations");
                                continue;
                            }
                            String setterName = Utils.getSetterName(f);
                            Class[] params = {getterMethod.getReturnType()};
                            Method setterMethod = Utils.getMethod(cl, setterName, params);
                            if (setterMethod == null) {
                                Log.error("Setter method " + setterName + " is null for class " + cl.getSimpleName() + ". Cannot save relation for field " + f.getName());
                                continue;
                            }
                            // Checking for a NullValueValidate annotation

                            savedRelations.put(setterMethod, new AnnotationFieldValueWrapper(f, a, getterMethod.invoke(o)));
                            //if(!Modifier.isTransient(f.getModifiers())){
                            //set the value to null so it isn't saved as a link. This shouldn't be necessary if the field is transient though
                            Object[] nullArray = {null};
                            setterMethod.invoke(o, nullArray);

                        }

                    }
                } catch (RuntimeException ex) {
                    Log.error(ex);
                    throw ex;
                } catch (Exception ex) {
                    Log.error(ex);
                }
            }
            cl = cl.getSuperclass();
        }
    }

    private <RET> RET updateEdgeRelations(RET r, Map<Method, AnnotationFieldValueWrapper> savedRelations, boolean onDelete) {
        //in order to keep our pojo synched with the latest db version we first update all relations
        //then reload the pojo and then attach all new related objects
        savedRelations.entrySet().stream().forEach(e -> {
            try {
                Field f = e.getValue().field;
                Object value = e.getValue().value;
                if (e.getValue().relation instanceof OEdgeRelation) {
                    OEdgeRelation relation = (OEdgeRelation) e.getValue().relation;
                    //First of all, get the existing edges for this field
                    Map<String, EdgeVertexPair> oldRelations = getExistingEdgesAndVertices(r, f, relation).stream()
                            .collect(Collectors.toMap(o -> Utils.getRecordId(o.getVertex()), o -> o, (o1, o2) -> {
                                throw new OGDBException(100, "conflict, ids: " + Utils.getRecordId(o1.getVertex()) + ", " + Utils.getRecordId(o1.getEdge()) + " .. " + o2.getVertex().getClass().getSimpleName() + ", " + f.getName() + ", " + value.getClass().getSimpleName());
                            })),
                            newRelations = new HashMap<>(oldRelations.size());
                    Object setParam;
                    if (relation.readOnly() || relation.accumulator() != OEdgeRelation.AccumulatorType.OBJECT) {
                        //don't do any updates whatsoever...
                        setParam = value;
                        //TODO maybe set to oldRealtions.values() instead?
                        newRelations = oldRelations;
                    } else {
                        if (value == null) {
                            //no edge should be created.
                            //possible edges should be deleted though, but that will be taken care of below
                            setParam = null;
                        } else if (value instanceof Iterable) {
                            //create edges for every object and remove object which should be cascaded away
                            //save object
                            Collection coll = getCollection(f, relation);
                            Iterable iterable = (Iterable) value;
                            Iterator itt = iterable.iterator();
                            while (itt.hasNext()) {

                                coll.add(saveRelatedVertex(r, itt.next(), relation, oldRelations, newRelations));
                            }
                            //sort if necessary
                            sortCollectionIfNecessary(coll, relation);
                            setParam = coll;
                        } else {
                            setParam = saveRelatedVertex(r, value, relation, oldRelations, newRelations);
                        }
                    }
                    deleteOldVerticesAndEdges(r, relation, newRelations, oldRelations);
                    e.getValue().value = setParam;
                } else { //OGRelation
                    OGRelation rel = (OGRelation) e.getValue().relation;
                    OGBinder ogb = (OGBinder) rel.binder().newInstance();
                    Object setParam;
                    if (onDelete) {
                        setParam = ogb.onDelete(this, getRecordByUserObject(r, false), value);
                    } else {
                        setParam = ogb.onSave(this, getRecordByUserObject(r, false), value);
                    }
                    e.getKey().invoke(r, setParam);
                    e.getValue().value = setParam;
                }
            } catch (RuntimeException ex) {
                Log.error(ex);
                throw ex;
            } catch (Exception ex) {
                Log.error(ex);
            }
        });
        //reload pojo from db
        RET ret = reloadNonRecursive(r);
        //set values
        setValues(ret, savedRelations);
        return ret;
    }

    protected <RET> void setValues(RET r, Map<Method, AnnotationFieldValueWrapper> savedRelations) {
        savedRelations.entrySet().stream().forEach(e -> {
            Object value = e.getValue().value;
            Object[] args = {value};
            try {
                e.getKey().invoke(r, args);
            } catch (RuntimeException ex) {
                Log.error(ex);
                throw ex;
            } catch (Exception ex) {
                Log.error(ex);
            }
        });
    }

    private Object saveRelatedVertex(Object vertex, Object relatedVertex, OEdgeRelation relation, Map<String, EdgeVertexPair> oldRelations, Map<String, EdgeVertexPair> newRelations) throws InstantiationException, IllegalAccessException, Exception {
        //save other vertex in a recursive manner.
        //TODO: this most likely doesn't work if this object is referenced by otherV. it can easily be handled by storing
        //saved object in a stack or even better by adding more properties to the annotations. we are not however trying to implement true jpa!
        if (relation.saveable() && !relation.idOnly()) {
            relatedVertex = save(relatedVertex);
        }
        Object edge = createEdgeIfNecessary(vertex, relatedVertex, relation, oldRelations);
        newRelations.put(Utils.getRecordId(relatedVertex),
                new EdgeVertexPair(
                        relation.idOnly() ? relatedVertex : reloadNonRecursive(relatedVertex),
                        edge)
        );
        return relatedVertex;
    }

    private Object createEdgeIfNecessary(Object vertex, Object relatedVertex, OEdgeRelation relation, Map<String, EdgeVertexPair> oldRelations) throws InstantiationException, IllegalAccessException, Exception {
        Object from = relation.outGoing() ? vertex : relatedVertex,
                to = relation.outGoing() ? relatedVertex : vertex;
        //see if edge already exists and is not lightweight
        EdgeFactoryInterface edgeFactory = createEdgeFactory(relation);
        EdgeVertexPair oldRelation = oldRelations.get(Utils.getRecordId(relatedVertex));

        if (oldRelation != null) {
            String updateEdgeQ = edgeFactory.getUpdateQuery(this, relation.edgeClass(), oldRelation.getEdge(), from, to);
            if (!oldRelation.isLightWeight() && updateEdgeQ != null) {
                //Log.debug(updateEdgeQ);
                return super.command(new OCommandSQL(updateEdgeQ)).execute();
            } else {
                //edge is either lightweight or should not be updated
                return oldRelation.getEdge();
            }
        } else {
            //Create a new edge
            String createEdgeQ = edgeFactory.getCreateQuery(this, relation.edgeClass(), from, to);
            //Log.debug(createEdgeQ);
            //work around to old bug in orientdb.
            //TODO: see if the underlying problem has been fixed.
            StringBuilder qBuilder
                    = new StringBuilder("LET edge = ")
                    .append(createEdgeQ)
                    .append("\n")
                    .append("LET res = SELECT FROM $edge.@rid\n")
                    .append("RETURN $res");
            return super.command(new OCommandScript("sql", qBuilder.toString())).execute();
        }
    }

    private void deleteOldVerticesAndEdges(Object vertex, OEdgeRelation relation, Map<String, EdgeVertexPair> newRelations, Map<String, EdgeVertexPair> oldRelations) {
        if (newRelations != oldRelations && !relation.readOnly()) {
            //delete oldRelations if they don't exist in the new relation
            oldRelations.entrySet().stream().filter(e -> !newRelations.containsKey(e.getKey()))
                    .map(e -> e.getValue()).forEach(v -> {
                        try {
                            if (relation.orphanRemoval()) {
                                //delete the vertex
                                deleteHelper(v.getVertex(), getVersion(v.getVertex()));
                            } else {
                                //Otherwise just the edge should be delted
                                EdgeFactoryInterface edgeFactory = createEdgeFactory(relation);
                                String deleteEdgeQ = edgeFactory.getDeleteQuery(this, relation.edgeClass(), vertex, v.getVertex());
                                super.command(new OCommandSQL(deleteEdgeQ)).execute();
                            }
                        } catch (RuntimeException ex) {
                            Log.error(ex);
                            throw ex;
                        } catch (Exception ex) {
                            Log.error(ex);
                        }
                    });
        }
    }

    private EdgeVertexPair getEdgeAndVertex(OIdentifiable next, OEdgeRelation rel) {
        /* FREE THE RECORD FROM THE LOCAL CACHE: THIS IS A WORKAROUND TO A BUG
         * IN
         * ORIENTDB OR {@link ObjectGraphDb} WHEN A RELATED RECORD IS LOADED
         * WITH AN OLD VERSION FROM CACHE
         * TODO: FIX THE BUG INSTEAD
         * MIGHT BE FIXED IF WE USE AN EMBEDDED DB
         */
        getLocalCache().freeRecord(next.getIdentity());
        ODocument doc = next.getRecord();
        EdgeVertexPair pair = new EdgeVertexPair();
        //determine if this is a lightweight edge or not
        if (!doc.containsField("in")) {
            //we just crossed a lightweight edge, so this is the object we're looking for
            //TODO we should look at the class instead and see it if is an edge. Right now we name fields "in" on a vertex and it fails
            if (rel.idOnly()) {
                pair.vertex = next;
            } else {
                pair.vertex = getUserObjectByRecord(doc, "*:1", false);
            }
            pair.setEdge(null);
        } else {
            pair.setEdge(getUserObjectByRecord(doc, "*:1", false));
            next = doc.field(rel.outGoing() ? "in" : "out");
            getLocalCache().freeRecord(next.getIdentity());
            //else load the vertex on the other side of the edge
            if (rel.idOnly()) {
                pair.vertex = next;
            } else {
                pair.vertex = getUserObjectByRecord(next, "*:1", false);
            }
        }
        return pair;
    }

    private EdgeFactoryInterface createEdgeFactory(OEdgeRelation rel) throws InstantiationException, IllegalAccessException {
        return rel.edgeFactory().newInstance();
    }

    private Collection<EdgeVertexPair> getExistingEdgesAndVertices(Object o, Field f, OEdgeRelation rel) {
        //Load the underlying ODocument
        ODocument d = super.getRecordByUserObject(o, false);
        String edgeFieldName = (rel.outGoing() ? "out_" : "in_") + WordUtils.capitalize(rel.edgeClass().getSimpleName());

        Class<?> type = f.getType();

        boolean isCollection = Iterable.class
                .isAssignableFrom(type);
        if (rel.idOnly()) {
            type = ORID.class;
        } else if (isCollection) {
            //this is a collection so we need to get the type of the collection elements
            type = getCollectionGenericType(f);
        }
        Object edges;

        if (rel.accumulator()
                == OEdgeRelation.AccumulatorType.COUNT) {
            //ignore type since we just want to count
            edges = d.field(edgeFieldName);
        } else {
            edges = d.field(edgeFieldName, type);
        }
        Iterable<OIdentifiable> relations;
        if (edges instanceof Iterable) {
            if (rel.accumulator() == OEdgeRelation.AccumulatorType.COUNT) {
                //count the number of edges
                //TODO: include a filter here
                Iterator itt = ((Iterable) edges).iterator();
                Long count = 0l;
                while (itt.hasNext()) {
                    count++;
                    itt.next();
                }
                return Arrays.asList(new EdgeVertexPair(count, null));
            } else {
                relations = (Iterable<OIdentifiable>) edges;
            }
        } else if (edges instanceof OIdentifiable) //sometimes the field returned is just a single OIdentifiable and not a collection.
        {
            if (rel.accumulator() == OEdgeRelation.AccumulatorType.COUNT) {
                //the count is simply one
                return Arrays.asList(new EdgeVertexPair(1l, null));
            } else {
                relations = Collections.singleton((OIdentifiable) edges);
            }
        } else {
            if (rel.accumulator() == OEdgeRelation.AccumulatorType.COUNT) {
                //the count is simply zero
                return Arrays.asList(new EdgeVertexPair(0L, null));
            }
            relations = null;
        }
        if (relations
                != null) {
            //go trough all outgoing edges and load both the edge and vertex
            Collection coll = new ArrayList<>();
            Iterator<OIdentifiable> itt = relations.iterator();
            while (itt.hasNext()) {
                //TODO filter here
                OIdentifiable next = itt.next();
                if (next == null || next.getIdentity() == null) {
                    //this is a bugfix/workaround for something shady that goes on in orientdb,
                    //I think it has to do with concurrency, a related object is removed through a query on another thread
                    //causing the iterator to find a broken edge or smething
                    continue;
                }
                EdgeVertexPair evp = getEdgeAndVertex(next, rel);
                coll.add(evp);
                if (!isCollection) {
                    //no need to fetch more relations than necessary if there happens to be more edges than one but we only require one element
                    break;
                }
            }
            return coll;
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * loads outGoing and inGoing vertices connected to the vertex o
     *
     * @param o
     * @param f
     * @param rel
     * @return
     */
    private Collection getExistingVertices(Object o, Field f, OEdgeRelation rel) throws InstantiationException, IllegalAccessException {
        Collection<EdgeVertexPair> coll = getExistingEdgesAndVertices(o, f, rel);
        return convertToVerticeCollection(coll, f, rel);
    }

    /**
     * loads outGoing and inGoing vertices connected to the edge o
     *
     * @param o
     * @param f
     * @param rel
     * @return
     */
    private Collection getExistingVertices(Object o, Field f, OVertexRelation rel) {
        ODocument doc = getRecordByUserObject(o, false);
        OIdentifiable id = doc.field((rel.outGoing() ? "out" : "in"));
        Object vertex = rel.idOnly() ? id : getUserObjectByRecord(id.getRecord(), "*:1");
        return Collections.singleton(vertex);
    }

    /**
     * converts to a collection with just the vertices depending. The collection
     * is either a list or set depending on the fieldtype
     *
     * @param coll
     * @param f
     * @return
     */
    private Collection convertToVerticeCollection(Collection<EdgeVertexPair> coll, Field f, OEdgeRelation rel) throws InstantiationException, IllegalAccessException {
        Collection ret;
        /* Stream stream = coll.stream().map(o -> o.getVertex());
         *
         * if (Set.class
         * .isAssignableFrom(f.getType())) {
         * ret = (Collection) stream.collect(Collectors.toSet());
         * } else {
         * ret = (Collection) stream.collect(Collectors.toList());
         * }
         * return ret; */
        ret = getCollection(f, rel);
        ret.addAll(coll.stream().map(ev -> ev.vertex).collect(Collectors.toList()));
        return ret;
    }

    private Object convertToIdType(Field f, Object setterParam) throws InstantiationException, IllegalAccessException {
        if (setterParam == null) {
            return null;

        }
        if (setterParam instanceof Collection && Collection.class
                .isAssignableFrom(f.getType())) {
            //go through each object and convert to id....
            Collection coll = getCollection(f, null);
            Class<?> genericType = getCollectionGenericType(f);
            for (Object o : (Collection) setterParam) {
                coll.add(convertObjectToIdType(genericType, o));
            }
            return coll;
        } else {
            return convertObjectToIdType(f.getType(), setterParam);
        }
    }

    private Object convertObjectToIdType(Class<?> type, Object setterParam) {
        OIdentifiable id;
        if (setterParam instanceof OIdentifiable) {
            id = ((OIdentifiable) setterParam).getIdentity();
        } else {
            Log.error("Found unexpected class type " + setterParam.getClass().getName() + " of idOnly field. Expected type is " + type.getName());
            return setterParam;

        }
        if (type == String.class) {
            if (id.getIdentity()
                    .isTemporary()) {
                Log.error("Binding a String-field with a temporary id which won't be updated after the transaction has been commited unless the POJO is reloaded.");
            }

            return id.getIdentity()
                    .toString();
        } else if (Long.class
                .isAssignableFrom(type)) {
            if (id.getIdentity()
                    .isTemporary()) {
                Log.error("Binding a Long-field with a temporary id which won't be updated after the transaction has been commited unless the POJO is reloaded.");
            }

            return id.getIdentity()
                    .getClusterPosition();
        } else {
            return id.getIdentity();
        }
    }

    public boolean isLoadRelatedPojos() {
        return loadRelatedPojos;
    }

    public void setLoadRelatedPojos(boolean loadRelatedPojos) {
        this.loadRelatedPojos = loadRelatedPojos;
    }

    public static void registerEdgeClasses(String packageName) throws IOException {
        registerEdgeClasses(getClassCollection(packageName));
    }

    public static void registerVertexClasses(String packageName) throws IOException {
        registerVertexClasses(getClassCollection(packageName));
    }

    public static void registerEdgeClasses(Collection<Class> classNames) {
        edgeClasses = new HashSet<>(classNames);
    }

    public static void registerVertexClasses(Collection<Class> classNames) {
        vertexClasses = new HashSet<>(classNames);

    }

    private static Collection<Class> getClassCollection(String packageName) throws IOException {
        Collection<Class> coll = new ArrayList<>();
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (final ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive(packageName)) {
            coll.add(classInfo.load());
        }

        return coll;
    }

    public boolean isEdge(Object o) {
        return classInSet(o, edgeClasses);
    }

    public boolean isVertex(Object o) {
        return classInSet(o, vertexClasses);
    }

    /**
     * Return all records related through the specified edge class to the
     * specific odocument. NOTE THAT THIS WON'T WORK WITH SUPER CLASSES!
     *
     * @param outGoing
     * @param edgeClass
     * @param d
     * @return
     */
    public List<ODocument> getRelatedVertices(boolean outGoing, Class edgeClass, ODocument d) {
        ORidBag ridBag = (ORidBag) d.field((outGoing ? "out_" : "in_") + edgeClass.getSimpleName());
        if (ridBag == null) {
            return null;
        }
        //convert records that will be fetched from disk if they aren't in memory already. see orientdb documentation for more info
        ridBag.setAutoConvertToRecord(true);
        //convert to records for convencience
        ridBag.convertLinks2Records();
        Iterator<OIdentifiable> itt = ridBag.iterator();
        List<ODocument> vertexList = new ArrayList<>(ridBag.size());
        while (itt.hasNext()) {
            ODocument edge = (ODocument) itt.next();
            if (edge == null) {
                Log.error("found null edge of class " + edgeClass.getSimpleName() + " on " + Utils.getRecordId(d));
                continue;
            }
            if (!edge.containsField("in")) {
                //we just crossed a lightweight edge and are already at the vertex. 
                //TODO: not 100% idiot proof if the user names a field "in" on a vertex...
                vertexList.add(edge);
            } else {
                vertexList.add(edge.field((outGoing ? "in" : "out")));
            }
        }

        return vertexList;
    }

    private boolean classInSet(Object o, Set<Class> classes) {
        if (o == null) {
            return false;
        }
        //iterate through all parents and see if we encounter something present in the set
        //if we just pick the class of o immediately it won't work with proxy objects
        Class cl = o.getClass();
        while (cl.getSuperclass() != null) {
            if (classes.contains(cl)) {
                return true;
            }
            cl = cl.getSuperclass();
        }
        return false;

    }

    private <E> Collection<E> getCollection(Field f, OEdgeRelation relation) throws InstantiationException, IllegalAccessException {
        // Class<E> cl = getCollectionGenericType(f);
        if (Set.class
                .isAssignableFrom(f.getType())) {
            return relation.comparator().length > 0 ? new TreeSet<>(relation.comparator()[0].newInstance()) : new TreeSet<>();
        } else {
            return new ArrayList<>();
        }
    }

    private <E> Class<E> getCollectionGenericType(Field f) {
        //since this is an iterable we actually need the generic type of the iterable
        //see http://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list 
        Type type = f.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            return paramType.getActualTypeArguments().length > 0 ? (Class<E>) paramType.getActualTypeArguments()[0] : null;
        } else if (type instanceof Class) {
            return (Class) type;
        } else {
            Log.error("cannot determine type of " + type.getTypeName());
            return null;
        }
    }

    private void sortCollectionIfNecessary(Collection coll, OEdgeRelation relation) throws InstantiationException, IllegalAccessException {
        if (coll instanceof List && relation != null && relation.comparator().length > 0) {
            Collections.sort((List) coll, relation.comparator()[0].newInstance());

        }
    }

    private boolean isOGAnnotation(Class<? extends Annotation> annotationType) {
        return annotationType == OEdgeRelation.class
                || annotationType == OVertexRelation.class
                || annotationType == OGRelation.class;

    }

    @XmlRootElement //just used for debugging. to be removed
    private static class EdgeVertexPair {

        protected Object vertex;
        protected Object edge;

        public EdgeVertexPair() {
        }

        private EdgeVertexPair(Object vertex, Object edge) {
            this.vertex = vertex;
            this.edge = edge;
        }

        public Object getVertex() {
            return vertex;
        }

        public Object getEdge() {
            return edge;
        }

        public void setEdge(Object edge) {
            this.edge = edge;
        }

        public boolean isLightWeight() {
            return this.edge == null;
        }
    }

    protected class AnnotationFieldValueWrapper {

        Annotation relation;
        Object value;
        Field field;

        public AnnotationFieldValueWrapper(Field field, Annotation edgeRelation, Object value) {
            this.relation = edgeRelation;
            this.value = value;
            this.field = field;
        }
    }
}
