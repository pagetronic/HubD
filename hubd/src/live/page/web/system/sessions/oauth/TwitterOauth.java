/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions.oauth;

import live.page.web.content.users.UsersUtils;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

public class TwitterOauth {

	private static final String authorize = "https://api.twitter.com/oauth/authorize";
	private static final String request_token = "https://api.twitter.com/oauth/request_token";
	private static final String access_token = "https://api.twitter.com/oauth/access_token";
	private static final String verify_credentials = "https://api.twitter.com/1.1/account/verify_credentials.json";
	private static final String oauth_consumer_key = Settings.TWITTER_OAUTH_CLIENT_ID;
	private static final String oauth_consumer_secret = Settings.TWITTER_OAUTH_CLIENT_SECRET;

	public static String getAuthorize() {

		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			String uuid_string = UUID.randomUUID().toString().replaceAll("-", "");
			HttpPost httpPost = new HttpPost(request_token);
			httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			String oauth_timestamp = String.valueOf(System.currentTimeMillis() / 1000);
			String parameter_string = "oauth_consumer_key=" + oauth_consumer_key + "&oauth_nonce=" + uuid_string + "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=" + oauth_timestamp + "&oauth_version=1.0";
			String signature_base_string = "POST&" + encode(request_token) + "&" + encode(parameter_string);
			String oauth_signature = computeSignature(signature_base_string, oauth_consumer_secret + "&");
			String authorization_header_string = "OAuth oauth_consumer_key=\"" + oauth_consumer_key + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp + "\",oauth_nonce=\"" + uuid_string + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\"";
			httpPost.addHeader("Authorization", authorization_header_string);
			CloseableHttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);
			EntityUtils.consume(entity);

			StringTokenizer st = new StringTokenizer(responseBody, "&");
			while (st.hasMoreTokens()) {
				String currenttoken = st.nextToken();
				if (currenttoken.startsWith("oauth_token=")) {
					return authorize + "?oauth_token=" + currenttoken.substring(currenttoken.indexOf("=") + 1);
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	public static Json getUserOauth(String oauth_token, String oauth_verifier) {

		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			String uuid_string = UUID.randomUUID().toString().replaceAll("-", "");
			HttpPost httpPost = new HttpPost(access_token);
			httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
			String oauth_timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
			String parameter_string = "oauth_consumer_key=" + oauth_consumer_key + "&oauth_nonce=" + uuid_string + "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=" + oauth_timestamp + "&oauth_version=1.0";

			String oauth_signature = computeSignature(parameter_string, oauth_consumer_secret + "&");

			String authorization_header_string = "OAuth oauth_consumer_key=\"" + oauth_consumer_key + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp + "\",oauth_nonce=\"" + uuid_string + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(oauth_token) + "\"";

			httpPost.addHeader("Authorization", authorization_header_string);
			List<NameValuePair> nvps = new ArrayList<>();
			nvps.add(new BasicNameValuePair("oauth_verifier", encode(oauth_verifier)));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));

			CloseableHttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			EntityUtils.consume(entity);
			String responseBody = EntityUtils.toString(response.getEntity());
			response.close();

			String access_token = "";
			String access_token_secret = "";

			StringTokenizer st = new StringTokenizer(responseBody, "&");
			String currenttoken = "";
			while (st.hasMoreTokens()) {
				currenttoken = st.nextToken();
				if (currenttoken.startsWith("oauth_token=")) {
					access_token = currenttoken.substring(currenttoken.indexOf("=") + 1);
				} else if (currenttoken.startsWith("oauth_token_secret=")) {
					access_token_secret = currenttoken.substring(currenttoken.indexOf("=") + 1);
				}
			}
			return verifyCredentials(access_token, access_token_secret);
		} catch (Exception e) {
		} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
			}
		}
		return null;

	}

	private static Json verifyCredentials(String access_token, String access_token_secret) {

		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		try {
			HttpGet httpGet = new HttpGet(verify_credentials + "?skip_status=true&include_email=true");
			String uuid_string = UUID.randomUUID().toString().replaceAll("-", "");
			String oauth_timestamp = String.valueOf(System.currentTimeMillis() / 1000);
			String parameter_string = "include_email=true&oauth_consumer_key=" + oauth_consumer_key + "&oauth_nonce=" + uuid_string + "&oauth_signature_method=HMAC-SHA1" + "&oauth_timestamp=" + oauth_timestamp + "&oauth_token=" + encode(access_token) + "&oauth_version=1.0&skip_status=true";
			String signature_base_string = "GET&" + encode(verify_credentials) + "&" + encode(parameter_string) + "";
			String oauth_signature = computeSignature(signature_base_string, oauth_consumer_secret + "&" + encode(access_token_secret));
			String authorization_header_string = "OAuth oauth_consumer_key=\"" + oauth_consumer_key + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + oauth_timestamp + "\",oauth_nonce=\"" + uuid_string + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oauth_signature) + "\",oauth_token=\"" + encode(access_token) + "\"";

			httpGet.addHeader("Authorization", authorization_header_string);
			CloseableHttpResponse response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
			response.close();
			Json user_twitter = new Json(responseBody);
			if (user_twitter.keySet().size() == 0) {
				return null;
			} else {
				Json user = new Json();
				user.put("id", user_twitter.getString("id_str"));
				user.put("name", UsersUtils.uniqueName(user_twitter.getString("screen_name")));
				user.put("verified", (user_twitter.getString("email") != null));
				user.put("email", user_twitter.getString("email"));
				String avatar = user_twitter.getString("profile_image_url");
				if (avatar != null) {
					avatar = avatar.replace("_normal", "");
				}
				user.put("avatar", avatar);
				return user;
			}
		} catch (Exception e) {
		} finally {
			try {
				httpclient.close();
			} catch (Exception e) {
			}
		}
		return null;
	}

	private static String encode(String value) {
		String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);

		StringBuilder buf = new StringBuilder(encoded.length());
		char focus;
		for (int i = 0; i < encoded.length(); i++) {
			focus = encoded.charAt(i);
			if (focus == '*') {
				buf.append("%2A");
			} else if (focus == '+') {
				buf.append("%20");
			} else if ((focus == '%') && ((i + 1) < encoded.length()) && (encoded.charAt(i + 1) == '7') && (encoded.charAt(i + 2) == 'E')) {
				buf.append('~');
				i += 2;
			} else {
				buf.append(focus);
			}
		}
		return buf.toString();
	}

	private static String computeSignature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException {
		SecretKey secretKey = null;
		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKey);
		byte[] text = baseString.getBytes();
		return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
	}
}
