/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.pages;

import live.page.web.system.Language;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api
@WebServlet(urlPatterns = {"/edit", "/edit/*", "/draft", "/draft/*"})
public class PagesEditing extends HttpServlet {

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {
		req.setRobotsIndex(false);

		if (!user.getEditor()) {
			resp.sendError(404, "Not found");
		}

		String uri = req.getRequestURI();
		if (uri.equals("/")) {
			uri = "/index";
		}

		Json revision = null;
		if (!uri.equals("/draft")) {

			if (uri.startsWith("/draft/")) {
				revision = PagesUtils.revision(req.getId());

			} else {
				revision = PagesUtils.revision(
						!uri.startsWith("/edit") && req.getId() == null ? uri : null,
						req.getServerName(),
						uri.startsWith("/edit") && req.getId() != null ? req.getId() : null
				);
			}

			if (revision != null) {

				req.setTitle(Language.get("PAGE_EDIT", req.getLng()) + " " + revision.getString("title"));
			}
		}

		if (uri.startsWith("/draft")) {
			req.setAttribute("drafts", PagesUtils.getDrafts());
		}
		req.setAttribute("active", "doc");
		req.setAttribute("revision", revision);

		if (revision == null && req.getQueryString() != null) {
			String[] urls = req.getQueryString().split("/");
			req.setAttribute("url", urls[urls.length - 1]);
		}

		resp.sendTemplate(req, "/pages/edit.html");

	}

	@Override
	public void doPostApiEditor(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json();
		switch (data.getString("action")) {
			case "publish":
				rez = PagesUtils.save(data, user);
				break;
			case "draft":
				rez = PagesUtils.draft(data, user);
				break;
			case "search":
				rez = PagesUtils.search(data);
				break;

			case "forums_add":
				rez = PagesUtils.addForum(data.getId(), data.getString("forum_id"));
				break;
			case "forums_remove":
				rez = PagesUtils.remove(data.getId(), data.getString("forum_id"));
				break;
			case "forums_sort":
				rez = PagesUtils.sortForums(data.getId(), data.getList("forums"));
				break;

			case "parents_add":
				rez = PagesUtils.addParents(data.getId(), data.getString("parent_id"), user);
				break;
			case "parents_remove":
				rez = PagesUtils.removeParents(data.getId(), data.getString("parent_id"), user);
				break;
			case "parents_sort":
				rez = PagesUtils.sortParents(data.getId(), data.getList("parents"), user);
				break;

			case "childrens_add":
				rez = PagesUtils.addChildrens(data.getId(), data.getString("children_id"), user);
				break;
			case "childrens_remove":
				rez = PagesUtils.removeChildrens(data.getId(), data.getString("children_id"), user);
				break;
			case "childrens_sort":
				rez = PagesUtils.sortChildrens(data.getId(), data.getList("childrens"), user);
				break;

			case "childrens_pages":
				rez = PagesUtils.childrensPage(data.getString("parent"));
				break;

			case "getKeywords":
				rez = PagesUtils.getKeywords(data.getId());
				break;
			case "keywords":
				rez = PagesUtils.keywords(data.getId(), data.getList("keywords"), user);
				break;
		}
		resp.sendResponse(rez);
	}

}
