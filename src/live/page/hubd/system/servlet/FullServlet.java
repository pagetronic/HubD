/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import live.page.hubd.system.Settings;
import live.page.hubd.system.servlet.utils.BruteLocker;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.BaseCookie;
import live.page.hubd.system.utils.Fx;

import java.io.IOException;
import java.net.URI;


public abstract class FullServlet extends BaseServlet {


    /**
     * Test the service needed, API or Standard web
     */
    @Override
    public void asyncService(ServletRequest request, ServletResponse response) throws IOException, ServletException {


        HttpServletRequest httpreq = (HttpServletRequest) request;
        HttpServletResponse httpresp = (HttpServletResponse) response;


        String origin = httpreq.getHeader("Origin");
        if (origin != null) {
            boolean authorized = false;
            try {
                URI uriOrigin = new URI(origin);
                authorized = Settings.domainAvailable(uriOrigin.getHost()) ;
            } catch (Exception ignore) {
            }

            if (authorized) {
                httpresp.setHeader("Access-Control-Expose-Headers", "X-User");
                httpresp.setHeader("Access-Control-Allow-Origin", origin);
                httpresp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                httpresp.setHeader("Access-Control-Allow-Credentials", "true");
                httpresp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Authorization");
                httpresp.setIntHeader("Access-Control-Max-Age", Settings.COOKIE_DELAY);
                httpresp.setHeader("Vary", "Origin");
            }

            if (httpreq.getMethod().equalsIgnoreCase("OPTIONS")) {
                WebServletResponse.setHeaderMaxCache(httpresp);
                httpresp.getWriter().write("");
                return;
            }
            if (!authorized) {
                httpresp.setStatus(405);
                httpresp.getWriter().write("");
                return;
            }
        }

        if (BruteLocker.isBan(ServletUtils.realIp(httpreq))) {
            Fx.log("BruteForcer: " + ServletUtils.realIp(httpreq));
            httpresp.setStatus(401);

            if (!Fx.IS_DEBUG) {
                httpresp.setIntHeader("Refresh", BruteLocker.DELAY);
            }
            BaseCookie.clearAuth(httpreq, httpresp);
            httpresp.getWriter().write("reload " + BruteLocker.DELAY + "s");
            return;
        }

        String host = httpreq.getServerName();

        if (host == null || (!host.equals(Settings.STANDARD_HOST) && !host.equals(Settings.HOST_API) && !host.equals(Settings.HOST_CDN) && !Settings.domainAvailable(host))) {
            ServletUtils.redirect301(Settings.getFullHttp(), httpresp);
            return;
        }

        if (host.equals(Settings.HOST_API)) {
            boolean xml = false;
            try {
                Object requestURI = httpreq.getAttribute("requestURI");
                xml = new URI(requestURI == null ? httpreq.getRequestURI() : requestURI.toString()).getPath().endsWith(".xml");
            } catch (Exception ignore) {
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

    @Override
    public void init(ServletConfig config) {
        this.config = config;
        init();
    }

    public void init() {
    }


}
