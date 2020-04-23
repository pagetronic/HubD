/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin.utils.scrap;

import live.page.web.system.json.Json;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.scrap.ScrapDataUtils;

import java.io.IOException;

public class ScrapAdmin {

	public static void doGetEditor(WebServletRequest req, WebServletResponse resp) throws IOException {

		req.setAttribute("scrap_active", true);

		if (req.contains("url")) {
			Json data = ScrapDataUtils.scrapNoCache(req.getString("url", ""));
			resp.sendText(data.getText("title"));
			resp.sendText("\n");
			resp.sendText("\n");
			resp.sendText(data.getList("logos").toString());
			resp.sendText("\n");
			resp.sendText("\n");
			resp.sendText(data.getText("description"));

			//resp.sendText(Jsoup.parse(HttpClient.getAsFacebook(req.getString("url", ""))).text());
			return;
		}

		req.setAttribute("sites", ScrapAdminUtils.getScraps(req.getLng(), req.getString("sort", "-date"), req.getString("paging", null)));
		resp.sendTemplate(req, "/admin/scrap.html");
	}

	public static Json doPostApiEditor(Json data) throws IOException {

		Json rez = new Json();
		switch (data.getString("type")) {
			case "refresh":
				rez = new Json("ok", ScrapAdminUtils.refresh(data.getId()));
				break;
			case "save":
				rez = ScrapAdminUtils.save(
						data.getId(),
						data.getString("url"),
						data.getString("cleaner"),
						data.getString("lng"),
						data.getListJson("scraps"),
						data.getBoolean("aggregater", false),
						data.getString("link", ""),
						data.getString("exclude", "")
				);
				break;
			case "edit":
				rez = ScrapAdminUtils.edit(data.getId());
				break;
			case "delete":
				rez = ScrapAdminUtils.delete(data.getId());
				break;
		}

		return rez;
	}
}
