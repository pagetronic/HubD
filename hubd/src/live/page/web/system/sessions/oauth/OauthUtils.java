/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions.oauth;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.blobs.BlobsUtils;
import live.page.web.content.posts.utils.DiscussAdmin;
import live.page.web.content.users.UsersUtils;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.BaseSession;
import live.page.web.system.sessions.UsersBase;
import live.page.web.utils.Fx;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class OauthUtils {

	public static void requestOauth(WebServletRequest req, WebServletResponse resp) throws IOException {

		resp.addHeader(HttpHeaders.VARY, HttpHeaders.REFERER);
		Json session = BaseSession.getOrCreateSession(req, resp);
		String provider = req.getQueryString().replaceAll("^(Google|Facebook|Twitter|Live).*", "$1").toLowerCase();

		if (req.getString("client_id", null) != null) {
			Json app = Db.find("ApiApps", Filters.eq("client_id", req.getString("client_id", ""))).first();
			if (app != null) {
				session.put("app_id", app.getId());
			} else {
				resp.sendError(401, "INVALID_APP");
				return;
			}
		}


		if (req.getHeader(HttpHeaders.REFERER) != null) {
			session.put("referer", req.getHeader(HttpHeaders.REFERER));
		}
		if (provider.equals("twitter")) {
			String urlOauth = TwitterOauth.getAuthorize();
			session.put("provider", provider);
			Db.save("Sessions", session);
			resp.sendRedirect(urlOauth);
		} else {
			String urlOauth = OauthDatas.valueOf(provider).getAuthorize();
			session.put("provider", provider);
			Db.save("Sessions", session);
			resp.sendRedirect(urlOauth);
		}
	}

	public static void validateOauth(WebServletRequest req, WebServletResponse resp) throws IOException {


		Json session = BaseSession.getOrCreateSession(req, resp);

		String provider = session.getString("provider");
		if (provider == null) {
			resp.sendRedirect("/oauth");
			return;
		}
		Json user_oauth;
		if (provider.equals("twitter")) {
			user_oauth = TwitterOauth.getUserOauth(req.getParameter("oauth_token"), req.getParameter("oauth_verifier"));
		} else {
			user_oauth = OauthUtils.getUserOauth(req.getParameter("code"), provider);
		}
		if (user_oauth == null || user_oauth.get("error") != null || user_oauth.get("name") == null || user_oauth.get("id") == null) {
			resp.sendRedirect("/oauth");
			return;
		}

		Json user = null;
		Object id = user_oauth.get("id");
		if (id != null) {
			if (user_oauth.getString("email") != null) {
				user = Db.find("Users", Filters.eq("email", user_oauth.getString("email", Fx.getUnique()))).sort(Sorts.descending("join")).first();
			}
			if (user == null) {
				user = Db.find("Users",
						Filters.and(Filters.eq("providers.id", id), Filters.eq("providers.provider", session.getString("provider")))
				).first();
			}
		}

		if (user == null && session.getJson("user") != null) {
			user = session.getJson("user");
		}

		if (user == null) {
			user = UsersBase.getBase();
		}

		List<Json> providers = user.getListJson("providers");
		if (providers == null) {
			user.add("providers", user_oauth);
		} else {
			for (int i = 0; i < providers.size(); i++) {
				Json prov = providers.get(i);
				if (prov.getString("provider", "").equals(provider) && prov.getString("id", "").equals(user_oauth.getString("id"))) {
					providers.set(i, user_oauth);
					user.put("providers", providers);
					break;
				}
			}
		}
		Date date = new Date();

		if (user.getString("name", "").equals("")) {
			user.put("name", UsersUtils.uniqueName(user_oauth.getString("name")));
		}

		if (user.get("lng") == null || user.getString("lng").equals("")) {
			user.put("lng", req.getLng());
		}
		if (user.get("join") == null) {
			user.put("join", date);
		}

		user.put("last", date);

		user.put("email", user_oauth.getString("email"));

		user.remove("key");

		Db.save("Users", user);

		if (!user_oauth.getString("avatar", "").equals("") && user.getString("avatar", "").equals("")) {
			String avatar_id = BlobsUtils.downloadToDb(user_oauth.getString("avatar"));
			if (avatar_id != null) {
				user.put("avatar", avatar_id);
				Db.updateOne("Users", Filters.eq("_id", user.getId()), new Json("$set", new Json("avatar", avatar_id)));
			}
		}

		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("sysid")) {
					DiscussAdmin.updateSysId(cookie.getValue(), user.getId());
					cookie.setMaxAge(0);
					resp.addCookie(cookie);
					break;
				}
			}

		}

		session.put("user", user.getId());

		Db.save("Sessions", session);

		if (session.containsKey("referer")) {
			resp.sendRedirect(session.getString("referer"));
		} else {
			resp.sendRedirect("/");
		}
	}

	public static Json getUserOauth(String code, String provider) throws ParseException, IOException {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			HttpPost httpPost = new HttpPost(OauthDatas.valueOf(provider).getToken());

			httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			String authorization = Base64.getEncoder().encodeToString((OauthDatas.valueOf(provider).getClient_id() + ":" + OauthDatas.valueOf(provider).getClient_secret()).getBytes(StandardCharsets.UTF_8));
			httpPost.addHeader("Authorization", "Basic " + authorization);
			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("code", code));
			nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
			nvps.add(new BasicNameValuePair("scope", OauthDatas.valueOf(provider).getScope()));
			nvps.add(new BasicNameValuePair("client_id", OauthDatas.valueOf(provider).getClient_id()));
			nvps.add(new BasicNameValuePair("client_secret", OauthDatas.valueOf(provider).getClient_secret()));
			nvps.add(new BasicNameValuePair("redirect_uri", Settings.getFullHttp() + "/oauth"));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));

			CloseableHttpResponse response = httpclient.execute(httpPost);

			String access_token = null;

			try {
				HttpEntity entity = response.getEntity();
				String res = EntityUtils.toString(entity);
				EntityUtils.consume(entity);
				EntityUtils.consume(entity);
				if (res.contains("access_token=")) {
					String[] ress = res.split("&");
					for (String res2 : ress) {
						String[] resss = res2.split("=");
						if (resss[0].equals("access_token")) {
							access_token = resss[1];
						}
					}
					if (access_token == null) {
						Fx.log(res);
						return null;
					}
				} else {
					Json apiresp = new Json(res);
					access_token = apiresp.getText("access_token");

					if (access_token == null) {
						Fx.log(apiresp);
						return null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				response.close();
			}

			HttpGet userinfoRequest;

			if (provider.equals("live")) {

				String url = OauthDatas.valueOf(provider).getUserinfo();
				url += "?access_token=" + access_token;
				userinfoRequest = new HttpGet(url);

			} else {
				userinfoRequest = new HttpGet(OauthDatas.valueOf(provider).getUserinfo());
				userinfoRequest.setHeader("Authorization", "Bearer " + access_token + "");
				userinfoRequest.setHeader("Content-Type", "application/json");
			}
			response = httpclient.execute(userinfoRequest);
			try {
				HttpEntity entity = response.getEntity();
				String res = EntityUtils.toString(entity);
				EntityUtils.consume(entity);
				Json user_json = new Json(res);
				Json user = new Json();

				switch (provider) {
					case "google":

						user.put("id", user_json.getId());
						user.put("name", user_json.getString("name"));
						user.put("verified", user_json.getBoolean("verified_email", false));
						user.put("email", user_json.getString("email"));
						user.put("avatar", user_json.getString("picture"));

						break;
					case "facebook":

						user.put("id", user_json.getId());
						user.put("name", user_json.getString("name"));
						user.put("verified", user_json.getBoolean("verified", false));
						user.put("email", user_json.getString("email"));
						String avatar = null;
						Json avatar_data = user_json.getJson("picture");
						if (avatar_data != null && avatar_data.getJson("data") != null) {
							avatar = avatar_data.getJson("data").getString("url");
						}
						user.put("avatar", avatar);

						break;
					case "live":

						user.put("id", user_json.getId());
						user.put("name", user_json.getString("name"));
						user.put("verified", true);
						user.put("email", user_json.getJson("emails").getString("preferred"));
						user.put("avatar", "https://apis.live.net/v5.0/" + user_json.getId() + "/picture");
						break;
				}
				if (user.getString("name") == null || user.getString("name").equals("")) {
					user.put("name", "anonymous");
				}
				user.put("provider", provider);
				user.put("src", user_json);
				return user;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				response.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpclient.close();
		}
		return null;
	}
}
