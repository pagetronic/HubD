/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.content.pages.PagesAggregator;
import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Aggregator;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.LightServlet;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import org.bson.BsonUndefined;

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
public class SitemapServlet extends LightServlet {
    private static final DateFormat urlDate = new SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss-SSS");
    private static final DateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");

    static {
        urlDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        isoDate.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final int maximumUrls = 1000;


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
            if (!matcher.group(2).isEmpty()) {
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
        try (MongoCursor<Json> threads = getSitemapThreads(date, lng).iterator()) {
            while (threads.hasNext()) {
                Json thread = threads.next();
                writer.write("<url>");
                writer.write("<loc>" + Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url") + "</loc>");
                //writer.write(" <date>" + isoDate.format(thread.getDate("date")) + "</date>");
                writer.write("<lastmod>" + isoDate.format(thread.getDate("update")) + "</lastmod>");
                writer.write("</url>");
                writer.flush();
            }
        }
    }

    private void writePages(Date date, String lng, PrintWriter writer) {
        try (MongoCursor<Json> pages = getSitemapPages(date, lng).iterator()) {
            while (pages.hasNext()) {
                Json page = pages.next();
                writer.write("<url>");
                writer.write("<loc>" + Settings.HTTP_PROTO + page.getString("domain") + page.getString("url") + "</loc>");
                //writer.write(" <date>" + isoDate.format(page.getDate("date")) + "</date>");
                writer.write("<lastmod>" + isoDate.format(page.getDate("update")) + "</lastmod>");
                writer.write("</url>");
                writer.flush();
            }
        }
    }

    private void writePagesIndex(String lng, PrintWriter writer) {
        int skip = 0;
        while (true) {
            Json pages = Db.aggregate("Pages", Arrays.asList(
                    Aggregates.match(Filters.eq("lng", lng)),
                    Aggregates.sort(Sorts.descending("date")),
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
                            Filters.gt("replies", 0)
                    )),
                    Aggregates.sort(Sorts.descending("date")),
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

    public AggregateIterable<Json> getSitemapPages(Date date, String lng) {

        Aggregator grouper = new Aggregator("id", "date", "breadcrumb", "update", "lng", "domain", "url");

        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(Filters.and(Filters.eq("lng", lng),
                Filters.gte("date", date))));

        pipeline.add(Aggregates.sort(Sorts.ascending("date")));
        pipeline.add(Aggregates.limit(maximumUrls));

        pipeline.makeBreadcrumb();
        pipeline.add(PagesAggregator.makeUrl());

        pipeline.add(Aggregates.project(grouper.getProjection()
                .put("update", new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$update", new BsonUndefined())), "$date", "$update")))
                .put("domain", Pipeline.getDomainExpose("$lng"))
        ));

        pipeline.add(Aggregates.project(grouper.getProjectionOrder()));

        pipeline.add(Aggregates.sort(Sorts.descending("update")));
        return Db.aggregate("Pages", pipeline);


    }

    private AggregateIterable<Json> getSitemapThreads(Date date, String lng) {

        Aggregator grouper = new Aggregator("date", "update", "url", "lng", "domain", "replies", "index", "breadcrumb");
        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.match(
                Filters.and(
                        Filters.ne("replies", 0),
                        Filters.eq("lng", lng),
                        Filters.gte("date", date))));
        pipeline.add(Aggregates.sort(Sorts.ascending("date")));

        pipeline.add(Aggregates.limit(maximumUrls));

        pipeline.makeBreadcrumb();
        pipeline.add(Aggregates.addFields(new Field<>("url", new Json("$concat", Arrays.asList("/threads/", "$_id")))));

        pipeline.add(Aggregates.project(grouper.getProjection()
                        .put("domain", Pipeline.getDomainExpose("$lng"))
                )

        );

        pipeline.add(Aggregates.sort(Sorts.descending("update")));
        return Db.aggregate("Posts", pipeline);
    }
}
