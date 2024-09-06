/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.threads;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.content.pages.PagesAggregator;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.utils.ServletUtils;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@Api(scope = "threads")
@WebServlet(asyncSupported = true, urlPatterns = {"/threads", "/threads/*"})
public class ThreadsServlet extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        req.setAttribute("active", "threads");

        Json thread = ThreadsAggregator.getThread(req.getId("last"), user, req.getString("paging", req.getRequestURI().endsWith("/last") ? "last" : "first"));


        if (thread == null) {
            resp.sendError(404, req);
            return;
        }
        if (thread.getString("domain") != null && !req.getServerName().equals(thread.getString("domain"))) {
            resp.sendRedirect(Settings.HTTP_PROTO + thread.getString("domain") + thread.getString("url"), 301);
            return;
        }
        if (thread.get("remove") != null && !req.contains("remove")) {
            resp.sendError(404, req);
            return;
        }

        req.setAttribute("thread_url", req.getString("url", ""));
        if (!req.contains("post") &&
                (!req.getRequestURI().equals(thread.getString("url")) || !req.getServerName().equals(thread.getString("domain")))) {
            if (thread.getString("domain") == null || thread.getString("url") == null) {
                resp.sendError(404, req);
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

        req.setRobotsIndex(req.getQueryString() == null && thread.getInteger("replies", 0) > 0, true);

        req.setAttribute("thread", thread);

        req.setCanonical(thread.getString("url"), "paging", "post");

        resp.sendTemplate(req, "/threads/thread.html");

    }

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {


        if (req.getRequestURI().equals("/threads/noreply")) {
            if (!req.getString("id", "").isEmpty()) {
                resp.sendResponse(PagesAggregator.getThreads(req.getString("id", ""), req.getString("paging", null), true, user));
                return;
            }

            resp.sendResponse(
                    ThreadsAggregator.getThreads(
                            Filters.and(
                                    Filters.ne("parents.type", "Posts"),
                                    Filters.eq("replies", 0),
                                    Filters.eq("lng", req.getLng())
                            ), req.getString("paging", null), user)
            );
            return;
        }

        if (req.getRequestURI().equals("/threads/reply")) {
            if (!req.getString("id", "").isEmpty()) {
                resp.sendResponse(PagesAggregator.getThreads(req.getString("id", ""), req.getString("paging", null), false, user));
                return;
            }
            resp.sendResponse(
                    ThreadsAggregator.getThreads(
                            Filters.and(
                                    Filters.ne("parents.type", "Posts"),
                                    Filters.ne("replies", 0),
                                    Filters.eq("lng", req.getLng())
                            ), req.getString("paging", null), user)
            );
            return;
        }
        if (req.getRequestURI().equals("/threads/my/replies") && user != null) {

            resp.sendResponse(
                    ThreadsAggregator.getThreads(
                            Filters.and(
                                    Filters.eq("parents.type", "Posts"),
                                    Filters.eq("user", user.getId())
                            ),
                            req.getString("paging", null), user)
            );
            return;
        }
        if (req.getRequestURI().equals("/threads/my/messages") && user != null) {

            resp.sendResponse(
                    ThreadsAggregator.getThreads(
                            Filters.and(
                                    Filters.ne("parents.type", "Posts"),
                                    Filters.eq("user", user.getId())
                            ),
                            req.getString("paging", null), user)
            );
            return;
        }

        if (req.getId() == null) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }


        Json postdata = ThreadsAggregator.getThread(req.getId("last"), user, req.getString("paging", req.getRequestURI().endsWith("/last") ? "last" : "first"));

        if (postdata == null) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }

        resp.sendResponse(postdata);

    }


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        if (user == null) {
            resp.sendResponse(switch (data.getString("action")) {
                case "post" -> DiscussPoster.post(data, null, ServletUtils.realIp(req));
                default -> new Json("error", "INVALID_DATA");
            });
            return;
        }

        resp.sendResponse(switch (data.getString("action")) {
            case "post" -> DiscussPoster.post(data, user, ServletUtils.realIp(req));
            case "search" -> ThreadsUtils.search(data);
            case "get" -> ThreadsUtils.edit(data.getId(), user);
            case "remove" -> DiscussPoster.remove(data, user);
            case "history" -> DiscussPoster.history(data.getString("post_id"), data.getInteger("comment", -1));
            default -> new Json("error", "INVALID_DATA");

        });
    }

}
