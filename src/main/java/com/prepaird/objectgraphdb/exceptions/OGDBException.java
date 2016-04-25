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
package com.prepaird.objectgraphdb.exceptions;

/**
 * Class for used for throwin HTTP-like error messages with text and error code
 *
 * @author Oliver Fleetwood
 */
public class OGDBException extends HTTPStatusException {

    private String errorMessage = null;

    public OGDBException(int statusCode, String errorMessage) {
        super(statusCode, errorMessage);
        this.errorMessage = errorMessage;
    }

    public OGDBException(Integer statusCode) {
        super(statusCode);
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
