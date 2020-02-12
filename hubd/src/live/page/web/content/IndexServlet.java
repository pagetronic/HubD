/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content;

import com.mongodb.client.model.Filters;
import live.page.web.content.pages.PagesAggregator;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.regex.Pattern;

@WebServlet(urlPatterns = {"/index"}, displayName = "index")
public class IndexServlet extends HttpServlet {
	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {

		req.setAttribute("active", "home");

		if (!req.getRequestURI().equals("/")) {
			resp.sendError(404, "Not found");
			return;
		}

		resp.setStatus(200);
		req.setCanonical("/", "paging");
		req.setRobotsIndex(req.getQueryString() == null, true);

		if (Language.exist("SITE_DESCRIPTION", req.getLng())) {
			req.setDescription(Language.get("SITE_DESCRIPTION", req.getLng()));
		}

		req.setAttribute("pages", PagesAggregator.getPages(Filters.eq("lng", Settings.getLang(req.getServerName())), 40, null));
		req.setAttribute("threads", ThreadsAggregator.getThreads(
				Filters.and(
						Filters.or(Filters.eq("index", true), Filters.gt("replies", 0)),
						Filters.eq("lng", Settings.getLang(req.getServerName())),
						Filters.exists("remove", false),
						Filters.regex("parents", Pattern.compile("^Forums\\("))
				),
				req.getString("paging", null), false)
		);

		resp.sendTemplate(req, "/index.html");

	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException, ServletException {
		resp.sendResponse(new Json("say", "hello"));
	}
}
