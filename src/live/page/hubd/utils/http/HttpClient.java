/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.utils.http;

import live.page.hubd.system.Settings;
import live.page.hubd.utils.Fx;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpClient {


    /**
     * Get the content of a web page
     *
     * @param url of the web page
     * @return the content of the web page
     */
    public static String get(String url) {
        return get(url, "PageLive/0.9; +" + Settings.getFullHttp());
    }


    /**
     * Get the content of a web page as a specific user agent signature
     *
     * @param url of the web page
     * @param ua  user agent signature
     * @return the content of the web page
     */
    public static String get(String url, String ua) {
        CloseableHttpClient httpclient = null;
        HttpGet request = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        InputStream in = null;
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            }, new SecureRandom());

            httpclient = HttpClients.custom().setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
                    .setDefaultRequestConfig(
                            RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD)
                                    .setContentCompressionEnabled(true)
                                    .setNormalizeUri(true)
                                    .setMaxRedirects(20)
                                    .setConnectionRequestTimeout(30 * 1000)
                                    .setSocketTimeout(30 * 1000)
                                    .setConnectTimeout(30 * 1000)
                                    .build()
                    ).setSSLContext(sc).setSSLHostnameVerifier((hostname, session) -> true)
                    .setUserAgent(ua).build();

            request = new HttpGet(HttpClient.encodeUrlPath(url));
            request.setHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
            request.setHeader(HttpHeaders.CONNECTION, "keep-alive");
            request.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            request.setHeader(HttpHeaders.PRAGMA, "no-cache");
            request.setHeader("DNT", "1");

            response = httpclient.execute(request);

            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }

            Charset charset = null;
            try {
                Matcher matcher = Pattern.compile("charset=[ '\"]?+([a-z0-9\\-]+)", Pattern.CASE_INSENSITIVE).matcher(response.getLastHeader("Content-Type").getValue());
                if (matcher.find() && matcher.groupCount() > 0) {
                    charset = Charset.forName(matcher.group(1).toLowerCase());
                }
            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
            }

            entity = response.getEntity();
            in = LimitedStream.asLimited(entity.getContent(), 3);
            byte[] src = IOUtils.toByteArray(in);
            String rez = new String(src, charset != null ? charset : StandardCharsets.UTF_8);
            if (charset == null) {
                Matcher matcher = Pattern.compile("charset=[ '\"]?+([a-z0-9\\-]+)", Pattern.CASE_INSENSITIVE).matcher(rez);
                while (matcher.find()) {
                    if (!matcher.group(1).equalsIgnoreCase(StandardCharsets.UTF_8.displayName())) {
                        try {
                            rez = new String(src, Charset.forName(matcher.group(1).toLowerCase()));
                            rez = rez.replace("\u0092", "'");
                            break;
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
            return rez;

        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return null;
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
            try {
                EntityUtils.consume(entity);
            } catch (Exception ignore) {
            }
            try {
                response.close();
            } catch (Exception ignore) {
            }
            try {
                request.abort();
            } catch (Exception ignore) {
            }
            try {
                httpclient.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Encode the url as a correct format
     *
     * @param url to encode
     * @return the url encoded
     */
    public static String encodeUrlPath(String url) {
        try {
            URL uri = new URL(url);
            StringBuilder str = new StringBuilder();
            if (uri.getProtocol() != null) {
                str.append(uri.getProtocol());
                str.append("://");
            }
            if (uri.getHost() != null) {
                str.append(uri.getHost());
            }
            for (String path : uri.getPath().split("/")) {
                if (!path.isEmpty()) {
                    str.append("/");
                    if (URLDecoder.decode(path, StandardCharsets.UTF_8).equals(path)) {
                        str.append(URLEncoder.encode(path, StandardCharsets.UTF_8));
                    } else {
                        str.append(path);
                    }
                }
            }
            if (uri.getQuery() != null) {
                str.append("?");
                str.append(uri.getQuery());
            }
            return str.toString();
        } catch (Exception ignore) {

        }
        return url;
    }

}
