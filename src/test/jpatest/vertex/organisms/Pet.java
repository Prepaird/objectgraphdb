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
package jpatest.vertex.organisms;

import com.prepaird.objectgraphdb.annotations.OEdgeRelation;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Id;
import jpatest.edge.HasPet;

/**
 *
 * @author Oliver Fleetwood
 */
@Access(AccessType.FIELD)
public class Pet {

    @Id
    protected String id;

    protected String species;

    @OEdgeRelation(outGoing = false, edgeClass = HasPet.class, readOnly = true, idOnly = true)
    protected Object ownerId;

    public Pet() {

    }

    public Pet(String species) {
        this.species = species;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Object getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Object ownerId) {
        this.ownerId = ownerId;
    }
}
