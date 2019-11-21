/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.servlet.HttpServlet;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.session.Users;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.*;

@WebServlet(urlPatterns = {"/admin/stats"})
public class StatsAdmin extends HttpServlet {

	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("admin_active", "stats");

		List<Json> stats = new ArrayList<>();

		Calendar cl = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cl.set(Calendar.HOUR, 0);
		cl.set(Calendar.MINUTE, 0);
		cl.set(Calendar.SECOND, 0);
		Date start_date = cl.getTime();
		cl.set(Calendar.HOUR_OF_DAY, 24);
		Date stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 2);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 1);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 7);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 7);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 31);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 31);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) - 365);
		start_date = cl.getTime();
		cl.set(Calendar.DAY_OF_YEAR, cl.get(Calendar.DAY_OF_YEAR) + 365);
		stop_date = cl.getTime();
		stats.add(getStats(start_date, stop_date));

		req.setAttribute("stats", stats);

		resp.sendTemplate(req, "/admin/stats.html");
	}

	private Json getStats(Date start_date, Date stop_date) {
		Json rez = new Json("start", start_date).put("stop", stop_date);
		List<Bson> pipeline = new ArrayList<>();
		pipeline.add(Aggregates.match(Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))));
		pipeline.add(Aggregates.group(new Json("ip", "$ip").put("ua", "$ua")));
		pipeline.add(Aggregates.group(null, Accumulators.sum("unique", 1)));
		Json unique = Db.aggregate("Stats", pipeline).first();
		rez.put("unique", unique == null ? 0 : unique.getInteger("unique", 0));

		pipeline.clear();
		pipeline.add(Aggregates.match(Filters.and(Filters.gte("date", start_date), Filters.lt("date", stop_date))));
		pipeline.add(Aggregates.group(null, Accumulators.sum("view", 1)));
		Json view = Db.aggregate("Stats", pipeline).first();
		rez.put("view", view == null ? 0 : view.getInteger("view", 0));

		return rez;
	}

}
