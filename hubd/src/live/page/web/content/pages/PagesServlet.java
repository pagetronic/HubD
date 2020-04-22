/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.pages;

import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;
import live.page.web.utils.Fx;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;


@WebServlet(name = "Pages Servlet", urlPatterns = {"/pages"})
public class PagesServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {
		doGetAuth(req, resp, null);
	}

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

		String uri = req.getRequestURI();
		if (uri.equals("/")) {
			uri = "/index";
		}

		Json page = PagesAggregator.getPage(uri, Settings.getLang(req.getServerName()), req.getString("paging", null), user != null && user.getEditor());

		if (page == null) {
			String redirect = PagesUtils.getPossibleRedirect(uri);
			if (redirect != null) {
				resp.sendRedirect(redirect, 301);
				return;
			}
		}

		if (page == null || page.getString("url") == null) {
			resp.sendError(404, "Not found");
			return;
		}
		if (!page.getString("url", "").equals(uri) || !page.getString("domain", "").equals(req.getServerName())) {

			String base = "";
			if (!req.getServerName().equals(Settings.STANDARD_HOST) && !Settings.LANGS_DOMAINS.containsValue(req.getServerName())) {
				base = Settings.getFullHttp();
			}

			resp.sendRedirect(base + page.getString("url"), 301);
			return;
		}

		resp.setStatus(200);

		req.setRobotsIndex(true);

		req.setCanonical(page.getString("url", "").equals("/index") ? "/" : page.getString("url"), "paging");

		req.setTitle(page.getString("top_title", "").equals("") ? page.getString("title") : page.getString("top_title"));
		req.setBreadCrumbTitle(page.getString("title"));
		req.setBreadCrumb(page.getListJson("breadcrumb"));
		if (!page.getString("intro", "").equals("")) {
			req.setDescription(Fx.truncate(Fx.textbrut(page.getString("intro")), 400));
		}

		if (page.getString("logo") != null) {
			req.setImageOg(page.getString("logo"));
		}

		if (req.contains("paging")) {
			page.remove("text");
		}

		req.setAttribute("page", page);

		req.setAttribute("active", "documents");

		if (uri.equals("/copyright")) {
			req.setAttribute("active", "copyright");
		}

		resp.sendTemplate(req, "/pages/page.html");
	}

	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException {
		doGetApiAuth(req, resp, null);
	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
		if (req.getRequestURI().startsWith("/orph")) {
			resp.sendResponse(PagesAggregator.getOrph());
		} else if (req.getRequestURI().startsWith("/pages")) {
			resp.sendResponse(PagesAggregator.getPages(null, 30, req.getString("paging", null)));
		} else {
			Json page = PagesAggregator.getPageDomainLng(req.getRequestURI(), req.getString("lng", req.getString("domain", null)), req.getString("paging", null), user != null && user.getEditor());
			if (page != null) {
				resp.sendResponse(page);
			} else {
				resp.sendError(404, "Not found");
			}
		}
	}

}

