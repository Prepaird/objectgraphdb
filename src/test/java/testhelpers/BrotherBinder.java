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
package testhelpers;

import com.prepaird.objectgraphdb.ObjectGraphDb;
import com.prepaird.objectgraphdb.queryrunners.OGBinder;
import com.prepaird.objectgraphdb.utils.Utils;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.util.List;
import jpatest.edge.SonOfFather;

/**
 *
 * @author Oliver Fleetwood
 */
public class BrotherBinder implements OGBinder {

    @Override
    public Object onLoad(ObjectGraphDb db, ODocument sourceVertex) {
        String maleId = Utils.getRecordId(sourceVertex);
        String q = "Select list(@rid.asString()) as brotherIds from ("
                + "select expand(out('" + SonOfFather.class.getSimpleName() + "').in('" + SonOfFather.class.getSimpleName() + "')[@rid <> " + maleId + "]) from " + maleId
                + ")";
//        Log.debug(q);
        List<ODocument> docs = db.getUnderlying().query(new OSQLSynchQuery(q));
        return docs.isEmpty() ? null : docs.get(0).field("brotherIds");
    }

    @Override
    public Object onSave(ObjectGraphDb db, ODocument sourceVertex, Object newValue) {
        return onLoad(db, sourceVertex);
    }

    @Override
    public Object onDelete(ObjectGraphDb db, ODocument source, Object value) {
        return null;
    }

}
