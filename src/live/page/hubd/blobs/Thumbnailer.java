/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.blobs;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import live.page.hubd.system.utils.http.HttpClient;
import org.apache.commons.io.IOUtils;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Thumbnail service
 */
@WebListener
public class Thumbnailer implements ServletContextListener {

    static final ExecutorService thumbnailerService = Executors.newFixedThreadPool(4);

    /**
     * Make a thumbnail from URL or DB image
     * apt install potrace imagemagick inkscape
     */
    public static Json makeDb(String blob, String type, int width, int height) {
        try {
            if (blob.matches("^https?://.*")) {
                Blob download = BlobsUtils.downloadAsBlob(blob);

                if ((download == null || download.file.length() < 100L || download.contentType.contains("html")) && blob.endsWith("/favicon.ico")) {
                    if (download != null) {
                        download.file.delete();
                    }
                    download = getFavicon(blob);
                }

                if (download == null) {
                    return null;
                }

                Json cache = thumbFile(download.file, blob, type, width, height);

                download.file.delete();
                return cache;

            } else {

                Json file = Db.getDb("BlobFiles").find(Filters.eq("_id", blob)).first();
                if (file == null) {
                    return null;
                }
                Bson filter = Filters.eq("f", blob);
                Bson sort = Sorts.ascending("o");

                if (file.getString("type", "").startsWith("image/")) {

                    MongoCursor<Json> chunks = Db.getDb("BlobChunks").find(filter).sort(sort).iterator();
                    File tmp = File.createTempFile("PageBack_thumb", ".file");
                    tmp.deleteOnExit();
                    FileOutputStream outStream = new FileOutputStream(tmp);
                    try {
                        if (!chunks.hasNext()) {
                            return null;
                        }
                        while (chunks.hasNext()) {
                            outStream.write(chunks.next().getBinary("b").getData());
                        }
                        return thumbFile(tmp, file, type, width, height);
                    } catch (Exception e) {
                        return null;
                    } finally {
                        try {
                            chunks.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            tmp.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            outStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    /**
     * Make a thumbnail from File
     */
    public static Json thumbFile(File file, String id, String type, int width, int height) {
        return thumbFile(file, new Json("_id", id), type, width, height);
    }

    /**
     * Make a thumbnail from File
     */
    public static Json thumbFile(File file, Json data, String type, int width, int height) {
        if (data == null || data.getId() == null) {
            return null;
        }
        Future<Json> future = thumbnailerService.submit(() -> {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            Json cache = new Json().put("blob", data.getId()).put("width", width).put("height", height).put("format", type).put("date", new Date());

            Runtime runtime = Runtime.getRuntime();
            try {

                if (type == null) {
                    cache.put("type", null);
                } else if (type.equalsIgnoreCase("png")) {
                    cache.put("type", "image/png");
                } else if (type.equalsIgnoreCase("jpeg") || type.equalsIgnoreCase("jpg")) {
                    cache.put("type", "image/jpeg");
                } else if (type.equalsIgnoreCase("gif")) {
                    cache.put("type", "image/gif");
                } else {
                    cache.put("type", null);
                }
                if (!data.containsKey("format")) {
                    Json identify = BlobsUtils.identify(file);
                    if (identify == null) {
                        return null;
                    }
                    if (identify.containsKey("format")) {
                        Db.updateOne("BlobFiles", Filters.eq("_id", data.getId()), new Json("$set", identify));
                        data.putAll(identify);
                    }

                }
                if (!data.containsKey("format")) {
                    return null;
                }

                String format = data.getString("format").toLowerCase();
                int width_origin = data.getInteger("width");
                int height_origin = data.getInteger("height");

                String outputformat = type == null ? format : type;
                if (type == null) {
                    outputformat = format;
                } else if (type.equals("jpg")) {
                    cache.put("format", "jpg");
                } else if (type.equals("png")) {
                    cache.put("format", "png");
                } else if (type.equals("gif")) {
                    cache.put("format", "gif");
                }
                if (outputformat.equals("svg")) {
                    outputformat = "png";
                }
                File resized = File.createTempFile("resize", "." + outputformat);

                List<String> commands = new ArrayList<>();
                commands.add("/usr/bin/convert");

                commands.add("-background");
                commands.add("none");


                if (height > 0) {
                    int tempheight = (height_origin * width) / width_origin;
                    int tempwidth = width;
                    if (tempheight < height) {
                        tempwidth = (width_origin * height) / height_origin;
                        tempheight = height;
                    }
                    commands.add("-resize");
                    commands.add(tempwidth + "x" + tempheight + "^");
                    commands.add("-gravity");
                    commands.add("Center");
                    commands.add("-extent");
                    commands.add(width + "x" + height);
                } else if (width > 0) {
                    int tempHeight = (height_origin * width) / width_origin;
                    commands.add("-resize");
                    commands.add(width + "x" + tempHeight);
                }

                if (outputformat.equals("jpeg") || outputformat.equals("jpg")) {
                    commands.add("-quality");
                    commands.add("70");
                }


                if (format.equalsIgnoreCase("gif") && !outputformat.equalsIgnoreCase("gif")) {
                    commands.add(file.getAbsolutePath() + "[0]");
                } else {
                    commands.add(file.getAbsolutePath());
                }

                commands.add(resized.getAbsolutePath());

                Process process = runtime.exec(commands.toArray(new String[0]));
                process.waitFor();


                if (hasError(process.getErrorStream())) {
                    return null;
                }

                byte[] bt = Files.readAllBytes(resized.toPath());
                cache.put("size", bt.length);

                if (outputformat.equalsIgnoreCase("png")) {
                    cache.put("type", "image/png");
                } else if (outputformat.equalsIgnoreCase("jpeg") || outputformat.equalsIgnoreCase("jpg")) {
                    cache.put("type", "image/jpeg");
                } else if (outputformat.equalsIgnoreCase("webp")) {
                    cache.put("type", "image/webp");
                }

                List<Binary> binaries = new ArrayList<>();
                try (ByteArrayInputStream bin = new ByteArrayInputStream(bt)) {
                    byte[] buffer = new byte[Math.min(bt.length, Settings.CHUNK_SIZE)];
                    while (bin.read(buffer) != -1) {
                        binaries.add(new Binary(buffer));
                    }
                } catch (Exception ignore) {
                    return null;
                }
                cache.put("binaries", binaries);
                if (!Fx.IS_DEBUG) {
                    Db.save("BlobCache", cache);
                }
                return cache;
            } catch (InterruptedException ie) {
                return null;
            } catch (Exception e) {

                if (!Fx.IS_DEBUG) {
                    cache.put("expire", 30 * 24 * 3600);
                    Db.save("BlobCache", cache);
                }

                e.printStackTrace();
                return null;
            }


        });
        try {
            return future.get();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Get cached thumbnail from DB
     */
    public static Json getCache(String blob, String format, int width, int height) {

        List<Bson> filters = new ArrayList<>();
        filters.add(Filters.eq("blob", blob));

        if (width > 0) {
            filters.add(Filters.eq("width", width));
        }
        filters.add(Filters.eq("height", height));

        if (format != null) {
            filters.add(Filters.eq("format", format.replace("jpeg", "jpg")));
        }

        return Db.getDb("BlobCache").findOneAndUpdate(Filters.and(filters), new Json("$set", new Json("date", new Date())));

    }

    /**
     * Search and find favicon
     */
    private static Blob getFavicon(String blob) {

        try {
            String url = blob.replaceAll("(.*)favicon.ico$", "$1");
            Document page = Jsoup.parse(HttpClient.get(url), url);
            Elements icons = page.select("head link[rel=shortcut icon],head link[rel=icon]");
            if (!icons.isEmpty()) {
                for (Element icon : icons) {
                    Blob favicon = BlobsUtils.downloadAsBlob(icon.absUrl("href"));
                    if (favicon != null) {
                        return favicon;
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean hasError(InputStream errorStream) throws IOException {
        if (errorStream != null) {
            String error = IOUtils.toString(errorStream, StandardCharsets.UTF_8);
            if (error != null && !error.isEmpty()) {
                // warning : apt install potrace imagemagick inkscape
                Fx.log(error);
                errorStream.close();
                return true;
            }
        }
        return false;
    }

    public static boolean resize(File file, int maxSize) throws InterruptedException, IOException {
        Runtime runtime = Runtime.getRuntime();
        List<String> commands = new ArrayList<>();
        commands.add("convert");
        commands.add("-background");
        commands.add("none");
        commands.add("-resize");
        commands.add(maxSize + "x" + maxSize + ">");
        commands.add(file.getAbsolutePath());
        commands.add(file.getAbsolutePath());
        Process process = runtime.exec(commands.toArray(new String[0]));
        process.waitFor();
        return !hasError(process.getErrorStream());
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(thumbnailerService);
    }
}