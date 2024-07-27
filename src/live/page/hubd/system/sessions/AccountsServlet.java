/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.sessions;

import jakarta.servlet.annotation.WebServlet;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.utils.Api;
import live.page.hubd.system.servlet.wrapper.ApiServletRequest;
import live.page.hubd.system.servlet.wrapper.ApiServletResponse;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.utils.Fx;

import java.io.IOException;

@Api(scope = "accounts")
@WebServlet(asyncSupported = true, urlPatterns = {"/accounts", "/accounts/*"})
public class AccountsServlet extends HttpServlet {
    //TODO: gérer les autorisations financières

    @Override
    public void doGetHttp(WebServletRequest req, WebServletResponse resp, Users user) throws IOException {

        req.setTitle(Fx.ucfirst(Language.get("SUBACCOUNTS", req.getLng())));
        req.setAttribute("profile_active", "accounts");

        req.setAttribute("accounts", AccountsUtils.getSubAccounts(user.getId(), req.getString("paging", null)));
        req.setAttribute("base_activate", Settings.getFullHttp(req.getLng()) + "/activate/");

        resp.sendTemplate(req, "/profile/accounts.html");

    }

    @Override
    public void doGetApi(ApiServletRequest req, ApiServletResponse resp, Users user) throws IOException {
        resp.sendResponse(AccountsUtils.getSubAccounts(user.getId(), req.getString("paging", null)));
    }

    @Override
    public void doPostApi(ApiServletRequest req, ApiServletResponse resp, Json data, Users user) throws IOException {

        Json rez = switch (data.getString("action", "")) {
            case "create" -> AccountsUtils.generateUser(data.getString("name", Language.get("ANONYMOUS", user.getString("locale", "en"))), data.getString("email"), user.getId());
            case "name" -> AccountsUtils.renameUser(data.getString("name", ""), data.getString("id", ""), user.getId());
            case "reset" -> AccountsUtils.initUser(data.getString("id", ""), user.getId());
            default -> new Json("error", "INVALID_DATA");
        };

        resp.sendResponse(rez);

    }
}
