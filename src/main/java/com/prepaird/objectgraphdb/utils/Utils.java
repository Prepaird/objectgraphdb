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
package com.prepaird.objectgraphdb.utils;

import com.prepaird.objectgraphdb.ObjectGraphDb;
import com.prepaird.objectgraphdb.exceptions.OGDBException;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.object.enhancement.OObjectEntitySerializer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author Oliver Fleetwood
 */
public class Utils {

    public static String getRecordId(Object o) {
        return getRecordId(o, null);
    }

    public static String getRecordId(Object o, ObjectGraphDb db) {
        if (o == null) {
            return null;
        }
        String id;
        //TODO: check the inheritance below. We probably only need to look for OIdentifiable
        if (o instanceof ORID) {
            id = ((ORID) o).toString();
        } else if (o instanceof ODocument) {
            id = ((ODocument) o).getIdentity().toString();
        } else if (o instanceof OIdentifiable) {
            id = ((OIdentifiable) o).getIdentity().toString();
        } else if (o instanceof String) {
            id = (String) o;
        } else {
            //see if there is a field with the @Id annotation. Otherwise return null,
            Field f = OObjectEntitySerializer.getIdField(o.getClass());
            if (f == null) {
                if (db != null) {
                    Object record = db.getRecordByUserObject(o, false);
                    return getRecordId(record, null);
                } else {
//                    Log.debug("no @Id field found on object of class " + o.getClass());
                    return null;
                }
            }
            try {
                Method getterMethod = getGetterMethod(o.getClass(), f);
                Object returnedId = getterMethod.invoke(o);
//                Object orid = OObjectEntitySerializer.getFieldValue(f, o);
                return getRecordId(returnedId);
            } catch (Exception ex) {
                Log.error(ex);
                return null;
            }
        }
        if (!isValidRecordId(id)) {
            Log.error("found invalid record id, " + id + ", returning null instead");
            id = null;
        }
        return id;
    }

    public static boolean isValidRecordId(String id) {
        return id != null && id.matches("^#-?[0-9]*:-?[0-9]*$");
    }

    public static String escapeRecordId(String s) throws OGDBException {
        if (isValidRecordId(s)) {
            return s;
        }
        throw new OGDBException(400, "Could not escape record id. Invalid record id: " + s);
    }

    public static String escapeRecordId(Object o, ObjectGraphDb db) throws OGDBException {
        if (o == null) {
            throw new OGDBException(400, "Invalid record id. Object is null.");
        }
        return escapeRecordId(getRecordId(o, db));
    }

    public static String escapeRecordId(Object o) throws OGDBException {
        return escapeRecordId(o, null);
    }

    public static String escapeRecordIds(Collection coll) throws OGDBException {
        String s = "[";
        boolean elementfound = false;
        for (Object o : coll) {
            if (elementfound) {
                s += ",";
            } else {
                elementfound = true;
            }
            s += escapeRecordId(o);
        }
        return s + "]";
    }

    public static Method getGetterMethod(Class cl, Field f) {
        String getterName = "get" + WordUtils.capitalize(f.getName()),
                isName = "is" + WordUtils.capitalize(f.getName());
        //find the getter method and call it
        Method getterMethod = getMethod(cl, getterName, null),
                isMethod = getMethod(cl, isName, null);
        return isMethod != null ? isMethod : getterMethod;
    }

    public static Method getSetterMethod(Class cl, Field f) {
        Class[] params = {getGetterMethod(cl, f).getReturnType()};
        return getMethod(cl, getSetterName(f), params);
    }

    public static String getSetterName(Field f) {
        return "set" + WordUtils.capitalize(f.getName());
    }

    public static Method getMethod(Class cl, String methodName, Class[] params) {
        try {
            return cl.getMethod(methodName, params);
        } catch (NoSuchMethodException ex) {
            //never mind
        } catch (SecurityException ex) {
            Log.error(ex);
        }
        return null;
    }

    /**
     * check if the object is null or its id is null
     *
     * @param value
     * @return
     */
    public static boolean idNotNull(Object value) {
        return value != null && getRecordId(value) != null;
    }
}
