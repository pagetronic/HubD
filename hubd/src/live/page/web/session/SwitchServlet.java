/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.session;

import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.servlet.HttpServlet;
import live.page.web.servlet.utils.Api;
import live.page.web.servlet.wrapper.ApiServletRequest;
import live.page.web.servlet.wrapper.ApiServletResponse;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.socket.SocketPusher;
import live.page.web.users.UsersUtils;
import live.page.web.utils.json.Json;
import org.bson.conversions.Bson;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(scope = "accounts")
@WebServlet(name = "Switch Servlet", urlPatterns = {"/switch", "/switch/*"})
public class SwitchServlet extends HttpServlet {
	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {


		String previous_user = user.getId();
		Json session = BaseSession.getSession(req);

		if (req.getId() == null) {
			if (session.get("original") == null) {
				resp.sendError(404, "Not found");
				return;
			}
			List<String> originals = session.getList("original");
			session.put("user", originals.get(originals.size() - 1));
			originals.remove(originals.size() - 1);
			if (originals.size() > 0) {
				session.put("original", originals);
			} else {
				session.remove("original");
			}
			Db.save("Sessions", session);
			resp.sendRedirect(req.getReferer("/"));

		} else {

			List<Bson> filters = new ArrayList<>();
			if (!user.getEditor()) {
				filters.add(Filters.eq("parent", user.getId()));
			}
			filters.add(Filters.eq("_id", req.getId()));
			Json user_switch = Db.find("Users", Filters.and(filters)).first();

			if (user_switch == null) {
				resp.sendError(404, "Not found");
				return;
			}
			session.add("original", user.getId());
			session.put("user", user_switch.getId());
			Db.save("Sessions", session);
			String refer = req.getReferer("/");
			resp.sendRedirect(refer.equals("/accounts") ? "/" : refer);

		}
		SocketPusher.send("user", previous_user, new Json("action", "reload"));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

		Json rez = new Json("error", "INVALID_DATA");
		switch (data.getString("action")) {
			case "create":
				if (user.getEditor()) {
					rez = UsersUtils.create(data, user);
				}
				break;
			case "search":
				rez = UsersUtils.searchChilds(data, user);
				break;
		}

		resp.sendResponse(rez);
	}
}
