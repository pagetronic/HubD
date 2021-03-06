/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions.oauth;

import live.page.web.system.Settings;

public enum OauthDatas {

	google("google", Settings.GOOGLE_OAUTH_CLIENT_ID, Settings.GOOGLE_OAUTH_CLIENT_SECRET, "profile email", "https://accounts.google.com/o/oauth2/auth", "https://www.googleapis.com/oauth2/v3/token", "https://www.googleapis.com/oauth2/v2/userinfo", "&prompt=select_account"),
	facebook("facebook", Settings.FACEBOOK_OAUTH_CLIENT_ID, Settings.FACEBOOK_OAUTH_CLIENT_SECRET, "email", "https://graph.facebook.com/oauth/authorize", "https://graph.facebook.com/oauth/access_token", "https://graph.facebook.com/me?fields=id,email,picture,name,first_name,last_name,locale,timezone,verified", "&auth_type=reauthenticate"),
	live("live", Settings.LIVE_OAUTH_CLIENT_ID, Settings.LIVE_OAUTH_CLIENT_SECRET, "wl.emails", "https://login.live.com/oauth20_authorize.srf", "https://login.live.com/oauth20_token.srf", "https://apis.live.net/v5.0/me", "");

	private final String provider;
	private final String client_id;
	private final String client_secret;
	private final String scope;
	private final String authorize;
	private final String token;
	private final String userinfo;
	private final String parameters;

	OauthDatas(String provider, String client_id, String client_secret, String scope, String authorize, String token, String userinfo, String parameters) {
		this.provider = provider;
		this.client_id = client_id;
		this.client_secret = client_secret;
		this.scope = scope;
		this.authorize = authorize;
		this.token = token;
		this.userinfo = userinfo;
		this.parameters = parameters;
	}

	public String getClient_id() {
		return client_id;
	}

	public String getClient_secret() {
		return client_secret;
	}

	public String getAuthorize() {
		return authorize + "?scope=" + getScope() + "&response_type=code&redirect_uri=" + Settings.getFullHttp() + "/oauth" + "&client_id=" + getClient_id() + getParameters();
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
