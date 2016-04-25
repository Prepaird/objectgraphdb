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
package com.prepaird.objectgraphdb.annotations;

import com.prepaird.objectgraphdb.edgefactories.DefaultEdgeFactory;
import com.prepaird.objectgraphdb.edgefactories.EdgeFactoryInterface;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * representing a relation between two vertices
 *
 * @author Oliver Fleetwood
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OEdgeRelation {

    /**
     * the Java class representing the edge connection this vertex to the
     * related vertex
     *
     * @return
     */
    Class edgeClass();

    /**
     * Is the annotated field connected through an outgoing edge from the
     * current object. If false, the edge is in the opposite direction
     *
     * @return
     */
    boolean outGoing();// default true;

    /**
     * sets if updated should be allowed on the related vertex and the edge.
     *
     * @return
     */
    boolean readOnly() default false;

    /**
     * Note that there is a problem using idOnly in transactions. The easiest
     * solution which is still good looking is to attach ids as Objects instead
     * o Strings. Note that fields marked with the '@Id' annotation will work
     * even if they are Strings, but those with the
     * '@OEdgeRelation(idOnly=true)' annotation will not be updated after
     * transactions. The IDs are serialized as Strings so the API should work as
     * it is. It is also okay to set the fields as Strings, it shouldn't break
     * anything.
     *
     * Some things that you should know if you use
     * '@OEdgeRelation(idOnly=true)':
     *
     * If the field might be updated in a transaction, use an Object as field
     * type and not a String or Long, or at least make sure to reload the
     * object. If the field can be null, which is often case, use the null-safe
     * {@link Const#getRecordId(java.lang.Object)} instead of
     * myObj.getRelatedId().toString() Be careful using .equals() on IDs. For
     * example: Objects.equals("#21:0",invite.getCompanyId()) might fail if
     * invite.getCompanyId() returns an OIDentifiable, not a String. The proper
     * way to to this is:
     * Objects.equals("#21:0",{@link Const#getRecordId(java.lang.Object)}))
     *
     * @return
     */
    boolean idOnly() default false;

    /**
     * same as in normal JPA annotations. If true, vertices will deleted
     * automatically if the edges of the annotated field are deleted.
     *
     * @return
     */
    boolean orphanRemoval() default true;

    /**
     * true if the vertex can be saved through cascading. False means that the
     * related vertex cannot be saved, but the connecting edge may still be
     * updated TODO: rename this or replace it using cascade settings
     *
     * @return
     */
    boolean saveable() default true;

    /**
     * An {@link AccumulatorType} which accumulates properties over the
     * edges/related vertices Default is {@link AccumulatorType#OBJECT} meaning
     * that the objects are simply loaded.
     *
     * @return
     */
    AccumulatorType accumulator() default AccumulatorType.OBJECT;

    /**
     * An {@link EdgeFactoryInterface} which handles the creation of the
     * connection edges
     *
     * @return
     */
    Class<? extends EdgeFactoryInterface> edgeFactory() default DefaultEdgeFactory.class;

    /**
     * A singleton array for comparing a list of related vertices. Optional.
     *
     * @return
     */
    Class<? extends Comparator>[] comparator() default {};

    public static enum AccumulatorType {

        OBJECT,
        /**
         * counts the number of elements in the edge collection.
         */
        COUNT;

    }

}
