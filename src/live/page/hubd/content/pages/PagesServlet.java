/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.pages;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.content.threads.ThreadsUtils;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.utils.Fx;

import java.io.IOException;

@WebServlet(asyncSupported = true, name = "Pages Servlet", urlPatterns = {"/"})
public class PagesServlet extends HttpServlet {


    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

        if (req.getServerName().equals(Settings.HOST_CDN)) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }
        if (req.getRequestURI().equals("/")) {
            req.getRequestDispatcher("/index").forward(req, resp);
            return;
        }

        String uri = req.getRequestURI();
        if (uri.equals("/")) {
            uri = "/index";
        }

        Json page = PagesAggregator.getPage(uri, Settings.getLang(req.getServerName()), req.getString("paging", null), user);

        if (page == null) {
            String redirect = PagesUtils.getPossibleRedirect(uri);
            if (redirect == null) {
                redirect = ThreadsUtils.getPossibleRedirect(uri);
            }
            if (redirect != null) {
                resp.sendRedirect(redirect, 301);
                return;
            }


            resp.sendError(404, req);
            return;

        }

        if (page.getString("url") == null) {
            resp.sendError(404, req);
            return;
        }
        if (!page.getString("url", "").equals(uri) || !page.getString("domain", "").equals(req.getServerName())) {

            String base = "";
            if (!req.getServerName().equals(Settings.STANDARD_HOST) && !Settings.domainAvailable(req.getServerName())) {
                base = Settings.getFullHttp();
            }

            resp.sendRedirect(base + page.getString("url"), 301);
            return;
        }

        resp.setStatus(200);
        if (req.getString("paging", "").isEmpty()) {
            req.setRobotsIndex(true);
        } else {
            req.setRobotsIndex(false, true);

        }
        req.setCanonical(page.getString("url", "").equals("/index") ? "/" : page.getString("url"), "paging");

        req.setTitle(page.getString("top_title", "").isEmpty() ? page.getString("title") : page.getString("top_title"));
        req.setBreadCrumbTitle(page.getString("title"));
        req.setBreadCrumb(page.getListJson("breadcrumb"));
        if (!page.getString("intro", "").isEmpty()) {
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
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {

        if (req.getRequestURI().startsWith("/pages")) {

            resp.sendResponse(PagesAggregator.getPages(null, 30, req.getString("paging", null)));

        } else if (req.getRequestURI().startsWith("/menu")) {
            resp.sendResponse(PagesAggregator.getBase(req.getLng()));
        } else {
            Json page = PagesAggregator.getPageDomainLng(req.getRequestURI(), req.getString("lng", req.getString("domain", null)), req.getString("paging", null), user);
            if (page != null) {
                resp.sendResponse(page);
            } else {
                resp.sendError(404, "NOT_FOUND");
            }
        }
    }

}

