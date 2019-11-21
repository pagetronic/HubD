/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.utils.json.Json;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Settings {

	public static final String HUB_REPO = "/data/repo/hubd";

	public static final String ADMIN_ID = "00000000000000000000000000";
	public static final long CTRL_PERIOD = 30L; // template and style cache in seconds

	public static final long START_COUNT = 1356048000000L; // Fri, 21 Dec 2012
	public static final int MAX_AGE = 31556930; // real one year 31556930.4

	public static final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10Mo

	public static final Map<String, String> CONTENT_TYPES = new HashMap<>();
	public static boolean PUBS = true;

	public static String SALT;
	public static long FLOOD_DELAY = 4000L;

	public static List<String> VALID_PARENTS = new ArrayList<>(Arrays.asList("Posts", "Forums", "Pages"));

	static {
		CONTENT_TYPES.put("pdf", "application/pdf");
		CONTENT_TYPES.put("png", "image/png");
		CONTENT_TYPES.put("jpg", "image/jpeg");
		CONTENT_TYPES.put("jpeg", "image/jpeg");
		CONTENT_TYPES.put("txt", "text/plain");
	}

	public static final Integer COOKIE_DELAY = 30 * 24 * 3600;
	public static final int CHUNCK_SIZE = 128 * 1024;


	public static String SITE_TITLE;
	public static String LOGO_TITLE;
	public static String UI_LOGO;


	public static String REPO;

	public static String HOST_HTTP;
	public static String HOST_API;
	public static String HOST_CDN;
	public static String HTTP_PROTO;
	public static String PROJECT_NAME;
	public static String COOKIE_NAME;
	public static Json LANGS_DOMAINS;
	public static String DB_USER;
	public static char[] DB_PASS;
	public static String DB_NAME;


	public static boolean MENU_FORUM = false;

	public static String SMTP_MAIL_USER;
	public static String SMTP_MAIL_PASSWD;

	public static String GOOGLE_SEARCH_API;
	public static String BING_SEARCH_API;


	public static String GCM_SENDER_ID;
	public static String GOOGLE_PUSH_KEY;

	public static String VAPID_PUB;
	public static String VAPID_PRIV;

	public static String GOOGLE_OAUTH_CLIENT_ID;
	public static String GOOGLE_OAUTH_CLIENT_SECRET;
	public static String FACEBOOK_OAUTH_CLIENT_ID;
	public static String FACEBOOK_OAUTH_CLIENT_SECRET;
	public static String LIVE_OAUTH_CLIENT_ID;
	public static String LIVE_OAUTH_CLIENT_SECRET;

	public static String TWITTER_OAUTH_CLIENT_ID;
	public static String TWITTER_OAUTH_CLIENT_SECRET;

	public static String PAYPAL_OAUTH_CLIENT_ID;
	public static String PAYPAL_OAUTH_CLIENT_SECRET;

	public static String TWITTER_CONSUMER_KEY;
	public static String TWITTER_CONSUMER_SECRET;
	public static String TWITTER_ACCESS_TOKEN;
	public static String TWITTER_TOKEN_SECRET;

	public static String YOUTUBE_API_KEY;


	public static boolean NOUI = false;


	public static String THEME_COLOR;


	public static String ANALYTICS;

	public static List<String> FILES_TYPE = new ArrayList<>();

	static {

		String file = "/res/settings";

		try {
			Properties props = new Properties();
			InputStream props_stream = Settings.class.getResourceAsStream(file);
			Reader reader = new InputStreamReader(props_stream, StandardCharsets.UTF_8);

			props.load(reader);

			SITE_TITLE = props.getProperty("SITE_TITLE", null);

			LOGO_TITLE = props.getProperty("LOGO_TITLE", null);
			UI_LOGO = props.getProperty("UI_LOGO", null);
			REPO = props.getProperty("REPO");

			PROJECT_NAME = props.getProperty("PROJECT_NAME", null);
			HOST_HTTP = props.getProperty("HOST_HTTP");
			HOST_API = props.getProperty("HOST_API");
			HOST_CDN = props.getProperty("HOST_CDN");
			HTTP_PROTO = props.getProperty("HTTP_PROTO", "https://");


			COOKIE_NAME = props.getProperty("COOKIE_NAME", "session") + ((Fx.IS_DEBUG) ? "Dev2" : "Id2");

			LANGS_DOMAINS = new Json();
			for (String langs_domains : props.getProperty("LANGS_DOMAINS", null).split(" ")) {
				String[] langs_domains_ = langs_domains.split(":");
				LANGS_DOMAINS.put(langs_domains_[0], langs_domains_[1]);
			}


			SALT = props.getProperty("SALT", "qsfsd~sfs#f55q@d8re6qze4sq6666683");

			DB_USER = props.getProperty("DB_USER");
			DB_NAME = props.getProperty("DB_NAME");
			DB_PASS = props.getProperty("DB_PASS").toCharArray();

			if (props.getProperty("NOUI", "false").equals("true")) {
				NOUI = true;
			}


			if (props.getProperty("MENU_FORUM", "false").equals("true")) {
				MENU_FORUM = true;
			}

			if (props.getProperty("PUBS", "true").equals("false")) {
				PUBS = false;
			}

			SMTP_MAIL_USER = props.getProperty("SMTP_MAIL_USER", null);
			SMTP_MAIL_PASSWD = props.getProperty("SMTP_MAIL_PASSWD", null);

			GCM_SENDER_ID = props.getProperty("GCM_SENDER_ID", null);
			GOOGLE_PUSH_KEY = props.getProperty("GOOGLE_PUSH_KEY", null);

			VAPID_PUB = props.getProperty("VAPID_PUB", null);
			VAPID_PRIV = props.getProperty("VAPID_PRIV", null);

			GOOGLE_SEARCH_API = props.getProperty("GOOGLE_SEARCH_API", null);
			BING_SEARCH_API = props.getProperty("BING_SEARCH_API", null);

			GOOGLE_OAUTH_CLIENT_ID = props.getProperty("GOOGLE_OAUTH_CLIENT_ID", null);
			GOOGLE_OAUTH_CLIENT_SECRET = props.getProperty("GOOGLE_OAUTH_CLIENT_SECRET", null);
			FACEBOOK_OAUTH_CLIENT_ID = props.getProperty("FACEBOOK_OAUTH_CLIENT_ID", null);
			FACEBOOK_OAUTH_CLIENT_SECRET = props.getProperty("FACEBOOK_OAUTH_CLIENT_SECRET", null);
			LIVE_OAUTH_CLIENT_ID = props.getProperty("LIVE_OAUTH_CLIENT_ID", null);
			LIVE_OAUTH_CLIENT_SECRET = props.getProperty("LIVE_OAUTH_CLIENT_SECRET", null);

			PAYPAL_OAUTH_CLIENT_ID = props.getProperty("PAYPAL_OAUTH_CLIENT_ID", null);
			PAYPAL_OAUTH_CLIENT_SECRET = props.getProperty("PAYPAL_OAUTH_CLIENT_SECRET", null);

			TWITTER_OAUTH_CLIENT_ID = props.getProperty("TWITTER_OAUTH_CLIENT_ID", null);
			TWITTER_OAUTH_CLIENT_SECRET = props.getProperty("TWITTER_OAUTH_CLIENT_SECRET", null);

			TWITTER_CONSUMER_KEY = props.getProperty("TWITTER_CONSUMER_KEY", null);
			TWITTER_CONSUMER_SECRET = props.getProperty("TWITTER_CONSUMER_SECRET", null);
			TWITTER_ACCESS_TOKEN = props.getProperty("TWITTER_ACCESS_TOKEN", null);
			TWITTER_TOKEN_SECRET = props.getProperty("TWITTER_TOKEN_SECRET", null);

			YOUTUBE_API_KEY = props.getProperty("YOUTUBE_API_KEY", null);

			ANALYTICS = props.getProperty("ANALYTICS", null);

			THEME_COLOR = props.getProperty("THEME_COLOR", "#FFFFFF");

			if (!props.getProperty("VALID_PARENTS", "").equals("")) {
				VALID_PARENTS.addAll(Arrays.asList(props.getProperty("VALID_PARENTS").split(",")));
			}

			FLOOD_DELAY = Long.valueOf(props.getProperty("FLOOD_DELAY", "5000"));

			if (!props.getProperty("FILES_TYPE", "").equals("")) {
				for (String type : props.getProperty("FILES_TYPE").split(",")) {
					type = type.replace(" ", "");
					if (!type.equals("")) {
						FILES_TYPE.add(type);
					}
				}
			}
			reader.close();

			props_stream.close();

		} catch (Exception e) {
			Fx.log("Error in " + file);
			e.printStackTrace();
		}
	}


	public static long getHttpExpires() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, MAX_AGE);
		return cal.getTimeInMillis();
	}

	public static String getFullHttp() {
		return HTTP_PROTO + HOST_HTTP;
	}

	public static String getFullHttp(String lng) {
		return HTTP_PROTO + getDomain(lng);
	}

	public static String getCDNHttp() {
		return HTTP_PROTO + HOST_CDN;
	}

	public static String getApiHTTP() {
		return HTTP_PROTO + HOST_API;
	}

	public static String getLogo() {
		return Settings.getCDNHttp() + Settings.UI_LOGO;
	}

	public static List<String> getLangs() {
		return Settings.LANGS_DOMAINS.keyList();
	}

	public static String getLang(String domain) {
		return Settings.LANGS_DOMAINS.findKey(domain);
	}

	public static String getDomain(String lang) {
		return Settings.LANGS_DOMAINS.getString(lang);
	}

	public static List<String> getDomains() {
		List<String> domains = new ArrayList<>();
		Settings.LANGS_DOMAINS.values().forEach(obj -> {
			domains.add((String) obj);
		});
		return domains;
	}
}
