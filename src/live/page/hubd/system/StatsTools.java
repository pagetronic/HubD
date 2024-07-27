/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.hubd.system;

import com.mongodb.client.model.*;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.db.utils.Pipeline;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.socket.SocketMessage;
import live.page.hubd.utils.Fx;
import org.bson.BsonUndefined;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


@WebListener
public class StatsTools implements ServletContextListener {

    private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);


    /**
     * Get all interested stats
     *
     * @return list for templating
     */
    public static Json getSimplesStats(TimeZone tz) {


        Pipeline pipeline = new Pipeline();

        pipeline.add(Aggregates.limit(1));
        Calendar cl = Calendar.getInstance(tz);

        //Today
        cl.add(Calendar.HOUR_OF_DAY, -24);
        Date start_date = cl.getTime();

        cl.add(Calendar.HOUR_OF_DAY, 24);
        Date stop_date = cl.getTime();
        pipeline.addAll(getPipelineStats("TODAY", start_date, stop_date));

        pipeline.add(Aggregates.project(new Json("_id", false).put("TODAY", true)));

        cl = Calendar.getInstance(tz);

        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);

        //Yesterday
        cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 1);
        start_date = cl.getTime();
        cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 1);
        stop_date = cl.getTime();
        pipeline.addAll(getPipelineStats("YESTERDAY", start_date, stop_date));

        //Last week
        cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 7);
        start_date = cl.getTime();
        cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 7);
        stop_date = cl.getTime();
        pipeline.addAll(getPipelineStats("LAST_WEEK", start_date, stop_date));

        //This month
        cl = Calendar.getInstance(tz);
        stop_date = cl.getTime();
        cl.add(Calendar.MONTH, -1);
        start_date = cl.getTime();
        pipeline.addAll(getPipelineStats("THIS_MONTH", start_date, stop_date));

        //Last month
        cl = Calendar.getInstance(tz);
        cl.add(Calendar.MONTH, -1);
        cl.set(Calendar.HOUR_OF_DAY, 0);
        cl.set(Calendar.MINUTE, 0);
        cl.set(Calendar.SECOND, 0);
        cl.set(Calendar.MILLISECOND, 0);
        cl.set(Calendar.DAY_OF_MONTH, 1);
        start_date = cl.getTime();
        cl.add(Calendar.MONTH, 1);
        stop_date = cl.getTime();
        pipeline.addAll(getPipelineStats("LAST_MONTH", start_date, stop_date));

        return Db.aggregate("Stats", pipeline).allowDiskUse(true).first();
    }


    /**
     * Get period interested stats
     *
     * @param start_date from date, null for all
     * @param stop_date  to date
     * @return pipeline for aggregate lookup
     */
    private static List<Bson> getPipelineStats(String key, Date start_date, Date stop_date) {

        Pipeline pipeline = new Pipeline();

        if (start_date != null) {
            pipeline.add(Aggregates.match(
                    Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))
            ));
        }


        pipeline.add(Aggregates.group(new Json("ip", "$ip").put("platform", "$platform"),
                Accumulators.first("unique", new Json("ip", "$ip").put("platform", "$platform")),
                Accumulators.sum("view", 1)

        ));

        pipeline.add(Aggregates.group(null,
                Accumulators.sum("unique", 1),
                Accumulators.sum("view", "$view")
        ));


        pipeline.add(Aggregates.project(new Json("_id", false)
                        .put("unique", "$unique")
                        .put("view", "$view")
                        .put("start", start_date)
                        .put("stop", stop_date)
                )
        );

        return Arrays.asList(
                Aggregates.lookup("Stats", pipeline, key),
                Aggregates.unwind("$" + key, new UnwindOptions().preserveNullAndEmptyArrays(true)),
                Aggregates.addFields(
                        new Field<>(key,
                                new Json("$cond", Arrays.asList(new Json("$eq", Arrays.asList("$" + key, new BsonUndefined())),
                                        new Json()
                                                .put("unique", 0)
                                                .put("view", 0)
                                                .put("start", start_date)
                                                .put("stop", stop_date)
                                        , "$" + key))
                        ))
        );
    }

    public static Json getStatsUrl(TimeZone tz) {
        Json stats = new Json();
        Calendar cl = Calendar.getInstance(tz);

        //Today
        cl.add(Calendar.HOUR_OF_DAY, -24);
        Date start_date = cl.getTime();

        cl.add(Calendar.HOUR_OF_DAY, 24);
        Date stop_date = cl.getTime();
        stats.put("TODAY", getStatsUrl(start_date, stop_date, 50));


        //This month
        cl = Calendar.getInstance(tz);
        stop_date = cl.getTime();
        cl.add(Calendar.MONTH, -1);
        start_date = cl.getTime();
        stats.put("THIS_MONTH", getStatsUrl(start_date, stop_date, 150));

        return stats;
    }

    private static List<Json> getStatsUrl(Date start_date, Date stop_date, int limit) {

        Pipeline pipeline = new Pipeline();
        if (start_date != null && stop_date != null) {
            pipeline.add(Aggregates.match(
                    Filters.and(Filters.gt("date", start_date), Filters.lte("date", stop_date))
            ));
        }


        pipeline.add(Aggregates.group(new Json("ip", "$ip").put("lng", "$lng").put("url", "$url"),
                Accumulators.first("unique", new Json("ip", "$ip").put("platform", "$platform")),
                Accumulators.sum("view", 1)

        ));

        pipeline.add(Aggregates.group(new Json("url", "$_id.url").put("lng", "$_id.lng"),
                Accumulators.sum("unique", 1),
                Accumulators.sum("view", "$view")
        ));

        pipeline.add(Aggregates.sort(Sorts.orderBy(Sorts.descending("unique"),
                Sorts.descending("view"))));
        pipeline.add(Aggregates.limit(limit));

        pipeline.add(Aggregates.project(new Json("_id", false)
                        .put("url", "$_id.url")
                        .put("lng", "$_id.lng")
                        .put("view", "$view")
                        .put("unique", "$unique")
                )
        );

        return Db.aggregate("Stats", pipeline).allowDiskUse(true).into(new ArrayList<>());
    }

    public static SocketMessage pushStats(String act, String ip, Json data) {

        Json stat = new Json();

        //RGPD // GDPR stat.put("sysid", data.getString("sysid")); AND IP??

        stat.put("lng", data.getString("lng"));
        stat.put("url", data.getString("location"));
        stat.put("platform", data.getString("platform"));
        stat.put("ip", Fx.crypt(ip));
        if (data.getString("user") != null) {
            stat.put("user", data.getString("user"));
        }
        stat.put("date", new Date());

        Db.save("Stats", stat);
        return new SocketMessage(act).put("_id", stat.getId());

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Fx.shutdownService(executor);
    }
}
