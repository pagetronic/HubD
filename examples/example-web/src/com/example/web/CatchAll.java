/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.example.web;

import com.mongodb.client.model.Filters;
import live.page.web.db.Db;
import live.page.web.servlet.BaseServlet;
import live.page.web.servlet.wrapper.BaseServletRequest;
import live.page.web.servlet.wrapper.BaseServletResponse;
import live.page.web.utils.Settings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(name = "Catch All", urlPatterns = {"/"})
public class CatchAll extends BaseServlet {
	@Override
	public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException {

		if (req.getServerName().equals(Settings.HOST_CDN)) {
			resp.sendError(404, "Not found");
			return;
		}

		if (req.getRequestURI().equals("/")) {
			req.getRequestDispatcher("/index").forward(req, resp);
			return;
		}


		if (req.getRequestURI().matches("^.*?/([A-Z0-9]{" + Db.DB_KEY_LENGTH + "})$")) {
			req.getRequestDispatcher("/threads").forward(req, resp);
		}

		if (Db.exists("Forums", Filters.and(
				Filters.eq("url", req.getRequestURI().replaceAll("^/([^/.]+).*", "$1")),
				Filters.eq("lng", req.getLng())
		))) {
			req.getRequestDispatcher("/forums").forward(req, resp);
			return;
		}

		if (req.getParameter("edit") != null) {
			req.getRequestDispatcher("/edit").forward(req, resp);
		} else {
			req.getRequestDispatcher("/pages").forward(req, resp);
		}

	}

}