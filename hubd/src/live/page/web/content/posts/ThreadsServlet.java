/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts;

import com.mongodb.client.model.Filters;
import live.page.web.content.posts.utils.DiscussAdmin;
import live.page.web.content.posts.utils.DiscussPoster;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.content.posts.utils.ThreadsUtils;
import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.cosmetic.tmpl.parsers.PostParser;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.regex.Pattern;

@Api(scope = "threads")
@WebServlet(urlPatterns = {"/threads", "/threads/*"})
public class ThreadsServlet extends HttpServlet {


	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		doGetAuth(req, resp, null);
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("active", "threads");
		Json thread = null;
		if (!req.getString("post", "").equals("")) {
			thread = ThreadsAggregator.getPost(req.getString("post", ""), user);
			req.setAttribute("highlight", "#" + req.getString("post", ""));
		}
		if (thread == null) {
			thread = ThreadsAggregator.getThread(req.getId(), user, req.getString("paging", "last"), user != null && user.getEditor() && req.contains("remove"));

		}

		if (thread == null) {
			resp.sendError(404, "Not found");
			return;
		}
		if (thread.getString("domain") != null && !req.getServerName().equals(thread.getString("domain"))) {
			resp.sendRedirect(Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url"), 301);
			return;
		}
		if (thread.get("remove") != null && !req.contains("remove")) {
			resp.sendError(404, "Not found");
			return;
		}

		req.setAttribute("thread_url", req.getString("url", ""));
		if (!req.contains("post") &&
				(!req.getRequestURI().equals(thread.getString("url")) || !req.getServerName().equals(thread.getString("domain")))) {
			if (thread.getString("domain") == null || thread.getString("url") == null) {
				resp.sendError(404, "Not found");
				return;
			}
			String base = "";
			if (!req.getServerName().equals(thread.getString("domain"))) {
				base = Settings.HTTP_PROTO + thread.getString("domain");
			}
			resp.sendRedirect(base + thread.getString("url"), 301);
			return;
		}

		req.setTitle(thread.getString("title", Language.get("NO_TITLE", req.getLng())));

		req.setBreadCrumb(thread.getListJson("breadcrumb"));
		if (req.contains("paging") || req.contains("post")) {
			req.addBreadCrumb(thread.getString("title"), thread.getString("url"));
		}
		try {
			req.setAttribute("menu", thread.getListJson("forums").get(0).getListJson("menu"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		req.setRobotsIndex(req.getQueryString() == null && thread.getBoolean("index", false), true);

		req.setAttribute("thread", thread);

		req.setCanonical(thread.getString("url"), "paging", "post");

		resp.sendTemplate(req, "/threads/thread.html");

	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException, ServletException {
		doGetApiAuth(req, resp, null);
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {

		if (req.getRequestURI().equals("/threads")) {
			resp.sendResponse(ThreadsAggregator.getThreads(
					Filters.and(
							Filters.eq("lng", req.getLng()),
							Filters.exists("remove", false),
							Filters.regex("parents", Pattern.compile("^Forums\\("))
					),
					req.getString("paging", null), false));
			return;
		}

		if (req.getId() == null) {
			resp.sendError(404, "Not found");
			return;
		}

		if (req.contains("simple")) {
			resp.sendResponse(ThreadsAggregator.getSimplePost(req.getId()));
			return;
		}

		Json postdata = ThreadsAggregator.getThread(req.getId(), user, req.getString("paging", "last"), user != null && user.getEditor() && req.contains("remove"));

		if (postdata == null) {
			resp.sendError(404, "Not found");
			return;
		}
/*
		postdata.getJson("posts").getListJson("result").forEach(json -> {
			json.put("html", PostParser.parse(json.getText("text"), json.getListJson("docs"), json.getListJson("links")));
		});
		postdata.put("html", PostParser.parse(postdata.getText("text"), postdata.getListJson("docs"), postdata.getListJson("links")));
*/
		resp.sendResponse(postdata);

	}

	@Override
	public void doPostApiPublic(ApiServletRequest req, ApiServletResponse resp, Json data) throws IOException {
		doPostApiAuth(req, resp, data, null);
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");

		switch (data.getString("action")) {
			case "send":
				rez = DiscussPoster.post(data, user, ServletUtils.realIp(req));
				break;

			case "search":
				rez = ThreadsUtils.search(data);
				break;

			case "get":
				rez = ThreadsUtils.edit(data.getId(), user);
				break;

			case "comment":
				rez = DiscussPoster.comment(data, user, req.getLng(), ServletUtils.realIp(req));
				break;
			case "remove":
				rez = DiscussPoster.remove(data, user);
				break;

			case "history":
				rez = DiscussPoster.history(data.getString("post_id"), data.getInteger("comment", -1));
				break;
		}

		resp.sendResponse(rez);
	}


	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

		Json rez = new Json();

		switch (data.getString("action")) {
			case "move":
				rez = DiscussAdmin.move(data.getId(), data.getList("parents"), user);
				break;

			case "split":
				rez = DiscussAdmin.split(data, user);
				break;

			case "relocate":
				rez = DiscussAdmin.relocatePostThread(data.getId(), data.getString("to"), user);
				break;

			case "parents_add":
				rez = DiscussAdmin.addParent(data.getId(), data.getString("parent"), user);
				break;

			case "parents_remove":
				rez = DiscussAdmin.removeParent(data.getId(), data.getString("parent"), user);
				break;

			case "parents_sort":
				rez = DiscussAdmin.sortParents(data.getId(), data.getList("parents"), user);
				break;

			case "pages_add":
				rez = DiscussAdmin.addPages(data.getId(), data.getString("page_id"), user);
				break;

			case "pages_remove":
				rez = DiscussAdmin.removePages(data.getId(), data.getString("page_id"), user);
				break;

			case "pages_sort":
				rez = DiscussAdmin.sortPages(data.getId(), data.getList("pages"), user);
				break;
		}

		if (!rez.isEmpty()) {
			resp.sendResponse(rez);
		} else {
			doPostApiAuth(req, resp, data, user);
		}
	}
}
