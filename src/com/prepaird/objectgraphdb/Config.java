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

import java.util.logging.Level;

/**
 *
 * @author Oliver Fleetwood
 */
public class Config {

    protected static int TRANSACTION_RETRY_COUNT = 10;
    public static Level defaultLogLevel = Level.INFO;

    //---------------TESTING-----------------------
    public static String TEST_DB_URL = "remote:localhost/ogdb";

    public static int getTRANSACTION_RETRY_COUNT() {
        return TRANSACTION_RETRY_COUNT;
    }

    public static void setTRANSACTION_RETRY_COUNT(int aTRANSACTION_RETRY_COUNT) {
        if (aTRANSACTION_RETRY_COUNT < 0) {
            TRANSACTION_RETRY_COUNT = 0;
        } else {
            TRANSACTION_RETRY_COUNT = aTRANSACTION_RETRY_COUNT;
        }
    }

}
