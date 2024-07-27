/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.hubd.system.servlet.utils;

import jakarta.servlet.ServletException;
import live.page.hubd.system.servlet.HttpServlet;
import live.page.hubd.system.servlet.wrapper.WebServletRequest;
import live.page.hubd.system.servlet.wrapper.WebServletResponse;
import live.page.hubd.system.sessions.Users;

import java.io.IOException;

public class GlobalControl extends HttpServlet {
    @Override
    public boolean controlWeb(Users user, WebServletRequest req, WebServletResponse resp) throws ServletException, IOException {
        req.setAttribute("tos", TosTester.isNotSeen(user, req.getLng()));
        return true;
    }

}
