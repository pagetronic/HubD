/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.posts;

import com.mongodb.client.model.Filters;
import live.page.web.content.posts.utils.ForumsAdmin;
import live.page.web.content.posts.utils.ForumsAggregator;
import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.Settings;
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

@Api(scope = "threads")
@WebServlet(urlPatterns = {"/forums"})
public class ForumsServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {
		doGetAuth(req, resp, null);
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setAttribute("active", "threads");

		Json forum = ForumsAggregator.getForum(req.getRequestURI(), req.getServerName(), req.getString("paging", null), user, user != null && user.getEditor() && req.contains("remove"));

		if (forum == null) {
			resp.sendError(404, "Not found");
			return;
		}

		if (!req.getRequestURI().equals(forum.getString("url", "/")) || !req.getServerName().equals(forum.getString("domain"))) {
			String base = "";
			if (!req.getServerName().equals(forum.getString("domain"))) {
				base = Settings.HTTP_PROTO + forum.getString("domain");
			}
			ServletUtils.redirect301(base + forum.getString("url", "/"), resp);
			return;
		}


		req.setAttribute("threads", forum.getJson("threads"));

		req.setRobotsIndex(req.getQueryString() == null, true);


		if (forum.getString("title") != null) {
			String title = forum.getString("title");
			req.setTitle(title);
			req.setBreadCrumbTitle(title);
		}

		if (!forum.getString("meta_title", "").equals("")) {
			req.setMetaTitle(forum.getString("meta_title", ""));
		}

		if (forum.getString("text") != null) {
			req.setDescription(forum.getString("text"));
		}

		req.setBreadCrumb(forum.getListJson("breadcrumb"));
		if (req.contains("paging") || req.contains("post")) {
			req.addBreadCrumb(forum.getString("title"), forum.getString("url"));
		}

		req.setAttribute("menu", forum.getListJson("menu"));


		req.setCanonical(forum.getString("url"), "paging");
		req.setRobotsIndex(req.getQueryString() == null, true);


		req.setAttribute("forum", forum);
		resp.sendTemplate(req, "/threads/forum.html");

	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException {
		doGetApiAuth(req, resp, null);
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {



		if (!req.contains("lng")) {
			resp.setStatus(500);
			resp.sendResponse(new Json("error", "NO_LANG"));
			return;
		}

		if (req.getRequestURI().endsWith("/root")) {
			resp.sendResponse(ForumsAggregator.getAllForumRoot(req.getLng()));
			return;
		}

		if (req.getRequestURI().endsWith("/home")) {
			resp.sendResponse(ThreadsAggregator.getThreads(Filters.and(Filters.gt("replies", 0), Filters.eq("lng", null)), req.getString("paging", null), false));
			return;
		}
		Json rez = ForumsAggregator.getForum(req.getId() != null ? req.getId() : req.getRequestURI(), req.getString("lng", req.getString("domain", null)), req.getString("paging", null), user, user != null && user.getEditor() && req.contains("remove"));

		if (rez == null) {
			resp.sendError(404, "Not found");
			return;
		}

		resp.sendResponse(rez);

	}


	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {

		Json rez = new Json();

		switch (data.getString("action")) {
			case "search":
				rez = ForumsAdmin.search(data);
				break;
			case "create":
				rez = ForumsAdmin.saveForum(data.getJson("forum"), user);
				break;

			case "edit":
				rez = ForumsAdmin.editForum(data.getId());
				break;

			case "childrens":
				rez = ForumsAdmin.getChildrens(data.getString("parent"));
				break;

			case "children_add":
				rez = ForumsAdmin.addChildren(data.getId(), data.getString("children"));
				break;

			case "childrens_sort":
				rez = ForumsAdmin.sortChildrens(data.getId(), data.getList("order"));
				break;

			case "order":
				rez = ForumsAdmin.orderForum(data.getJson("data"));
				break;

			case "parents_add":
				rez = ForumsAdmin.addParent(data.getId(), data.getString("parent"));
				break;

			case "parents_remove":
				rez = ForumsAdmin.removeParent(data.getId(), data.getString("parent"));
				break;

			case "parents_sort":
				rez = ForumsAdmin.sortParents(data.getId(), data.getList("parents"));
				break;

			case "root_sort":
				rez = ForumsAdmin.sortRoot(data.getList("order"));
				break;

			case "pages_sort":
				rez = ForumsAdmin.sortPages(data.getId(), data.getList("pages"));
				break;
		}


		if (!rez.isEmpty()) {
			resp.sendResponse(rez);
		} else {
			super.doPostApiAuth(req, resp, data, user);
		}
	}
}
