/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.blobs;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.IndexBuilder;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.io.FileUtils;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlobsService {

    /**
     * Thumbnail process local file situated in war/webContent folder
     */
    public static void processLocal(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        try {

            Pattern pattern = Pattern.compile("/ui/([\\w/\\-]+)@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png|\\.gif)?$");
            Matcher matcher = pattern.matcher(req.getRequestURI());
            if (matcher.matches()) {
                int width = -1;
                int height = -1;
                String blob = null;
                String format = null;
                if (matcher.group(1) != null) {
                    blob = matcher.group(1);
                } else {
                    resp.sendError(404, "NOT_FOUND");
                    return;
                }
                if (matcher.group(2) != null) {
                    width = Integer.parseInt(matcher.group(2));
                }
                if (matcher.group(3) != null) {
                    height = Integer.parseInt(matcher.group(3).replaceFirst("^x", ""));
                }
                if (matcher.group(4) != null) {
                    format = matcher.group(4).substring(1);
                }

                if (width > 2000 || height > 2000) {
                    resp.sendError(404, "NOT_FOUND");
                    return;
                }

                Json file = Thumbnailer.getCache(blob, format, width, height);

                if (file == null) {
                    URL localRessource = req.getServletContext().getResource("/css/" + blob + ".png");
                    if (localRessource == null) {
                        resp.sendError(404, "NOT_FOUND");
                        return;
                    }
                    File tmp = File.createTempFile("PageBack_thumb", ".file");
                    try {
                        FileUtils.copyFile(new File(localRessource.getFile()), tmp);
                        file = Thumbnailer.thumbFile(tmp, blob, matcher.group(4), width, height);
                    } finally {
                        tmp.delete();
                    }
                }
                if (file == null) {
                    resp.sendError(404, "NOT_FOUND");
                    return;

                }

                doHeaders(req, resp, file);
                ServletOutputStream outStream = resp.getOutputStream();
                try {
                    List<Binary> bins = file.getList("binaries");
                    for (Binary bin : bins) {
                        outStream.write(bin.getData());
                    }
                    outStream.flush();
                } catch (Exception e) {

                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        outStream.close();
                    } catch (Exception e) {
                        if (Fx.IS_DEBUG) {
                            e.printStackTrace();
                        }
                    }
                }
                return;
            }

        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            resp.sendError(500);
            return;
        }
        resp.sendError(404, "NOT_FOUND");

    }


    /**
     * Thumbnail process local file situated in war/webContent folder
     */
    public static void doGetThumb(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        Pattern pattern = Pattern.compile(".*@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png|\\.webp)?$");
        Matcher matcher = pattern.matcher(req.getRequestURI());
        String blob = URLDecoder.decode(req.getRequestURI().replaceAll("^/files/(.*)@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png|\\.webp)?$", "$1"), StandardCharsets.UTF_8);


        if (matcher.matches()) {
            String format = null;
            int width = -1;
            int height = -1;

            if (matcher.group(1) != null) {
                width = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null) {
                    height = Integer.parseInt(matcher.group(2).replaceFirst("^x", ""));
                }

                if (matcher.group(3) != null) {
                    format = matcher.group(3).substring(1);
                }

                if (width > 2000 || height > 2000) {
                    resp.sendTextError(404, "Not found");
                    return;
                }
            } else {
                resp.sendTextError(404, "Not found");
                return;
            }

            Json blobCache = Thumbnailer.getCache(blob, format, width, height);

            if (blobCache == null && !BruteLocker.isBan(ServletUtils.realIp(req))) {
                blobCache = Thumbnailer.makeDb(blob, format, width, height);
            }
            if (blobCache == null || !blobCache.containsKey("binaries")) {
                resp.setStatus(404);
                blobCache = Thumbnailer.getCache("error", format, width, height);
                if (blobCache == null) {
                    blobCache = Thumbnailer.thumbFile(new File(Settings.REPO + "/res/error.png"), "error", format, width, height);
                }
            }
            if (blobCache == null || !blobCache.containsKey("binaries")) {
                resp.sendTextError(404, "Not found");
                return;
            }

            doHeaders(req, resp, blobCache);
            ServletOutputStream outStream = resp.getOutputStream();
            try {
                List<Binary> bins = blobCache.getList("binaries");
                for (Binary bin : bins) {
                    outStream.write(bin.getData());
                }
                blobCache.clear();
            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    outStream.close();
                } catch (Exception e) {
                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            resp.sendTextError(404, "Not found");
        }

    }

    public static void doGetConvert(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        Pattern pattern = Pattern.compile(".*\\.(jpg|png|webp)$");
        Matcher matcher = pattern.matcher(req.getRequestURI());
        String blob = URLDecoder.decode(req.getRequestURI().replaceAll("^/files/(.*)(\\.jpg|\\.png|\\.webp)$", "$1"), StandardCharsets.UTF_8);

        if (matcher.matches()) {
            if (matcher.group(1) == null) {
                resp.sendTextError(404, "Not found");
                return;

            }

            String format = matcher.group(1);
            Json blobCache = Thumbnailer.getCache(blob, format, -1, -1);

            if (blobCache == null && !BruteLocker.isBan(ServletUtils.realIp(req))) {
                blobCache = Thumbnailer.makeDb(blob, format, -1, -1);
            }
            if (blobCache == null || !blobCache.containsKey("binaries")) {
                resp.setStatus(404);
                blobCache = Thumbnailer.getCache("error", format, 100, 100);
                if (blobCache == null) {
                    blobCache = Thumbnailer.thumbFile(new File(Settings.REPO + "/res/error.png"), "error", format, 100, 100);
                }
            }
            if (blobCache == null || !blobCache.containsKey("binaries")) {
                resp.sendTextError(404, "Not found");
                return;
            }

            doHeaders(req, resp, blobCache);
            ServletOutputStream outStream = resp.getOutputStream();
            try {
                List<Binary> bins = blobCache.getList("binaries");
                for (Binary bin : bins) {
                    outStream.write(bin.getData());
                }
                blobCache.clear();
            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    outStream.close();
                } catch (Exception e) {
                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            resp.sendTextError(404, "Not found");
        }

    }


    /**
     * Thumbnail process db file
     */
    public static void doGetBlob(BaseServletRequest req, BaseServletResponse resp) throws IOException {

        Json file = Db.getDb("BlobFiles").find(Filters.eq("_id", req.getId())).first();
        if (file != null) {
            doHeaders(req, resp, file);
            Bson filter = Filters.eq("f", file.getId());
            Bson sort = Sorts.ascending("o");
            MongoCursor<Json> chunks = Db.getDb("BlobChunks").find(filter).hint(IndexBuilder.getHint("BlobChunks", "file")).sort(sort).iterator();
            ServletOutputStream outStream = resp.getOutputStream();
            try {
                while (chunks.hasNext()) {
                    try {
                        outStream.write(chunks.next().getBinary("b").getData());
                        outStream.flush();
                    } catch (Exception e) {
                        if (Fx.IS_DEBUG) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                if (Fx.IS_DEBUG) {
                    e.printStackTrace();
                }
            } finally {
                try {
                    chunks.close();
                } catch (Exception e) {
                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                }
                try {
                    outStream.close();
                } catch (Exception e) {
                    if (Fx.IS_DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            resp.sendTextError(404, "Not found");
        }
    }

    /**
     * Send corrects headers, does not use the Tag header, we prefer a long time expiration date.
     */
    public static void doHeaders(HttpServletRequest req, HttpServletResponse resp, Json file) {
        String origin = req.getHeader("Origin");
        if (origin != null) {
            try {
                URI uriOrigin = new URI(origin);
                boolean authorized = origin.startsWith("http://localhost:") || Settings.domainAvailable(uriOrigin.getHost());
                if (Fx.IS_DEBUG || authorized) {
                    resp.setHeader("Access-Control-Allow-Origin", origin);
                }
            } catch (Exception ignore) {
            }
        }

        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type");
        resp.setIntHeader("Access-Control-Max-Age", Settings.COOKIE_DELAY);
        resp.setHeader("Vary", "Origin");

        resp.setContentType(file.getString("type"));
        resp.setHeader("Content-Type", file.getString("type"));
        resp.setContentLength(file.getInteger("size"));
        resp.setDateHeader("Expires", Settings.getHttpExpires());
        if (file.containsKey("date")) {
            resp.setDateHeader("Last-Modified", file.getDate("date").getTime());
        }
        resp.setHeader("Cache-Control", "public, max-age=" + Settings.MAX_AGE);
        try {
            String filename = file.getString("name", "image" + file.getString("type").replaceAll(".*/(.*)$", ".$1").replace("jpeg", "jpg"));
            resp.setHeader("Content-Disposition",
                    ((req.getQueryString() != null && req.getQueryString().equals("attachment")) ? "attachment" : "inline") +
                            "; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        } catch (Exception ignore) {
        }
        resp.setContentType(file.getString("type"));
    }

}
