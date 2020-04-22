/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system;

import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Settings {


	private static Properties settings = load();

	//Dir for the base templates and others like style/fonts/javascript..
	public static final String HUB_REPO = settings.getProperty("HUB_REPO", "/data/repo/HubD/hubd");

	//Dir for the specifics templates and others like style/fonts/javascript..
	public static final String REPO = settings.getProperty("REPO");

	// template and style control delay in seconds
	public static final long CTRL_PERIOD = 30L;

	// End of the world, or new.. ..Fri, 21 Dec 2012
	public static final long START_COUNT = 1356048000000L;

	// Real one year 31556930.4
	public static final int MAX_AGE = 31556930;

	// Files can not exceed 10Mo
	public static final long MAX_FILE_SIZE = 1024 * 1024 * 10;

	// AdSense or not
	public static final boolean PUBS = Boolean.parseBoolean(settings.getProperty("PUBS", "false"));

	// Salt for passwords
	public static final String SALT = settings.getProperty("SALT", "qsfsd~sfs#f55q@d8re6qze4sq6666683");

	// Flood delay, delay before post other
	public static final long FLOOD_DELAY = Long.parseLong(settings.getProperty("FLOOD_DELAY", "5000"));


	// List of keys available for parent form an item
	public static final List<String> VALID_PARENTS = getValidParents();


	// Delay for cookie
	public static final int COOKIE_DELAY = 30 * 24 * 3600;

	// File chunck size, size of an entry in database
	public static final int CHUNCK_SIZE = 128 * 1024;


	// Global site title
	public static final String SITE_TITLE = settings.getProperty("SITE_TITLE");
	// Site title used in header bar
	public static final String LOGO_TITLE = settings.getProperty("LOGO_TITLE");
	// Logo address for all default
	public static final String UI_LOGO = settings.getProperty("UI_LOGO", "/ui/logo");


	// The base host, the host used by default
	public static final String STANDARD_HOST = settings.getProperty("STANDARD_HOST");
	// The host used for Api and Web posting
	public static final String HOST_API = settings.getProperty("HOST_API");

	// The host used for content delivery
	public static final String HOST_CDN = settings.getProperty("HOST_CDN");

	// The protocol used
	public static final String HTTP_PROTO = settings.getProperty("HTTP_PROTO", "https://");

	// The project name, user as Server signature and Threads names
	public static final String PROJECT_NAME = settings.getProperty("PROJECT_NAME");

	// The name of the cookie session
	private static final String COOKIE_NAME = settings.getProperty("COOKIE_NAME", "session");

	// Transmit or not the referrer
	public static final String REFERRER_POLICY = settings.getProperty("REFERRER_POLICY", "origin-when-cross-origin");

	// All host by language
	public static final Json LANGS_DOMAINS = getLangsDomains("LANGS_DOMAINS");

	// MongoDb username
	public static final String DB_USER = settings.getProperty("DB_USER");
	// MongoDb password
	public static final char[] DB_PASS = settings.getProperty("DB_PASS", "").toCharArray();
	// MongoDb database
	public static final String DB_NAME = settings.getProperty("DB_NAME");

	// MongoDb username used for migration
	public static final String MIGRATOR_DB_USER = settings.getProperty("MIGRATOR_DB_USER");
	// MongoDb password used for migration
	public static final char[] MIGRATOR_DB_PASS = settings.getProperty("MIGRATOR_DB_PASS", "").toCharArray();
	// MongoDb database used for migration
	public static final String MIGRATOR_DB_NAME = settings.getProperty("MIGRATOR_DB_NAME");

	// All host by language for migration
	public static final Json MIGRATOR_LANGS_DOMAINS = getLangsDomains("MIGRATOR_LANGS_DOMAINS");

	// Calculation and include special breadcrumb menu
	public static final boolean MENU_FORUM = Boolean.parseBoolean(settings.getProperty("MENU_FORUM", "false"));

	// SMTP host for send email
	public static final String SMTP_MAIL_HOST = settings.getProperty("SMTP_MAIL_HOST");
	// SMTP password for send email
	public static final String SMTP_MAIL_PORT = settings.getProperty("SMTP_MAIL_PORT");
	// SMTP user for send email
	public static final String SMTP_MAIL_USER = settings.getProperty("SMTP_MAIL_USER");
	// SMTP password for send email
	public static final String SMTP_MAIL_PASSWD = settings.getProperty("SMTP_MAIL_PASSWD");
	// SMTP starttls for send email
	public static final String SMTP_MAIL_TLS = settings.getProperty("SMTP_MAIL_TLS", "false");


	// Google cloud sender id used in webpush
	public static final String GCM_SENDER_ID = settings.getProperty("GCM_SENDER_ID");
	// Firebase webpush key
	public static final String GOOGLE_PUSH_KEY = settings.getProperty("GOOGLE_PUSH_KEY");

	// VAPID public for webpush
	public static final String VAPID_PUB = settings.getProperty("VAPID_PUB");
	// VAPID private for webpush
	public static final String VAPID_PRIV = settings.getProperty("VAPID_PRIV");

	// Google login with OAuth
	public static final String GOOGLE_OAUTH_CLIENT_ID = settings.getProperty("GOOGLE_OAUTH_CLIENT_ID");
	public static final String GOOGLE_OAUTH_CLIENT_SECRET = settings.getProperty("GOOGLE_OAUTH_CLIENT_SECRET");

	// Facebook login with OAuth
	public static final String FACEBOOK_OAUTH_CLIENT_ID = settings.getProperty("FACEBOOK_OAUTH_CLIENT_ID");
	public static final String FACEBOOK_OAUTH_CLIENT_SECRET = settings.getProperty("FACEBOOK_OAUTH_CLIENT_SECRET");

	// Microsoft Live login with OAuth
	public static final String LIVE_OAUTH_CLIENT_ID = settings.getProperty("LIVE_OAUTH_CLIENT_ID");
	public static final String LIVE_OAUTH_CLIENT_SECRET = settings.getProperty("LIVE_OAUTH_CLIENT_SECRET");

	// Twitter login with OAuth
	public static final String TWITTER_OAUTH_CLIENT_ID = settings.getProperty("TWITTER_OAUTH_CLIENT_ID");
	public static final String TWITTER_OAUTH_CLIENT_SECRET = settings.getProperty("TWITTER_OAUTH_CLIENT_SECRET");


	// Paypal login with OAuth
	public static final String PAYPAL_OAUTH_CLIENT_ID = settings.getProperty("PAYPAL_OAUTH_CLIENT_ID");
	public static final String PAYPAL_OAUTH_CLIENT_SECRET = settings.getProperty("PAYPAL_OAUTH_CLIENT_SECRET");


	// YouTube API Key for scrap data
	public static final String YOUTUBE_API_KEY = settings.getProperty("YOUTUBE_API_KEY");

	// Disable all base UI exempt libs for specials projects
	public static final boolean NOUI = Boolean.parseBoolean(settings.getProperty("NOUI", "false"));

	// Enable ajax navigation
	public static final boolean AJAX = Boolean.parseBoolean(settings.getProperty("AJAX", "true"));

	// Theme color (address bar in chrome mobile)
	public static final String THEME_COLOR = settings.getProperty("THEME_COLOR", "#FFFFFF");

	// Google Analytics project ID
	public static final String ANALYTICS = settings.getProperty("ANALYTICS");

	//Files type authorized as upload
	public static final List<String> FILES_TYPE = Arrays.asList(settings.getProperty("FILES_TYPE", "image/png,image/jpeg,image/jpg").split("[ ]?+,[ ]?+"));


	/**
	 * @return cookie name with a different value when debug
	 */
	public static String getCookieName() {
		return COOKIE_NAME + (Fx.IS_DEBUG ? "Dev" : "");
	}

	/**
	 * @return value from max age for header expire
	 */
	public static long getHttpExpires() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, MAX_AGE);
		return cal.getTimeInMillis();
	}

	/**
	 * @return standard host address with protocol
	 */
	public static String getFullHttp() {
		return HTTP_PROTO + STANDARD_HOST;
	}

	/**
	 * @return host address from language with protocol
	 */
	public static String getFullHttp(String lng) {
		return HTTP_PROTO + getDomain(lng);
	}

	/**
	 * @return content delivery address with protocol
	 */
	public static String getCDNHttp() {
		return HTTP_PROTO + HOST_CDN;
	}

	/**
	 * @return api address with protocol
	 */
	public static String getApiHTTP() {
		return HTTP_PROTO + HOST_API;
	}

	/**
	 * @return logo address with protocol
	 */
	public static String getLogo() {
		return Settings.getCDNHttp() + Settings.UI_LOGO;
	}

	/**
	 * @return languages codes lang available
	 */
	public static List<String> getLangs() {
		return Settings.LANGS_DOMAINS.keyList();
	}

	/**
	 * @return language from domain
	 */
	public static String getLang(String domain) {
		return Settings.LANGS_DOMAINS.findKey(domain);
	}

	/**
	 * @return domain from language
	 */
	public static String getDomain(String lang) {
		return Settings.LANGS_DOMAINS.getString(lang);
	}

	/**
	 * @return all domains
	 */
	public static List<String> getDomains() {
		List<String> domains = new ArrayList<>();
		Settings.LANGS_DOMAINS.values().forEach(obj -> domains.add((String) obj));
		return domains;
	}

	/**
	 * load properties from settings file
	 */
	private static Properties load() {
		Properties settings = new Properties();
		String file = "/res/.settings";
		try {
			InputStream props_stream = Settings.class.getResourceAsStream(file);
			if (props_stream == null) {
				file = "/res/settings";
				props_stream = Settings.class.getResourceAsStream(file);
			}
			Reader reader = new InputStreamReader(props_stream, StandardCharsets.UTF_8);
			settings.load(reader);
			reader.close();
			props_stream.close();
		} catch (Exception e) {
			Fx.log("Error in " + file);
			e.printStackTrace();
			return null;
		}
		return settings;
	}

	/**
	 * Find valid parents from settings file
	 */
	private static List<String> getValidParents() {
		List<String> VALID_PARENTS = new ArrayList<>(Arrays.asList("Posts", "Forums", "Pages"));
		for (String valid : settings.getProperty("VALID_PARENTS", "").split("[ ]?+,[ ]?+")) {
			if (!valid.equals("") && !VALID_PARENTS.contains(valid)) {
				VALID_PARENTS.add(valid);
			}
		}
		return VALID_PARENTS;
	}

	/**
	 * Find languages and domains associations from settings file
	 */
	private static Json getLangsDomains(String key) {
		Json LANGS_DOMAINS = new Json();
		for (String langs_domains : settings.getProperty(key, "").split("[ ]+")) {
			String[] langs_domains_ = langs_domains.split("[ ]?+:[ ]?+");
			if (langs_domains_.length == 2) {
				LANGS_DOMAINS.put(langs_domains_[0], langs_domains_[1]);
			}
		}
		return LANGS_DOMAINS;
	}

	public static String getString(String key) {
		return settings.getProperty(key);
	}
}
