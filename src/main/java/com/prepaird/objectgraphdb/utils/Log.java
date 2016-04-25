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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.prepaird.objectgraphdb.Config;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Oliver Fleetwood
 */
public class Log {

    public static final String LOGGER_NAME = "OBJECT_GRAPH_DB";

    /**
     * set this to true for debugging.
     */
    protected static boolean DEBUG = true;
    private static ObjectMapper defaultMapper = null;

    /**
     * write any string to this function to debug. this function should not do
     * anything on the server.
     *
     * @param s
     */
    public static synchronized void debug(String s) {
        debug(s, null, null, null);
    }

    public static synchronized void debug(String s, Object referer, String method, Level logLevel) {
        if (DEBUG) {
            String refClass = referer != null ? referer.getClass().getName() : Utils.class.getName();
            Logger.getLogger(LOGGER_NAME).logp(logLevel == null ? Config.defaultLogLevel : logLevel, refClass, method != null ? method : "debug", s);
            //TODO do high performance logging. see for example http://www.rgagnon.com/javadetails/java-0603.html
        }
    }

    public static void disableDebugging() {
        DEBUG = false;
    }

    public static void enableDebugging() {
        DEBUG = true;
    }

    /**
     * write any object by serializing it to json if possible, otherwise using
     * the toString() method.
     *
     * @param o
     */
    public static void debug(Object o) {
        if (DEBUG) {
            String s;
            try {
                if (o instanceof ODocument) {
                    s = ((ODocument) o).toJSON();
                } else if (o instanceof OIdentifiable) {
                    s = ((OIdentifiable) o).getIdentity().toString();
                } else { //convert any document ot json
                    ObjectWriter ow = getObjectWriter();
                    s = ow.writeValueAsString(o);
                }
            } catch (Exception ex) {
                s = o.toString();
                Log.error(ex);
            }
            debug(s);
        }
    }

    public static void error(Throwable e) {
        Logger.getLogger(LOGGER_NAME).log(Level.SEVERE, null, e);
    }

    public static void error(String e) {
        Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, e);
    }

    public static boolean isDEBUG() {
        return DEBUG;
    }

    public static void setDEBUG(boolean aDEBUG) {
        DEBUG = aDEBUG;
    }

    private static synchronized ObjectWriter getObjectWriter() {
        if (defaultMapper == null) {
            defaultMapper = new ObjectMapper();
            //Set ignoreUnknown by default
            defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            defaultMapper.addMixInAnnotations(Object.class, MixIn.class);
            SimpleModule module = new SimpleModule();
            module.addSerializer(ORecordId.class, new ORecordIdSerializer());
            module.addSerializer(ODocument.class, new ODocumentSerializer());
            defaultMapper.registerModule(module);

            //mapper.addHandler(null)
            defaultMapper.setVisibilityChecker(defaultMapper.getSerializationConfig().getDefaultVisibilityChecker()
                    .withFieldVisibility(JsonAutoDetect.Visibility.NONE)
                    .withGetterVisibility(JsonAutoDetect.Visibility.ANY)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.ANY)
                    .withSetterVisibility(JsonAutoDetect.Visibility.ANY)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));
        }

        return defaultMapper.writer().withDefaultPrettyPrinter();
    }

    @JsonIgnoreProperties(ignoreUnknown = true, value = {"handler", "owner", "identity", "@fieldTypes", "@version", "classes", "version", "conflictStrategy"})
    abstract class MixIn {
    }

    static class ORecordIdSerializer extends JsonSerializer<ORecordId> {

        @Override
        public void serialize(ORecordId t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            String json = t == null ? null : t.toString();//"#" + t.getClusterId() + ":" + t.getClusterPosition();
            jg.writeObject(json);
        }

    }

    static class ODocumentSerializer extends JsonSerializer<ODocument> {

        @Override
        public void serialize(ODocument t, JsonGenerator jg, SerializerProvider sp) throws IOException, JsonProcessingException {
            //jg.writeStartArray();
            jg.writeStartObject();
            String json = t.toJSON("rid");
            //remove opening and closing brackets
            json = json.substring(1, json.length() - 1);
            jg.writeRaw(json);
            jg.writeEndObject();
            //jg.writeEndArray();
        }

    }
}
