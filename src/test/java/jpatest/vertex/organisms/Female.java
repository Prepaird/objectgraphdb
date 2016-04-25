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
import com.prepaird.objectgraphdb.edgefactories.ManyToOneEdgeFactory;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.xml.bind.annotation.XmlRootElement;
import jpatest.edge.DaugtherOfMother;

/**
 *
 * @author Oliver Fleetwood
 */
@XmlRootElement
@Access(AccessType.FIELD)
public class Female extends Human {

    @OEdgeRelation(outGoing = true, edgeClass = DaugtherOfMother.class, edgeFactory = ManyToOneEdgeFactory.class)
    protected transient Human mother;
}
