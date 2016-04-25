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
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlRootElement;
import jpatest.edge.FriendTo;

/**
 *
 * @author Oliver Fleetwood
 */
@XmlRootElement
@Access(AccessType.FIELD)
public class Human {

    @Id
    protected String id;

    protected String name;

    @OEdgeRelation(outGoing = true, edgeClass = FriendTo.class, saveable = false)
    protected transient List<Human> friends;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Human> getFriends() {
        return friends;
    }

    public void setFriends(List<Human> friends) {
        this.friends = friends;
    }
}
