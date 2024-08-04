/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.pages;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/edit", "/edit/*", "/draft", "/draft/*"})
public class PagesEditing extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {
        req.setRobotsIndex(false);

        if (!user.getAdmin()) {
            resp.sendError(404, "NOT_FOUND");
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
                        uri.startsWith("/edit") && req.getId() != null ? req.getId() : null, user);
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
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        Json rez = new Json("error", "INVALID_DATA");

        if (user.getAdmin()) {
            switch (data.getString("action")) {
                case "publish" -> rez = PagesUtils.save(data, user);
                case "draft" -> rez = PagesUtils.draft(data, user);
                case "search" -> rez = PagesUtils.search(data);
                case "forums_add" -> rez = PagesUtils.addForum(data.getId(), data.getString("forum_id"));
                case "forums_remove" -> rez = PagesUtils.remove(data.getId(), data.getString("forum_id"));
                case "forums_sort" -> rez = PagesUtils.sortForums(data.getId(), data.getList("forums"));
                case "parents_add" -> rez = PagesUtils.addParents(data.getId(), data.getString("parent_id"), user);
                case "parents_remove" -> rez = PagesUtils.removeParents(data.getId(), data.getString("parent_id"), user);
                case "parents_sort" -> rez = PagesUtils.sortParents(data.getId(), data.getList("parents"), user);
                case "children_add" -> rez = PagesUtils.addChildrens(data.getId(), data.getString("children_id"), user);
                case "children_remove" -> rez = PagesUtils.removeChildrens(data.getId(), data.getString("children_id"), user);
                case "children_sort" -> rez = PagesUtils.sortChildrens(data.getId(), data.getList("children"), user);
                case "children_pages" -> rez = PagesUtils.childrenPage(data.getString("parent"));
                case "getKeywords" -> rez = PagesUtils.getKeywords(data.getId());
                case "keywords" -> rez = PagesUtils.keywords(data.getId(), data.getList("keywords"), user);
            }
        }
        resp.sendResponse(rez);
    }

}
