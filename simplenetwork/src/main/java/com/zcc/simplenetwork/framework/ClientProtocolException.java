/*
 * $HeadURL: $
 * $Revision: $
 * $Date: $
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.zcc.simplenetwork.framework;

import java.io.IOException;

/**
 * Signals an error in the HTTP protocol.
 *
 * @deprecated Please use {@link java.net.URL#openConnection} instead.
 *     Please visit <a href="http://android-developers.blogspot.com/2011/09/androids-http-clients.html">this webpage</a>
 *     for further details.
 */
@Deprecated
public class ClientProtocolException extends IOException {
    
    private static final long serialVersionUID = -5596590843227115865L;

    public ClientProtocolException() {
        super();
    }

    public ClientProtocolException(String s) {
        super(s);
    }
    
    public ClientProtocolException(Throwable cause) {
        initCause(cause);
    }
    
    public ClientProtocolException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
    

}
