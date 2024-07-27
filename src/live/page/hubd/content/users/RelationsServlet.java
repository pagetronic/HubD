/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.users;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, urlPatterns = {"/relations"})
public class RelationsServlet extends HttpServlet {


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {
        Json rez = new Json("error", "NOT_FOUND");

        switch (data.getString("action", "")) {
            case "search":
                rez = RelationsUtils.search(data.getString("search", ""), data.get("filter"), data.getString("paging", null), user);
                break;
            default:

                break;
        }
        resp.sendResponse(rez);

    }
}
