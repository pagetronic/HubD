package live.page.web.admin.utils;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.web.content.pages.PagesAutoLink;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@WebListener
public class TempExtractLink implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		Fx.log("> autolink update start");
		Map<String, List<String>> store = new HashMap<>();
		MongoCursor<Json> pages = Db.find("Pages").iterator();

		while (pages.hasNext()) {
			Json page = pages.next();
			String text = page.getText("text", "");
			Pattern pattern = Pattern.compile("\\[Pages\\(([a-z0-9]+)\\) ?([^]]+)]", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(text);
			while (matcher.find()) {
				String id = matcher.group(1);
				String title = matcher.group(2);
				List<String> keyword = store.getOrDefault(id, new ArrayList<>());
				if (!keyword.contains(title)) {
					keyword.add(title);
					Collections.sort(keyword, (s, s1) -> s1.length() - s.length());

					store.put(id, keyword);
				}
			}

		}
		pages.close();
		Fx.log("autolink update make keyword done");

		for (Map.Entry<String, List<String>> keyword : store.entrySet()) {
			String id = keyword.getKey();
			List<String> keywords = keyword.getValue();
			Db.save("Revisions", new Json().put("origine", id).put("keywords", keywords).put("edit", new Date()));
			Db.updateOne("Pages", Filters.eq("_id", id), new Json("$set", new Json("keywords", keywords)));
		}

		Fx.log("autolink update set keyword done");

		MongoCursor<Json> pages_link = Db.find("Pages").iterator();
		while (pages_link.hasNext()) {
			Json page = pages_link.next();
			PagesAutoLink.keywords(page.getId(), page.getList("keywords"), null);
		}
		pages_link.close();
		Fx.log("> autolink update done");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}
}