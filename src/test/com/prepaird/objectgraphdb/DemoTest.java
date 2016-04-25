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

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.prepaird.objectgraphdb.utils.Log;
import com.prepaird.objectgraphdb.utils.Utils;
import java.io.IOException;
import java.util.*;
import jpatest.vertex.organisms.Male;
import jpatest.vertex.organisms.Pet;
import org.junit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import testhelpers.ConnectionHelper;

/**
 *
 * @author Oliver Fleetwood
 */
public class DemoTest {

    private static ObjectGraphDb db;

    List<Object> createdVertexIDs = new LinkedList<>();

    public DemoTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        db = ConnectionHelper.getDb();
        OrientDbConnector.registerEntityClasses(db, "jpatest");
    }

    @AfterClass
    public static void tearDownClass() {
        db.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
        String q = "delete vertex from (select from " + Utils.escapeRecordIds(createdVertexIDs) + ")";
//        Log.debug(q);
        db.command(new OCommandSQL(q)).execute();
    }

    /**
     * Test of main method, of class ObjectGraphDb.
     */
    @Test
    public void maleDemo() throws Exception {

        //create one male
        Male pete = new Male();
        String peteName = "pete" + UUID.randomUUID().toString();
        pete.setName(peteName);
        Log.debug("Saving a male named " + peteName);
        pete = db.save(pete);
        Log.debug(pete.getName());
        Assert.assertNotNull(pete.getId());
        createdVertexIDs.add(pete.getId());
        Log.debug("fetching " + peteName + " from query");
        pete = ((List<Male>) db.query(new OSQLSynchQuery("select from Human where @rid = ?"), pete.getId())).iterator().next();
        Assert.assertEquals(peteName, pete.getName());
        //create another male
        Male joe = new Male();
        joe.setName("joe" + UUID.randomUUID().toString());
        joe.setFather(pete);
        Log.debug("Saving a male named " + joe.getName());
        joe = db.save(joe);
        Assert.assertNotNull(joe.getId());
        createdVertexIDs.add(joe.getId());
        Assert.assertEquals(pete.getId(), joe.getFather().getId());
        pete = db.reload(pete);
        assertEquals(1, pete.getSons().size());
        Assert.assertEquals(joe.getId(), Utils.getRecordId(pete.getSons().iterator().next()));
        //make sure that the loaded object is of correct class
        assertTrue(
                "Expected instance of " + Male.class.getSimpleName() + ", got " + joe.getFather().getClass().getSimpleName(),
                Male.class.isAssignableFrom(joe.getFather().getClass())
        );
        Log.debug(joe);
        Log.debug(pete);
//        joe = db.reload(joe);
//        db.query(new OSQLSynchQuery<>("select from " + joe.getId()));
        joe = db.save(joe); //save again

        //add another child...
        Male dave = new Male();
        dave.setName("Dave" + UUID.randomUUID().toString());
        dave.setFather(pete);
        Log.debug("Saving a male named " + dave.getName());
        dave.setFavouritePet(new Pet("dog"));
        dave = db.save(dave);
        createdVertexIDs.add(dave.getId());
        Pet dogPet = dave.getFavouritePet();
        createdVertexIDs.add(dogPet.getId());
        Log.debug(dogPet);
        assertNotNull(dogPet);
        assertNotNull(dogPet.getId());
        assertEquals(dave.getId(), Utils.getRecordId(dogPet.getOwnerId()));
        assertEquals("dog", dogPet.getSpecies());

        //save again
        dave = db.save(dave);
        //change pet and save again
        dave.setFavouritePet(new Pet("cat"));
        dave = db.save(dave);
        dave = db.reload(dave);
        Pet catPet = dave.getFavouritePet();
        createdVertexIDs.add(catPet.getId());

        assertNotNull(catPet);
        Assert.assertNotSame(dogPet.getId(), catPet.getId());
        assertEquals(dave.getId(), Utils.getRecordId(catPet.getOwnerId()));
        assertEquals("cat", catPet.getSpecies());

        Log.debug((Male) db.reload(dave));

        pete = db.reload(pete);
        assertEquals(2, pete.getSons().size());
        //bye bye pete
        db.delete(pete);
        joe = db.reload(joe);
        assertNull(joe.getFather());
    }

}
