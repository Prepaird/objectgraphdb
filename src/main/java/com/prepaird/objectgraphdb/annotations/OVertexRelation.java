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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation representing the relations from an edge to a vertex. Always
 * readonly
 *
 * @author Oliver Fleetwood
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OVertexRelation {

    /**
     * is this the vertex that this edge leads out from or in from.
     *
     * @return
     */
    boolean outGoing() default true;

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
}
