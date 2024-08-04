/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import live.page.hubd.system.Settings;
import live.page.hubd.system.servlet.utils.LogsUtils;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.utils.Fx;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@WebFilter(asyncSupported = true, urlPatterns = {"/", "/*"})
public class RootFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req_, ServletResponse resp_, FilterChain chain) throws IOException, ServletException {
        String host = req_.getServerName();
        if (host == null) {
            host = "127.0.0.1";
        }
        if (host.equals("localhost")) {
            return;
        }
        if (!Settings.getDomains().contains(host) && !host.equals(Settings.STANDARD_HOST)
                && !host.equals(Settings.HOST_CDN) && !host.equals(Settings.HOST_API)) {
            ServletUtils.redirect301(Settings.getFullHttp(), resp_);
            return;
        }

        BaseServletResponse resp = new BaseServletResponse(resp_, ((HttpServletRequest) req_).getRequestURI());

        if (((HttpServletRequest) req_).getMethod().equals("OPTIONS")) {
            String origin = ((HttpServletRequest) req_).getHeader("Origin");
            if (origin != null) {
                try {
                    URI uriOrigin = new URI(origin);
                    boolean authorized = origin.startsWith("http://localhost:") || Settings.domainAvailable(uriOrigin.getHost()) && uriOrigin.getScheme().equals(Settings.HTTP_PROTO.replace("://", ""));
                    if (Fx.IS_DEBUG || authorized) {
                        resp.setHeader("Access-Control-Allow-Origin", origin);
                    }
                } catch (Exception ignore) {
                }
            }

            resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Authorization, User-Agent");
            resp.setIntHeader("Access-Control-Max-Age", Settings.COOKIE_DELAY);
            resp.setHeader("Vary", "Origin");
            return;
        }

        req_.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        LogsUtils.pushLog(req_);

        String requestURI = ((HttpServletRequest) req_).getRequestURI();

        if (host.equals(Settings.HOST_CDN) && (requestURI.startsWith("/canvaskit") || requestURI.startsWith("/assets"))) {
            resp.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET");
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            WebServletResponse.setHeaderMaxCache(resp);
            req_.getServletContext().getNamedDispatcher("default").forward(req_, resp);
            return;
        }
        if (req_.getAttribute("requestURI") == null) {
            req_.setAttribute("requestURI", requestURI);
        }
        req_.setAttribute("ip", ServletUtils.realIp(req_));

        resp.setHeader("Server", Settings.PROJECT_NAME);

        if (!host.equals(Settings.HOST_API) && !(req_.getScheme()).equals(Settings.HTTP_PROTO.replace("://", ""))) {
            resp.sendRedirect(Settings.HTTP_PROTO + host + requestURI, 301);
            return;
        }

        if ((!host.equals(Settings.STANDARD_HOST) || !req_.getScheme().equals(Settings.HTTP_PROTO.replace("://", "")))
                && (!host.equals(Settings.HOST_CDN) || !req_.getScheme().equals(Settings.HTTP_PROTO.replace("://", "")))
                && !host.equals(Settings.HOST_API)
                && !Settings.domainAvailable(host)
        ) {
            try {
                resp.sendRedirect(Settings.getFullHttp());
            } catch (Exception ignore) {
            }
            return;
        }


        if (!Settings.domainAvailable(host) && host.equals(Settings.STANDARD_HOST) && !Arrays.asList("/auth", "/token", "/oauth").contains(requestURI)) {
            // redirect to standard Host
            req_.getRequestDispatcher("/STANDARD_HOST").forward(req_, resp);
            return;
        }

        if (req_.getServerName().equals(Settings.STANDARD_HOST)) {
            if (requestURI.equals("/") || requestURI.equals("/index.html")) {
                req_.getRequestDispatcher("/index").forward(req_, resp);
                return;
            }
        }

        if (host.equals(Settings.HOST_CDN) && requestURI.startsWith("/ui/")) {
            req_.getRequestDispatcher(requestURI).forward(req_, resp);
            return;
        }

        if (host.equals(Settings.HOST_API)) {

            if (requestURI.endsWith(".xml") || requestURI.endsWith(".json")) {
                req_.getRequestDispatcher(requestURI.replaceAll("\\.(xml|json)$", "")).forward(req_, resp);
                return;
            }
        }

        if (!requestURI.equals("/") && !requestURI.equals("/robots.txt") && req_.getServletContext().getResource(requestURI) != null) {

            req_.getServletContext().getNamedDispatcher("default").forward(req_, resp);
            return;
        }

        chain.doFilter(req_, resp);
    }

    @Override
    public void destroy() {

    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

}
