/**
 * Copyright 2010 Newcastle University
 * <p>
 * http://research.ncl.ac.uk/smart/
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package live.page.hubd.system.api.oltu.common.parameters;

import live.page.hubd.system.api.oltu.common.exception.OAuthSystemException;
import live.page.hubd.system.api.oltu.common.message.OAuthMessage;
import live.page.hubd.system.api.oltu.common.utils.OAuthUtils;

import java.util.Map;

/**
 *
 */
public class BodyURLEncodedParametersApplier implements OAuthParametersApplier {

    public OAuthMessage applyOAuthParameters(OAuthMessage message, Map<String, Object> params)
            throws OAuthSystemException {

        String body = OAuthUtils.format(params.entrySet(), "UTF-8");
        message.setBody(body);
        return message;

    }
}
