/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.json.Json;
import live.page.web.system.db.paginer.Paginer;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


@WebServlet(urlPatterns = {"/countries"})
public class CountriesUtils extends HttpServlet {


	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {
		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action", "")) {
			case "search":
				rez = search(data.getString("search"), data.getBoolean("value", false), data.getString("paging", null));
				break;
		}
		resp.sendResponse(rez);
	}

	public static Json search(String search, boolean value, String paging) {

		List<Bson> pipeline = new ArrayList<>();
		List<Bson> filters = new ArrayList<>();

		Paginer paginer = new Paginer(paging, "name", 15);

		Bson paging_filter = paginer.getFilters();

		if (paging_filter != null) {
			filters.add(paging_filter);
		}
		if (search != null && !search.equals("")) {
			if (value) {
				filters.add(Filters.eq("_id", search));
			} else {
				filters.add(Filters.regex("name", Pattern.compile(search, Pattern.CASE_INSENSITIVE)));
			}
		}
		if (filters.size() > 0) {
			pipeline.add(Aggregates.match(filters.size() > 1 ? Filters.and(filters) : filters.get(0)));
		}
		pipeline.add(paginer.getFirstSort());
		pipeline.add(paginer.getLimit());
		pipeline.add(paginer.getLastSort());

		return paginer.getResult("Countries", pipeline);
	}

	/*
		@Override
		public void init(ServletConfig config) {

			try {
				for (live.page.web.utils.json.Json country : new live.page.web.utils.json.Json(FileUtils.readFileToString(new File(Settings.REPO + "/res/countries.json"))).getListJson("countries")) {
					country.set("_id", country.getId()).remove("id");
					Db.getDb("Countries").insertOne(country);
					Logs.log(country);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	*/
	public static String getCountry(String id) {
		return Db.findById("Countries", id).getString("name");
	}

}