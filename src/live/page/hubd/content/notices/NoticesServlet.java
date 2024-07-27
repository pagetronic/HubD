/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.notices;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.content.notices.push.PushSubscriptions;
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

        resp.sendResponse(NoticesUtils.getNotices(user, req.getString("paging", null)));
    }


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {
        if (user == null) {
            resp.sendResponse(switch (data.getString("action", "")) {
                case "get" -> PushSubscriptions.listConfigFollows(data.getJson("config"), data.getString("paging", null));
                case "subscribe" -> PushSubscriptions.subscribe(null, data.getString("lng"), data.getJson("device"), data.getJson("config"), data.getString("obj"));
                case "unsubscribe" -> PushSubscriptions.unsubscribe(data.getJson("config"), data.getString("obj"));
                case "control" -> PushSubscriptions.control(data.getJson("config"), data.getString("obj"));
                case "unpush" -> PushSubscriptions.remove(data.getId(), data.getJson("config"), null);
                case "test" -> PushSubscriptions.test(data.getJson("config"), data.getString("lng"));
                default -> new Json("error", "INVALID_DATA");
            });
            return;
        }

        resp.sendResponse(switch (data.getString("action", "")) {
            case "read" -> NoticesUtils.read(user.getId(), data);
            case "remove" -> NoticesUtils.remove(user.getId(), data);
            case "subscribe" -> PushSubscriptions.subscribe(user.getId(), data.getString("lng"), data.getJson("device"), data.getJson("config"), data.getString("obj"));
            case "unpush" -> PushSubscriptions.remove(data.getId(), data.getJson("config"), user);
            default -> new Json("error", "INVALID_DATA");
        });

    }
}
