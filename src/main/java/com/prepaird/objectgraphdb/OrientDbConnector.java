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
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

/**
 * Class which
 *
 * @author Oliver Fleetwood
 */
public class OrientDbConnector {

    /**
     * Register all classes in {@link webapp.models.jpa} as subclasses to V
     * (vertices) or E (edges) in the db.
     *
     * @param db         an instance of the database
     * @param jpaPackage top level package name containing the subpackages
     *                   jpaPackage.vertex & jpaPackage.edge. All entityclasses
     *                   should be in this package
     * @throws IOException
     */
    public static synchronized void registerEntityClasses(OObjectDatabaseTx db, String jpaPackage) throws IOException {
        String edgePackage = jpaPackage + ".edge", vertexPackage = jpaPackage + ".vertex";
        db.setAutomaticSchemaGeneration(true);
        db.getEntityManager().registerEntityClasses(jpaPackage);

        //load all edges/vertices and without a superclass
        Collection<String> topEdges = new TreeSet<>(), topVertices = new TreeSet<>();
        loadClasses(topEdges, edgePackage);
        loadClasses(topVertices, vertexPackage);
        //set superclass V to custom classes without a superclass
        OSchema schema = db.getMetadata().getSchema();
        OClass V = schema.getClass("V"),
                E = schema.getClass("E");

        for (OClass c : schema.getClasses()) {
            //update superclass if necessary
            if (topEdges.contains(c.getName()) && !c.getSuperClassesNames().contains(E.getName())) {
                //this is an edge...
                c.setSuperClasses(Arrays.asList(E));
            } else if (topVertices.contains(c.getName()) && !c.getSuperClassesNames().contains(V.getName())) {
                c.setSuperClasses(Arrays.asList(V));
            } else {
                //TODO: make sure the currect superclass is set from java. this should be handled by orientdb entity manager by default but it remains to verify that it works
            }
        }
        ObjectGraphDb.registerEdgeClasses(edgePackage);
        ObjectGraphDb.registerVertexClasses(vertexPackage);
    }

    private static void loadClasses(Collection<String> coll, String packageName) throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (final ClassPath.ClassInfo classInfo : ClassPath.from(loader).getTopLevelClassesRecursive(packageName)) {
            if (classInfo.load().getSuperclass().equals(Object.class)) {
                coll.add(classInfo.getSimpleName());
            }
        }
    }
}
