/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.sessions;

import live.page.web.system.Language;
import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.utils.Api;
import live.page.web.system.servlet.wrapper.ApiServletRequest;
import live.page.web.system.servlet.wrapper.ApiServletResponse;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Fx;

import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@Api(scope = "accounts")
@WebServlet(urlPatterns = {"/accounts", "/accounts/*"})
public class AccountsServlet extends HttpServlet {
	//TODO: gérer les autorisations financières

	@Override
	public void doGetAuth(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

		req.setTitle(Fx.ucfirst(Language.get("SUBACCOUNTS", req.getLng())));
		req.setAttribute("profile_active", "accounts");

		req.setAttribute("accounts", AccountsUtils.getSubAccounts(user.getId(), req.getString("paging", null)));
		req.setAttribute("base_activate", Settings.getFullHttp(req.getLng()) + "/activate/");

		resp.sendTemplate(req, "/profile/accounts.html");

	}

	@Override
	public void doGetApiAuth(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
		resp.sendResponse(AccountsUtils.getSubAccounts(user.getId(), req.getString("paging", null)));
	}

	@Override
	public void doPostApiAuth(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {


		Json rez = new Json("error", "INVALID_DATA");

		switch (data.getString("action", "")) {
			case "create":
				rez = AccountsUtils.generateUser(data.getString("name", Language.get("ANONYMOUS", user.getString("locale", "en"))), data.getString("email"), user.getId());
				break;
			case "name":
				rez = AccountsUtils.renameUser(data.getString("name", ""), data.getString("id", ""), user.getId());
				break;
			case "reset":
				rez = AccountsUtils.initUser(data.getString("id", ""), user.getId());
				break;
		}

		resp.sendResponse(rez);

	}
}
