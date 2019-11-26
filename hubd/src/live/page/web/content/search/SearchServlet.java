/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.content.search;

import live.page.web.system.Language;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(urlPatterns = {"/search", "/hashtag/*", "/opensearch.xml"})
public class SearchServlet extends HttpServlet {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException {


		if (req.getRequestURI().equals("/opensearch.xml")) {
			resp.sendTemplate(req, "/opensearch.xml");
			return;
		}

		if (req.getRequestURI().matches("/hashtag/?")) {
			resp.sendRedirect("/search", 301);
			return;
		}


		req.setAttribute("active", "home");
		String query = SearchUtils.cleanQuery(req.getString("q", ""));
		String title = Language.get("SEARCH_RESULTS", req.getLng());

		if (req.getRequestURI().startsWith("/hashtag/")) {
			query = req.getRequestURI().replaceAll("^/hashtag/(.*)", "$1");
			query = SearchUtils.cleanQuery(URLDecoder.decode(query, StandardCharsets.UTF_8));
			String url = "/hashtag/" + URLEncoder.encode(query.toLowerCase(), StandardCharsets.UTF_8);

			if (!req.getRequestURI().equals(url)) {
				resp.sendRedirect(url, 301);
				return;
			}

			if (query.contains("_")) {
				query = query.replace("_", " ");
			}

			title = Fx.ucfirst(query.replace("_", " "));

			req.setRobotsIndex(req.getQueryString() == null, true);
			req.setCanonical(url);
			req.setAttribute("search_url", url);

		} else if (!query.equals("")) {

			query = SearchUtils.cleanQuery(query);
			title += " « " + query + " »";

			req.setRobotsIndex(false, true);
			req.setCanonical("/search");
			req.setAttribute("search_url", "/search?q=" +
					URLEncoder.encode(req.getString("q", ""), StandardCharsets.UTF_8) +
					(req.contains("type") ? "&amp;type=" + req.getString("type", null) : ""));

		} else {

			req.setRobotsIndex(req.getQueryString() == null, true);
			req.setCanonical("/search");
		}

		req.setAttribute("q", query.replace("\"", "&#34;").replace("_", " "));
		req.setTitle(title);


		req.setAttribute("search", SearchUtils.search(query, req.getLng(),
				req.getString("type", ""),
				req.getString("paging", null)
		));


		resp.sendTemplate(req, "/search/search.html");
	}


	@Override
	public void doGetApiPublic(ApiServletRequest req, ApiServletResponse resp) throws IOException {

		String query = SearchUtils.cleanQuery(req.getString("q", ""));
		resp.sendResponse(SearchUtils.search(query, req.getLng(),
				req.getString("type", ""),
				req.getString("paging", null)
		));

	}

}
