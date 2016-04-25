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

import com.google.common.base.Objects;
import com.prepaird.objectgraphdb.ObjectGraphDb;
import com.prepaird.objectgraphdb.queryrunners.OGBinder;
import com.prepaird.objectgraphdb.utils.Log;
import com.prepaird.objectgraphdb.utils.Utils;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import java.util.List;
import jpatest.edge.HasPet;
import jpatest.vertex.organisms.Pet;

/**
 *
 * @author Oliver Fleetwood
 */
public class PetBinder implements OGBinder {

    @Override
    public Object onLoad(ObjectGraphDb db, ODocument sourceVertex) {
        String maleId = Utils.getRecordId(sourceVertex);
        String q = "select expand(out('" + HasPet.class.getSimpleName() + "')[favourite=true]) from " + maleId;
//        Log.debug(q);
        List<Pet> res = db.query(new OSQLSynchQuery(q));
        return res.isEmpty() ? null : res.get(0);
    }

    @Override
    public Object onSave(ObjectGraphDb db, ODocument sourceVertex, Object newValue) {
        if (newValue == null) {
            return null;
        }
        //any existing pets are not favourites...
        Pet p = db.save(newValue);
        String maleId = Utils.getRecordId(sourceVertex), petId = p.getId(), ownerId = Utils.getRecordId(p.getOwnerId());
        String q;
        if (ownerId == null) {
            q = "create Edge " + HasPet.class.getSimpleName() + " from " + maleId + " TO " + petId;
//            Log.debug(q);
            db.command(new OCommandSQL(q)).execute();
        } else if (!Objects.equal(maleId, ownerId)) {
            Log.error("this is not the owner of this pet!!!");
            return null;
        }
        q = "update ("
                + "select expand(out('" + HasPet.class.getSimpleName() + "')) from " + maleId
                + ") set favourite = eval('@rid == " + petId + "')";
//        Log.debug(q);
        db.command(new OCommandSQL(q)).execute();
        return db.reload(p);
    }

    @Override
    public Object onDelete(ObjectGraphDb db, ODocument source, Object value) {
        if (Utils.idNotNull(value)) {
            //delete the pet/orphan removal
            db.delete(value);
        }
        return null;
    }

}
