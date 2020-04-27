/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.content.pages.PagesAggregator;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.BaseServlet;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {"/sitemap.xml", "/sitemap/*"}, displayName = "sitemap")
public class SitemapServlet extends BaseServlet {
	private final int maximumUrls = 5000;
	private static final DateFormat urlDate = new SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss-SSS");
	private static final DateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");

	static {
		urlDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		isoDate.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

		long start = System.currentTimeMillis();

		if (req.getRequestURI().equals("/sitemap.xml")) {
			resp.setContentType("application/xml; charset=utf-8");
			resp.setHeaderNoCache();
			PrintWriter writer = resp.getWriter();
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
			writePagesIndex(req.getLng(), writer);
			writeThreadsIndex(req.getLng(), writer);
			writer.write("</sitemapindex>");
			writer.write("<!-- " + (System.currentTimeMillis() - start) + "ms -->");
			writer.close();
			return;
		}

		Pattern pattern = Pattern.compile("^/sitemap/(pages|threads)(.*)?\\.xml$");
		Matcher matcher = pattern.matcher(req.getRequestURI());

		if (matcher.find()) {

			Date date = new Date(0);
			if (!matcher.group(2).equals("")) {
				try {
					date = urlDate.parse(matcher.group(2));
				} catch (Exception ignore) {
					ServletUtils.redirect301("/sitemap.xml", resp);
					return;
				}
			}

			resp.setContentType("application/xml; charset=utf-8");
			resp.setHeaderNoCache();

			PrintWriter writer = resp.getWriter();
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
			if (matcher.group(1).equals("pages")) {
				writePages(date, req.getLng(), writer);
			} else if (matcher.group(1).equals("threads")) {
				writeThreads(date, req.getLng(), writer);
			}
			writer.write("</urlset>");
			writer.write("<!-- " + (System.currentTimeMillis() - start) + "ms -->");
			writer.close();
			return;
		}

		resp.sendError(404, "NOT_FOUND");
	}

	private void writeThreads(Date date, String lng, PrintWriter writer) {
		for (Json thread : ThreadsAggregator.getSitemapThreads(date, lng, maximumUrls)) {
			writer.write("<url>");
			writer.write("<loc>" + Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url") + "</loc>");
			//writer.write(" <date>" + isoDate.format(thread.getDate("date")) + "</date>");
			writer.write("<lastmod>" + isoDate.format(thread.getDate("update")) + "</lastmod>");
			writer.write("</url>");
			writer.flush();
		}
	}

	private void writePages(Date date, String lng, PrintWriter writer) {
		for (Json page : PagesAggregator.getSitemapPages(date, lng, maximumUrls)) {
			writer.write("<url>");
			writer.write("<loc>" + Settings.HTTP_PROTO + page.getString("domain") + page.getString("url") + "</loc>");
			//writer.write(" <date>" + isoDate.format(page.getDate("date")) + "</date>");
			writer.write("<lastmod>" + isoDate.format(page.getDate("update")) + "</lastmod>");
			writer.write("</url>");
			writer.flush();
		}
	}

	private void writePagesIndex(String lng, PrintWriter writer) {
		int skip = 0;
		while (true) {
			Json pages = Db.aggregate("Pages", Arrays.asList(
					Aggregates.match(Filters.eq("lng", lng)),
					Aggregates.sort(Sorts.ascending("date")),
					Aggregates.skip(skip),
					Aggregates.limit(maximumUrls),
					Aggregates.project(new Json("date", true)),
					Aggregates.group(null, Arrays.asList(
							Accumulators.first("date", "$date"),
							Accumulators.push("ids", "$_id")
					)),
					new Json("$lookup",
							new Json("from", "Pages")
									.put("let", new Json("ids", "$ids"))
									.put("pipeline",
											Arrays.asList(
													new Json("$match", new Json("$expr",
															new Json("$in", Arrays.asList("$_id", "$$ids"))
													)
													),
													Aggregates.sort(Sorts.descending("update")),
													Aggregates.limit(1)
											)
									).put("as", "pages")
					),
					Aggregates.unwind("$pages"),
					Aggregates.project(new Json().put("date", true).put("update", "$pages.update"))
			)).first();
			if (pages == null) {
				break;
			}
			String id = skip == 0 ? "" : urlDate.format(pages.getDate("date"));
			writer.write("<sitemap>");
			writer.write("<loc>" + Settings.getFullHttp(lng) + "/sitemap/pages" + id + ".xml</loc>");
			writer.write("<lastmod>" + isoDate.format(pages.getDate("update")) + "</lastmod>");
			writer.write("</sitemap>");
			writer.flush();
			skip = skip + maximumUrls;
		}
	}

	private void writeThreadsIndex(String lng, PrintWriter writer) {
		int skip = 0;
		while (true) {
			Json threads = Db.aggregate("Posts", Arrays.asList(
					Aggregates.match(Filters.and(
							Filters.eq("lng", lng),
							Filters.or(Filters.eq("index", true), Filters.gt("replies", 0)),
							Filters.regex("parents", Pattern.compile("^Forums\\([0-9a-z]+\\)", Pattern.CASE_INSENSITIVE))
					)),
					Aggregates.sort(Sorts.ascending("date")),
					Aggregates.skip(skip),
					Aggregates.limit(maximumUrls),
					Aggregates.project(new Json("date", true)),
					Aggregates.group(null, Arrays.asList(
							Accumulators.first("date", "$date"),
							Accumulators.push("ids", "$_id")
					)),
					new Json("$lookup",
							new Json("from", "Posts")
									.put("let", new Json("ids", "$ids"))
									.put("pipeline",
											Arrays.asList(
													new Json("$match", new Json("$expr",
															new Json("$in", Arrays.asList("$_id", "$$ids"))
													)
													),
													Aggregates.sort(Sorts.descending("update")),
													Aggregates.limit(1)
											)
									).put("as", "threads")
					),
					Aggregates.unwind("$threads"),
					Aggregates.project(new Json().put("date", true).put("update", "$threads.last.date"))
			)).first();
			if (threads == null) {
				break;
			}
			String id = skip == 0 ? "" : urlDate.format(threads.getDate("date"));
			writer.write("<sitemap>");
			writer.write("<loc>" + Settings.getFullHttp(lng) + "/sitemap/threads" + id + ".xml</loc>");
			writer.write("<lastmod>" + isoDate.format(threads.getDate("update")) + "</lastmod>");
			writer.write("</sitemap>");
			writer.flush();
			skip = skip + maximumUrls;
		}
	}
}
