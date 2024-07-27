/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.blobs;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;
import live.page.hubd.utils.http.HttpClient;
import live.page.hubd.utils.http.LimitedStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.Executors;

public class BlobsUtils {


    /**
     * Put file in database
     */
    public static String putFile(File file, String user_id, String ip, String name, String type) {
        try {
            Json blob = BlobsUtils.identify(file);
            if (blob == null) {
                blob = new Json();
            }
            blob.put("user", user_id).put("ip", ip).put("type", type).put("name", name).put("size", file.length()).put("date", new Date());
            Db.save("BlobFiles", blob);
            FileInputStream bin = new FileInputStream(file);
            byte[] buffer = new byte[(int) Math.min(file.length(), Settings.CHUNK_SIZE)];
            int order = 0;
            MongoCollection<Json> blobChunks = Db.getDb("BlobChunks");
            while (bin.read(buffer) != -1) {
                blobChunks.insertOne(new Json("_id", Db.getKey()).put("b", buffer).put("o", order++).put("f", blob.getId()));
            }
            bin.close();
            return blob.getId();
        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Download external image and return file and contentType
     */
    public static Blob downloadAsBlob(String url) {
        CloseableHttpClient httpclient = null;
        HttpGet request = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        InputStream in = null;
        File file = null;
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
            }, new java.security.SecureRandom());
            httpclient = HttpClients.custom()
                    .setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(10000)
                            .setContentCompressionEnabled(true)
                            .setNormalizeUri(true)
                            .setMaxRedirects(20)
                            .setConnectionRequestTimeout(10000)
                            .setSocketTimeout(10000).build()
                    ).setSSLContext(sc).setSSLHostnameVerifier((hostname, session) -> true).build();

            file = File.createTempFile("download.", ".file");
            file.deleteOnExit();

            request = new HttpGet(HttpClient.encodeUrlPath(url));

            response = httpclient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                file.delete();
                return null;
            }
            String contentType = response.getLastHeader(HttpHeaders.CONTENT_TYPE).getValue();
            entity = response.getEntity();
            in = LimitedStream.asLimited(entity.getContent(), 10);

            FileUtils.copyInputStreamToFile(in, file);

            if (file.length() == 0) {
                file.delete();
                return null;
            }
            return new Blob(file, contentType);
        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                Fx.log("error downloadAsBlob");
            }
            try {
                file.delete();
            } catch (Exception ignore) {
            }
            return null;
        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
            try {
                response.close();
            } catch (Exception ignore) {
            }
            try {
                EntityUtils.consume(entity);
            } catch (Exception ignore) {
            }
            try {
                httpclient.close();
            } catch (Exception ignore) {
            }
        }
    }


    /**
     * Download external image and put in Db with initial infos
     */
    public static String downloadToDb(String url, int maxSize) {

        try {
            Blob download = downloadAsBlob(url);
            if (download == null) {
                return null;
            }
            if (maxSize > 0) {
                BufferedImage image = ImageIO.read(download.file);
                if (image.getHeight() > maxSize || image.getWidth() > maxSize) {
                    if (!Thumbnailer.resize(download.file, maxSize)) {
                        return null;
                    }
                }
            }

            Json blob = new Json().put("type", download.contentType).put("size", download.file.length()).put("date", new Date());

            Db.save("BlobFiles", blob);

            FileInputStream bin = new FileInputStream(download.file);

            byte[] buffer = new byte[(int) Math.min(download.file.length(), Settings.CHUNK_SIZE)];
            int order = 0;
            MongoCollection<Json> blobChunks = Db.getDb("BlobChunks");
            while (bin.read(buffer) != -1) {
                blobChunks.insertOne(new Json("_id", Db.getKey()).put("b", buffer).put("o", order++).put("f", blob.getId()));
            }
            bin.close();
            download.file.delete();
            return blob.getId();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }


    public static void remove(String _id) {
        Executors.newSingleThreadExecutor().submit(() -> {
            Db.deleteMany("BlobFiles", Filters.eq("_id", _id));
            Db.deleteMany("BlobChunks", Filters.eq("f", _id));
            Db.deleteMany("BlobCache", Filters.eq("blob", _id));
        });
    }

    public static Json delete(String _id, Users user) {

        boolean deleteInfos = Db.deleteOne("BlobFiles", Filters.and(Filters.eq("user", user.getId()), Filters.eq("_id", _id)));
        if (deleteInfos) {
            Db.deleteMany("BlobChunks", Filters.eq("f", _id));
        }
        return new Json("ok", deleteInfos);
    }

    public static Json parent(String id, String parent, Users user, boolean add) {

        UpdateResult update = Db.updateOne("BlobFiles", user.getAdmin() ? Filters.eq("_id", id) : Filters.and(Filters.eq("_id", id), Filters.eq("user", user.getId())),
                new Json().put(add ? "$addToSet" : "$pull", new Json("parents", parent))
        );
        return new Json("ok", update.getModifiedCount() > 0);
    }

    public static Json identify(File file) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(new String[]{"/usr/bin/identify", "-format", "%m|%w|%h", file.getAbsolutePath() + "[0]"});
            process.waitFor();
            if (Thumbnailer.hasError(process.getErrorStream())) {
                return null;
            }
            InputStream stream = process.getInputStream();
            String[] console = IOUtils.toString(stream, StandardCharsets.UTF_8).split("\\|");
            stream.close();
            return new Json("format", console[0].toLowerCase()).put("width", Integer.parseInt(console[1])).put("height", Integer.parseInt(console[2]));

        } catch (Exception ignored) {
        }
        return null;
    }

    public static Json addPhoto(String id, String photo, Users user) {
        UpdateResult update = Db.updateOne("BlobFiles",
                Filters.and(Filters.eq("_id", photo), user.getEditFilter()
                ), new Json("$push", new Json("parents", id)));

        return new Json("ok", update.getModifiedCount() > 0);
    }

    public static Json rmPhoto(String id, String photo, Users user) {
        UpdateResult update = Db.updateOne("BlobFiles",
                Filters.and(Filters.eq("_id", photo), Filters.or(
                        Filters.eq("user", user.getId()),
                        Filters.eq("users", user.getId()))
                ), new Json("$pull", new Json("parents", id)));

        return new Json("ok", update.getModifiedCount() > 0);
    }
}
