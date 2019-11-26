/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet;

import live.page.web.system.Settings;
import live.page.web.system.servlet.utils.BruteLocker;
import live.page.web.system.servlet.utils.ServletUtils;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.BaseCookie;
import live.page.web.utils.Fx;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;

public abstract class FullServlet implements javax.servlet.Servlet {

	/**
	 * Test the service needed, API or Standard web
	 */
	@Override
	public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {

		HttpServletRequest httpreq = (HttpServletRequest) request;
		HttpServletResponse httpresp = (HttpServletResponse) response;

		if (BruteLocker.isBan(ServletUtils.realIp(httpreq))) {
			Fx.log("BruteForcer: " + ServletUtils.realIp(httpreq));
			httpresp.setStatus(401);
			httpresp.setIntHeader("Refresh", BruteLocker.DELAY);
			BaseCookie.clearAuth(httpreq, httpresp);
			httpresp.getWriter().write("reload " + BruteLocker.DELAY + "s");
			return;
		}

		String host = httpreq.getServerName();

		if (host == null || (!host.equals(Settings.STANDARD_HOST) && !host.equals(Settings.HOST_API) && !host.equals(Settings.HOST_CDN) && !Settings.LANGS_DOMAINS.containsValue(host))) {
			ServletUtils.redirect301(Settings.getFullHttp(), httpresp);
			return;
		}

		if (host.equals(Settings.HOST_API)) {
			boolean xml = false;
			try {
				Object requestURI = httpreq.getAttribute("requestURI");
				xml = new URI(requestURI == null ? httpreq.getRequestURI() : requestURI.toString()).getPath().endsWith(".xml");
			} catch (Exception e) {
			}
			serviceApi(new ApiServletRequest(request), new ApiServletResponse(response, xml));
			return;
		}

		if (host.equals(Settings.HOST_CDN)) {
			httpreq.getRequestDispatcher("/files").forward(httpreq, httpresp);
			return;
		}

		serviceWeb(new WebServletRequest(request), new WebServletResponse(response));
	}

	abstract void serviceApi(ApiServletRequest req, ApiServletResponse resp);

	abstract void serviceWeb(WebServletRequest req, WebServletResponse resp);

	private ServletConfig config;

	@Override
	public void init(ServletConfig config) {
		this.config = config;
		init();
	}

	public void init() {
	}

	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	@Override
	public String getServletInfo() {
		return Settings.SITE_TITLE;
	}

}
