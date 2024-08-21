/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system;

import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class Settings {

    // template and style control delay in seconds
    public static final long CTRL_PERIOD = 30L;
    // End of the world, or new.. ..Fri, 21 Dec 2012
    public static final long START_COUNT = 1356048000000L;
    // Real one year 31556930.4
    public static final int MAX_AGE = 31556930;
    // Files can not exceed 10Mo
    public static final long MAX_FILE_SIZE = 1024 * 1024 * 10;
    // Delay for cookie
    public static final int COOKIE_DELAY = 30 * 24 * 3600;
    // File chunck size, size of an entry in database
    public static final int CHUNK_SIZE = 128 * 1024;
    private static final Json settings = load();
    //Dir for the specifics templates and others like style/fonts/javascript..
    public static final String REPO = settings.getString("REPO");
    // Salt for passwords
    public static final String SALT = settings.getString("SALT", "qsfsd~sfs#f55q@d8re6qze4sq6666683");
    // Flood delay, delay before post other
    public static final long FLOOD_DELAY = (long) settings.getDouble("FLOOD_DELAY", 5000D);
    // List of keys available for parent form an item
    public static final List<String> VALID_PARENTS = settings.getList("VALID_PARENTS");
    // Global site title
    public static final String SITE_TITLE = settings.getString("SITE_TITLE");
    // Site title used in header bar
    public static final String LOGO_TITLE = settings.getString("LOGO_TITLE");
    // Logo address for all default
    public static final String UI_LOGO = settings.getString("UI_LOGO", "/ui/logo");


    // The base host, the host used by default
    public static final String STANDARD_HOST = settings.getString("STANDARD_HOST");
    // The host used for Api and Web posting
    public static final String HOST_API = settings.getString("HOST_API");

    // The host used for content delivery
    public static final String HOST_CDN = settings.getString("HOST_CDN");

    // The protocol used
    public static final String HTTP_PROTO = settings.getString("HTTP_PROTO", "https://");
    // The protocol used

    // The project name, user as Server signature and Threads names
    public static final String PROJECT_NAME = settings.getString("PROJECT_NAME");
    // Transmit or not the referrer
    public static final String REFERRER_POLICY = settings.getString("REFERRER_POLICY", "unsafe-url");
    // MongoDb username
    public static final List<String> DB_HOSTS = settings.getList("DB_HOSTS");
    // MongoDb username
    public static final String DB_USER = settings.getString("DB_USER", null);
    // MongoDb password
    public static final char[] DB_PASS = settings.getString("DB_PASS", "").toCharArray();
    // MongoDb database
    public static final String DB_NAME = settings.getString("DB_NAME");
    // SMTP host for send email
    public static final String SMTP_MAIL_HOST = settings.getString("SMTP_MAIL_HOST");
    // SMTP password for send email
    public static final int SMTP_MAIL_PORT = settings.getInteger("SMTP_MAIL_PORT", 25);
    // SMTP user for send email
    public static final String SMTP_MAIL_USER = settings.getString("SMTP_MAIL_USER");
    // SMTP password for send email
    public static final String SMTP_MAIL_PASSWD = settings.getString("SMTP_MAIL_PASSWD");
    // SMTP starttls for send email
    public static final boolean SMTP_MAIL_TLS = settings.getBoolean("SMTP_MAIL_TLS", false);
    // Google cloud sender id used in webpush
    public static final String GCM_SENDER_ID = settings.getString("GCM_SENDER_ID");

    // ChatGPT OpenAi key
    public static final String OPEN_AI_KEY = settings.getString("OPEN_AI_KEY", null);

    // Google login with OAuth
    public static final String GOOGLE_OAUTH_CLIENT_ID = settings.getString("GOOGLE_OAUTH_CLIENT_ID");
    public static final String GOOGLE_OAUTH_CLIENT_SECRET = settings.getString("GOOGLE_OAUTH_CLIENT_SECRET");
    // Facebook login with OAuth
    public static final String META_OAUTH_CLIENT_ID = settings.getString("META_OAUTH_CLIENT_ID");
    public static final String META_OAUTH_CLIENT_SECRET = settings.getString("META_OAUTH_CLIENT_SECRET");
    //Files type authorized as upload
    public static final List<String> FILES_TYPE = settings.getList("FILES_TYPE");
    // The name of the cookie session
    private static final String COOKIE_NAME = settings.getString("COOKIE_NAME", "session");
    // All host by language
    private static final Json LANGS_DOMAINS = settings.getJson("LANGS_DOMAINS");

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
        for (String lng : Settings.LANGS_DOMAINS.keyList()) {
            if (Settings.LANGS_DOMAINS.getJson(lng).getString("domain").equals(domain)) {
                return lng;
            }
        }
        return null;
    }

    /**
     * @return domain from language
     */
    public static String getDomain(String lng) {
        if (Settings.LANGS_DOMAINS.getJson(lng) == null) {
            return Settings.STANDARD_HOST;
        }
        return Settings.LANGS_DOMAINS.getJson(lng).getString("domain");
    }

    public static String getName(String lng) {
        if (Settings.LANGS_DOMAINS.getJson(lng) == null) {
            return null;
        }
        return Settings.LANGS_DOMAINS.getJson(lng).getString("name");
    }

    public static boolean domainAvailable(String host) {
        for (String lng : Settings.LANGS_DOMAINS.keyList()) {
            if (Settings.LANGS_DOMAINS.getJson(lng).getString("domain").equals(host)) {
                return true;
            }
        }

        return false;
    }

    public static List<Json> getDomainsInfos() {
        List<Json> list = new ArrayList<>();
        for (String lng : Settings.LANGS_DOMAINS.keyList()) {
            Json data = Settings.LANGS_DOMAINS.getJson(lng);
            list.add(new Json().put("lng", lng).put("name", data.getString("name")).put("domain", data.getString("domain")));
        }
        return list;
    }

    public static List<String> getDomains() {
        List<String> list = new ArrayList<>();
        for (String lng : Settings.LANGS_DOMAINS.keyList()) {
            Json data = Settings.LANGS_DOMAINS.getJson(lng);
            list.add(data.getString("domain"));
        }
        return list;
    }

    /**
     * load properties from settings file
     */
    @SuppressWarnings("unchecked")
    private static Json load() {
        return new Json((Map<String, Object>) new Yaml().load(Fx.getResource("res/settings.yml")));
    }

}
