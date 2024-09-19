/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions.oauth;

import live.page.hubd.system.Settings;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OauthDatas {


    private final String provider;
    private final String client_id;
    private final String client_secret;
    private final String scope;
    private final String authorize;
    private final String token;
    private final String userinfo;
    private final String parameters;


    public OauthDatas(String provider, String client_id, String client_secret, String scope, String authorize, String token, String userinfo, String parameters) {
        this.provider = provider;
        this.client_id = client_id;
        this.client_secret = client_secret;
        this.scope = scope;
        this.authorize = authorize;
        this.token = token;
        this.userinfo = userinfo;
        this.parameters = parameters;
    }

    public static OauthDatas google() {
        return new OauthDatas("google", Settings.GOOGLE_OAUTH_CLIENT_ID, Settings.GOOGLE_OAUTH_CLIENT_SECRET, "profile email", "https://accounts.google.com/o/oauth2/auth", "https://www.googleapis.com/oauth2/v3/token", "https://www.googleapis.com/oauth2/v2/userinfo", "&prompt=select_account");
    }

    public static OauthDatas meta() {
        return new OauthDatas("meta", Settings.META_OAUTH_CLIENT_ID, Settings.META_OAUTH_CLIENT_SECRET, "email", "https://graph.facebook.com/oauth/authorize", "https://graph.facebook.com/oauth/access_token", "https://graph.facebook.com/me?fields=id,email,picture,name,first_name,last_name,locale,timezone,verified", "&auth_type=reauthenticate");
    }

    public static OauthDatas valueOf(String provider) {
        return switch (provider) {
            case "google" -> google();
            case "meta" -> meta();
            default -> null;
        };
    }

    public String getName() {
        return provider;
    }

    public String getClient_id() {
        return client_id;
    }

    public String getClient_secret() {
        return client_secret;
    }

    public String getAuthorize() {
        return authorize + "?scope=" + getScope() + "&response_type=code&redirect_uri=" + URLEncoder.encode(Settings.getFullHttp() + "/oauth", StandardCharsets.UTF_8) + "&client_id=" + getClient_id() + getParameters();
    }

    public String getToken() {
        return token;
    }

    public String getUserinfo() {
        return userinfo;
    }

    public String getScope() {
        return scope;
    }

    public String getParameters() {
        return parameters;
    }
}
