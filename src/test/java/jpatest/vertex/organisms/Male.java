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
import com.prepaird.objectgraphdb.annotations.OGRelation;
import com.prepaird.objectgraphdb.edgefactories.ManyToOneEdgeFactory;
import java.util.Collection;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.xml.bind.annotation.XmlRootElement;
import jpatest.edge.SonOfFather;
import testhelpers.BrotherBinder;
import testhelpers.PetBinder;

/**
 *
 * @author Oliver Fleetwood
 */
@XmlRootElement
@Access(AccessType.FIELD)
public class Male extends Human {

    @OEdgeRelation(outGoing = true, edgeClass = SonOfFather.class, edgeFactory = ManyToOneEdgeFactory.class, saveable = false)
    protected transient Human father;

    @OEdgeRelation(outGoing = false, edgeClass = SonOfFather.class, readOnly = true, idOnly = true)
    protected transient List<Object> sons;

    @OGRelation(binder = BrotherBinder.class)
    protected transient Collection<Object> brotherIds;

    @OGRelation(binder = PetBinder.class)
    protected transient Pet favouritePet;

    public Human getFather() {
        return father;
    }

    public void setFather(Human father) {
        this.father = father;
    }

    public List<Object> getSons() {
        return sons;
    }

    public void setSons(List<Object> sons) {
        this.sons = sons;
    }

    public Collection<Object> getBrotherIds() {
        return brotherIds;
    }

    public void setBrotherIds(Collection<Object> brotherIds) {
        this.brotherIds = brotherIds;
    }

    public Pet getFavouritePet() {
        return favouritePet;
    }

    public void setFavouritePet(Pet favouritePet) {
        this.favouritePet = favouritePet;
    }

}
