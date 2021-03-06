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
package com.prepaird.objectgraphdb.edgefactories;

import com.prepaird.objectgraphdb.Config;
import com.prepaird.objectgraphdb.ObjectGraphDb;
import com.prepaird.objectgraphdb.utils.Utils;

/**
 *
 * @author Oliver Fleetwood
 */
public class DefaultEdgeFactory implements EdgeFactoryInterface {

    @Override
    public String getCreateQuery(ObjectGraphDb db, Class edgeClass, Object from, Object to) throws Exception {
        return "Create Edge " + edgeClass.getSimpleName() + " FROM " + Utils.escapeRecordId(from, db) + " TO " + Utils.escapeRecordId(to, db) + " RETRY " + Config.getTRANSACTION_RETRY_COUNT();
    }

    @Override
    public String getUpdateQuery(ObjectGraphDb db, Class edgeClass, Object edge, Object from, Object to) throws Exception {
        return "Update " + Utils.escapeRecordId(edge,db) + " set lastUpdate = Date()";
    }

    @Override
    public String getDeleteQuery(ObjectGraphDb db, Class edgeClass, Object from, Object to) throws Exception {
        return "Delete Edge " + edgeClass.getSimpleName() + " FROM " + Utils.escapeRecordId(from, db) + " TO " + Utils.escapeRecordId(to, db);

    }

}
