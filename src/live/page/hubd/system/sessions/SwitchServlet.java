/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import com.mongodb.client.model.Filters;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.content.users.UsersUtils;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.socket.SocketPusher;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(scope = "accounts")
@WebServlet(asyncSupported = true, name = "Switch Servlet", urlPatterns = {"/switch", "/switch/*"})
public class SwitchServlet extends HttpServlet {

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        if (user == null) {
            resp.sendError(401, "PLEASE_LOGIN");
            return;
        }

        resp.sendResponse(switch (data.getString("action", "")) {
            case "search" -> UsersUtils.searchChildren(data, user);
            default -> new Json("error", "INVALID_DATA");
        });

    }


    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException, ServletException {

        String previous_user = user.getId();
        Json session = req.getHeader("Authorization") != null ?
                BaseSession.getAuthorization(req) :
                BaseSession.getSession(req);

        if (session == null) {
            resp.sendError(404, "NOT_FOUND");
            return;
        }

        if (req.getId() == null || req.getId().equals("switch")) {
            if (session.get("original") == null) {
                resp.sendError(404, "NOT_FOUND");
                return;
            }
            List<String> originals = session.getList("original");
            session.put("user", originals.get(originals.size() - 1));
            originals.remove(originals.size() - 1);
            if (!originals.isEmpty()) {
                session.put("original", originals);
            } else {
                session.remove("original");
            }
            Db.save("Sessions", session);


        } else {

            List<Bson> filters = new ArrayList<>();
            if (!user.getAdmin()) {
                filters.add(Filters.eq("parent", user.getId()));
            }
            filters.add(Filters.eq("_id", req.getId()));
            Json user_switch = Db.find("Users", Filters.and(filters)).first();

            if (user_switch == null) {
                resp.sendError(404, "NOT_FOUND");
                return;
            }
            session.add("original", user.getId());
            session.put("user", user_switch.getId());
            Db.save("Sessions", session);

        }
        SocketPusher.send("user", previous_user, new Json("action", "reload"));

        resp.sendResponse(new Json("ok", true));
    }

}
