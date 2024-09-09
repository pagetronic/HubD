/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.content.likes;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

@WebServlet(asyncSupported = true, name = "Likes Servlet", urlPatterns = {"/likes"})
public class LikesServlet extends HttpServlet {


    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException, ServletException {
        resp.sendResponse(switch (data.getString("action", "")) {
            case "like" -> LikesUtils.like(data.getString("type"), data.getString("parent"), data.getBoolean("like", false), user);
            default -> new Json("error", "UNKNOWN_METHOD");
        });
    }

}

