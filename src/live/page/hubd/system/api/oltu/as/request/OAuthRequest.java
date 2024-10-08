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

package live.page.hubd.system.api.oltu.as.request;

import jakarta.servlet.http.HttpServletRequest;
import live.page.hubd.system.api.oltu.common.OAuth;
import live.page.hubd.system.api.oltu.common.exception.OAuthProblemException;
import live.page.hubd.system.api.oltu.common.exception.OAuthSystemException;
import live.page.hubd.system.api.oltu.common.utils.OAuthUtils;
import live.page.hubd.system.api.oltu.common.validators.OAuthValidator;
import live.page.hubd.system.utils.Fx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Abstract OAuth request for the Authorization server.
 */
public abstract class OAuthRequest {

    protected HttpServletRequest request;
    protected OAuthValidator<HttpServletRequest> validator;
    protected Map<String, Class<? extends OAuthValidator<HttpServletRequest>>> validators =
            new HashMap<>();

    public OAuthRequest(HttpServletRequest request) throws OAuthSystemException, OAuthProblemException {
        this.request = request;
        validate();
    }

    public OAuthRequest() {
    }

    protected void validate() throws OAuthSystemException, OAuthProblemException {
        try {
            validator = initValidator();
            validator.validateMethod(request);
            validator.validateContentType(request);
            validator.validateRequiredParameters(request);
            validator.validateClientAuthenticationCredentials(request);
        } catch (OAuthProblemException e) {
            try {
                String redirectUri = request.getParameter(OAuth.OAUTH_REDIRECT_URI);
                if (!OAuthUtils.isEmpty(redirectUri)) {
                    e.setRedirectUri(redirectUri);
                }
            } catch (Exception ex) {
                Fx.log("Cannot read redirect_url from the request: {}");

            }

            throw e;
        }

    }

    protected abstract OAuthValidator<HttpServletRequest> initValidator() throws OAuthProblemException,
            OAuthSystemException;

    public String getParam(String name) {
        return request.getParameter(name);
    }

    public String getClientId() {
        String[] creds = OAuthUtils.decodeClientAuthenticationHeader(request.getHeader(OAuth.HeaderType.AUTHORIZATION));
        if (creds != null) {
            return creds[0];
        }
        return getParam(OAuth.OAUTH_CLIENT_ID);
    }

    public String getRedirectURI() {
        return getParam(OAuth.OAUTH_REDIRECT_URI);
    }

    public String getClientSecret() {
        String[] creds = OAuthUtils.decodeClientAuthenticationHeader(request.getHeader(OAuth.HeaderType.AUTHORIZATION));
        if (creds != null) {
            return creds[1];
        }
        return getParam(OAuth.OAUTH_CLIENT_SECRET);
    }

    /**
     * @return
     */
    public boolean isClientAuthHeaderUsed() {
        return OAuthUtils.decodeClientAuthenticationHeader(request.getHeader(OAuth.HeaderType.AUTHORIZATION)) != null;
    }

    public Set<String> getScopes() {
        String scopes = getParam(OAuth.OAUTH_SCOPE);
        return OAuthUtils.decodeScopes(scopes);
    }

}
