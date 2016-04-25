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
package com.prepaird.objectgraphdb.queryrunners;

import com.prepaird.objectgraphdb.ObjectGraphDb;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Interface for custom binding between a source with this annotation on
 * other database objects.
 *
 * @author Oliver Fleetwood
 */
public interface OGBinder {

    /**
     * Executed when the source is loaded
     *
     * @param db
     * @param source
     * @return the field value
     */
    public Object onLoad(ObjectGraphDb db, ODocument source);

    /**
     * Executed when the source is saved.
     *
     * @param db
     * @param source
     * @param newValue
     * @return the field value
     */
    public Object onSave(ObjectGraphDb db, ODocument source, Object newValue);

    /**
     * Executed when the source is deleted
     *
     * @param db
     * @param source
     * @param value  the current value of the field
     * @return the field value
     */
    public Object onDelete(ObjectGraphDb db, ODocument source, Object currentValue);

}
