/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet;

import live.page.web.content.posts.utils.ForumsAggregator;
import live.page.web.system.Settings;
import live.page.web.system.servlet.utils.LogsUtils;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.BaseServletResponse;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@WebFilter(asyncSupported = true, urlPatterns = {"/", "/*"})
public class RootFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp_, FilterChain chain) throws IOException, ServletException {

		String host = req.getServerName();
		if (host == null) {
			host = "127.0.0.1";
		}
		if (host.equals("localhost")) {
			return;
		}

		BaseServletResponse resp = new BaseServletResponse(resp_);

		req.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
		LogsUtils.pushLog(req);


		if (Settings.MENU_FORUM) {
			String lng = req.getParameter("lng") != null ? req.getParameter("lng") : Settings.getLang(req.getServerName());
			String url = ((HttpServletRequest) req).getRequestURI().replaceAll("^/([^/.]+).*", "$1");
			req.setAttribute("menus", ForumsAggregator.getForumsRoot(url, lng));
		}

		String requestURI = ((HttpServletRequest) req).getRequestURI();
		if (req.getAttribute("requestURI") == null) {
			req.setAttribute("requestURI", requestURI);
		}
		req.setAttribute("ip", ServletUtils.realIp(req));

		resp.setHeader("Server", Settings.PROJECT_NAME);

		if (!host.equals(Settings.HOST_API) && !(req.getScheme()).equals(Settings.HTTP_PROTO.replace("://", ""))) {
			resp.sendRedirect(Settings.HTTP_PROTO + host + requestURI, 301);
			return;
		}

		if ((!host.equals(Settings.STANDARD_HOST) || !req.getScheme().equals(Settings.HTTP_PROTO.replace("://", "")))
				&& (!host.equals(Settings.HOST_CDN) || !req.getScheme().equals(Settings.HTTP_PROTO.replace("://", "")))
				&& !host.equals(Settings.HOST_API)
				&& !Settings.LANGS_DOMAINS.containsValue(host)
		) {
			try {
				resp.sendRedirect(Settings.getFullHttp());
			} catch (Exception e) {
			}
			return;
		}


		if (!Settings.LANGS_DOMAINS.containsValue(host) && host.equals(Settings.STANDARD_HOST) && !Arrays.asList("/auth", "/token", "/oauth").contains(requestURI)) {
			// redirect to standard Host
			req.getRequestDispatcher("/STANDARD_HOST").forward(req, resp);
			return;
		}

		if (req.getServerName().equals(Settings.STANDARD_HOST)) {
			if (requestURI.equals("/") || requestURI.equals("/index.html")) {
				req.getRequestDispatcher("/index").forward(req, resp);
				return;
			}
		}

		if (host.equals(Settings.HOST_CDN) && requestURI.startsWith("/ui/")) {
			req.getRequestDispatcher(requestURI).forward(req, resp);
			return;
		}

		if (host.equals(Settings.HOST_API)) {

			if (requestURI.endsWith(".xml") || requestURI.endsWith(".json")) {
				req.getRequestDispatcher(requestURI.replaceAll("\\.(xml|json)$", "")).forward(req, resp);
				return;
			}
		}

		if (!requestURI.equals("/") && !requestURI.equals("/robots.txt") && req.getServletContext().getResource(requestURI) != null) {

			req.getServletContext().getNamedDispatcher("default").forward(req, resp);
			return;
		}

		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(FilterConfig filterConfig) {

	}

}
