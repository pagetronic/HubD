package live.page.web.admin.utils;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.system.sessions.Users;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoLink {
	public static Json autolink(String id, List<String> keywords, Users user) {

		Json rez = new Json("ok", true).put("links", new ArrayList<>());
		MongoCursor<Json> pages = Db.find("Pages",
				Filters.and(Filters.ne("_id", id),
						Filters.regex("text", Pattern.compile("(" + StringUtils.join(keywords, "|") + ")", Pattern.CASE_INSENSITIVE))
				)
		).iterator();

		pageloop:
		while (pages.hasNext()) {
			Json page = pages.next();
			String text = page.getText("text", "");
			if (text.contains(id)) {
				continue;
			}
			List<String> groups = new ArrayList<>();
			for (String pat : new String[]{"\\[([^]]+)]", "<a[^>]+>([^<]+)</a>", "=([^\n]+)=([ ]+)?\n"}) {
				Pattern pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(text);
				while (matcher.find()) {
					text = text.replace(matcher.group(), "@@@###" + groups.size() + "###@@@");
					groups.add(matcher.group());
				}
			}

			for (String keyword : keywords) {
				if (keyword.equals("")) {
					continue;
				}
				Pattern pattern = Pattern.compile("([\\r\\n\\t ,’'ʼ]|^)(" + keyword + ")([.,!?; ])", Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(text);
				if (matcher.find()) {
					String start = matcher.group(1);
					String title = matcher.group(2);
					String punct = matcher.group(3);

					text = matcher.replaceFirst(start + "[Pages(" + id + ") " + title + "]" + punct);

					for (int i = groups.size() - 1; i >= 0; i--) {
						text = text.replace("@@@###" + i + "###@@@", groups.get(i));
					}

					Db.save("Revisions", new Json().put("origine", page.getId()).put("editor", user.getId()).put("text", text).put("edit", new Date()));
					Db.updateOne("Pages", Filters.eq("_id", page.getId()), new Json().put("$set", new Json().put("text", text).put("update", new Date())));
					rez.add("links", "/" + page.getString("url"));
					continue pageloop;
				}
			}
		}

		pages.close();


		return rez;
	}
}
