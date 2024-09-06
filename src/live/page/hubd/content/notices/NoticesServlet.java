/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

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

@Api(scope = "user")
@WebServlet(asyncSupported = true, name = "Notices", urlPatterns = {"/notices", "/notices/*"})
public class NoticesServlet extends HttpServlet {

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        Json notice = NoticesUtils.readClick(req.getId());
        if (notice == null) {
            resp.sendError(404, Language.get("UNKNOWN", req.getLng()));
            return;
        }
        resp.sendRedirect(notice.getString("url"), 301);

    }

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
        if (user == null) {
            resp.sendError(401, "PLEASE_LOGIN");
            return;
        }

        resp.sendResponse(NoticesUtils.getNotices(user, req.getString("start", null), req.getString("paging", null)));
    }


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        if (user == null) {
            resp.sendError(401, "PLEASE_LOGIN");
            return;
        }

        resp.sendResponse(switch (data.getString("action", "")) {
            case "all" -> Subscriptions.listSubscriptions(user, data.getString("paging", null));
            case "subscribe" -> Subscriptions.subscribe(user, data.getString("channel"), data.getString("type"));
            case "control" -> Subscriptions.control(user, data.getString("channel"));
            case "read" -> NoticesUtils.read(user.getId(), data);
            case "remove" -> NoticesUtils.remove(user.getId(), data);
            default -> new Json("error", "INVALID_DATA");
        });

    }
}
