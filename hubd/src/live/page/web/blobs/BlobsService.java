/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.db.Aggregator;
import live.page.web.db.Db;
import live.page.web.db.IndexBuilder;
import live.page.web.servlet.wrapper.BaseServletRequest;
import live.page.web.servlet.wrapper.BaseServletResponse;
import live.page.web.session.Users;
import live.page.web.utils.Fx;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;
import live.page.web.utils.paginer.Paginer;
import org.apache.commons.io.FileUtils;
import org.bson.conversions.Bson;
import org.bson.types.Binary;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlobsService {

	public static void processLocal(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		try {

			Pattern pattern = Pattern.compile("/ui/([\\w/\\-]+)@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png)?$");
			Matcher matcher = pattern.matcher(req.getRequestURI());
			if (matcher.matches()) {
				int width = -1;
				int height = -1;
				String blob = null;
				String format = null;
				if (matcher.group(1) != null) {
					blob = matcher.group(1);
				} else {
					resp.sendError(404, "Not found");
					return;
				}
				if (matcher.group(2) != null) {
					width = Integer.valueOf(matcher.group(2));
				}
				if (matcher.group(3) != null) {
					height = Integer.valueOf(matcher.group(3).replaceFirst("^x", ""));
				}
				if (matcher.group(4) != null) {
					format = matcher.group(4).substring(1);
				}

				if (width > 2000 || height > 2000) {
					resp.sendError(404, "Not found");
					return;
				}

				Json file = Thumbnailer.getCache(blob, format, width, height);

				if (file == null) {
					URL localRessource = req.getServletContext().getResource("/css/" + blob + ".png");
					if (localRessource == null) {
						resp.sendError(404, "Not found");
						return;
					}
					File tmp = File.createTempFile("pagebase_thumb", ".file");
					try {
						FileUtils.copyFile(new File(localRessource.getFile()), tmp);
						file = Thumbnailer.makeFile(tmp, blob, matcher.group(4), width, height);
					} finally {
						tmp.delete();
					}
				}
				if (file == null) {
					resp.sendError(404, "Not found");
					return;

				}
				try {
					doHeaders(req, resp, file);
					ServletOutputStream outStream = resp.getOutputStream();
					for (Binary bin : file.getList("binaries", Binary.class)) {
						outStream.write(bin.getData());
					}
					outStream.flush();
					outStream.close();
				} catch (Exception e) {

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
		resp.sendError(404, "Not found");

	}

	public static void doGetThumb(BaseServletRequest req, BaseServletResponse resp) throws IOException {

		Pattern pattern = Pattern.compile(".*@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png)?$");
		Matcher matcher = pattern.matcher(req.getRequestURI());
		String blob = URLDecoder.decode(req.getRequestURI().replaceAll("^/files/(.*)@([0-9]{1,4})(x[0-9]{1,4})?(\\.jpg|\\.png)?$", "$1"), StandardCharsets.UTF_8);


		if (matcher.matches()) {
			String format = null;
			int width = -1;
			int height = -1;

			if (matcher.group(1) != null) {
				width = Integer.valueOf(matcher.group(1));
				if (matcher.group(2) != null) {
					height = Integer.valueOf(matcher.group(2).replaceFirst("^x", ""));
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

			if (blobCache == null) {
				blobCache = Thumbnailer.makeDb(blob, format, width, height);
			}
			if (blobCache == null || !blobCache.containsKey("binaries")) {
				resp.setStatus(404);
				blobCache = Thumbnailer.getCache("error", format, width, height);
				if (blobCache == null) {
					blobCache = Thumbnailer.makeFile(new File(Settings.HUB_REPO + "/res/error.png"), "error", format, width, height);
				}
			}
			if (blobCache == null || !blobCache.containsKey("binaries")) {
				resp.sendTextError(404, "Not found");
				return;
			}

			doHeaders(req, resp, blobCache);
			ServletOutputStream outStream = resp.getOutputStream();
			try {
				for (Binary bin : blobCache.getList("binaries", Binary.class)) {
					outStream.write(bin.getData());
				}
				outStream.close();
			} catch (Exception e) {
				if (Fx.IS_DEBUG) {
					e.printStackTrace();
				}
			}
		} else {
			resp.sendTextError(404, "Not found");
		}

	}

	public static void doGetBlob(BaseServletRequest req, BaseServletResponse resp) throws IOException {

		Json file = Db.getDb("BlobFiles").find(Filters.eq("_id", req.getId())).first();
		if (file != null) {
			doHeaders(req, resp, file);
			Bson filter = Filters.eq("f", file.getId());
			Bson sort = Sorts.ascending("o");
			MongoCursor<Json> chunks = Db.getDb("BlobChunks").find(filter).hint(IndexBuilder.getHint("BlobChunks", "file")).sort(sort).iterator();
			ServletOutputStream outStream = resp.getOutputStream();
			while (chunks.hasNext()) {
				try {
					outStream.write(chunks.next().getBinary("b").getData());
					outStream.flush();
				} catch (Exception e) {
					if (Fx.IS_DEBUG) {
						e.printStackTrace();
					}
				}
			}
			chunks.close();
			outStream.close();
		} else {
			resp.sendTextError(404, "Not found");
		}
	}

	public static void doHeaders(HttpServletRequest req, HttpServletResponse resp, Json file) {

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
							"; filename=" + filename + "");
		} catch (Exception e) {
		}
		resp.setContentType(file.getString("type"));
	}

	public static Json getFiles(String user_id, String paging_str) {


		Aggregator grouper = new Aggregator("name", "type", "size", "date", "url");

		Paginer paginer = new Paginer(paging_str, "-date", 30);

		Bson paging_filter = paginer.getFilters();

		List<Bson> pipeline = new ArrayList<>();

		List<Bson> filters = new ArrayList<>();

		filters.add(Filters.eq("user", user_id));

		if (paging_filter != null) {
			filters.add(paging_filter);
		}

		pipeline.add(Aggregates.match(Filters.and(Filters.and(filters))));

		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());

		pipeline.add(Aggregates.project(grouper.getProjection().put("url", new Json("$concat", Arrays.asList(Settings.getCDNHttp(), "/files/", "$_id")))));

		pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

		pipeline.add(paginer.getLastSort());


		return paginer.getResult("BlobFiles", pipeline);

	}

	public static Json updateText(Json data, Users user) {
		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("_id", data.getId()));
		if (!user.getEditor()) {
			filters.add(Filters.eq("user", user.getId()));
		}
		return new Json("ok", Db.updateOne("BlobFiles", Filters.and(filters), new Json("$set", new Json("text", Fx.normalizePost(data.getString("text"))))).getModifiedCount() > 0);
	}

	public static boolean remove(Users user, String id) {
		Json file = Db.findByIdUser("BlobFiles", id, user.getId());
		if (file == null) {
			return false;
		}
		Db.deleteOne("BlobFiles", Filters.eq("_id", file.getId()));
		Db.deleteMany("BlobChunks", Filters.eq("f", file.getId()));
		return true;
	}

}
