/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.blobs;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@Api(scope = "files")
@WebServlet(asyncSupported = true, name = "Blobs Servlet", urlPatterns = {"/blobs", "/blobs/*"})
public class BlobsServlet extends HttpServlet {

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
        if (user == null) {
            resp.sendError(401, "PLEASE_LOGIN");
            return;
        }

        if (req.getRequestURI().equals("/blobs")) {
            resp.sendResponse(BlobsDb.getUserFiles(user.getId(), req.getString("paging", null)));
        } else {
            resp.sendResponse(BlobsDb.getChildFiles(req.getId(), req.getString("paging", null)));
        }
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        Json rez = switch (data.getString("action", "")) {
            case "text" -> BlobsDb.updateText(data, user);
            case "delete" -> BlobsUtils.delete(data.getId(), user);
            case "parent" -> BlobsUtils.parent(data.getId(), data.getString("parent"), user, data.getBoolean("remove", false));
            default -> new Json("error", "INVALID_DATA");
        };

        resp.sendResponse(rez);
    }

}
