/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.servlet;

import live.page.web.servlet.utils.GlobalControl;
import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.utils.Settings;
import live.page.web.utils.json.Json;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

@WebServlet(urlPatterns = {"/HOST_HTTP"}, displayName = "baseHost")
public class HostServlet extends GlobalControl {

	@Override
	public void doGetPublic(WebServletRequest req, WebServletResponse resp) throws IOException, ServletException {

		if (req.getRequestURI().startsWith("/google") && req.getRequestURI().endsWith(".html")) {
			URL res = req.getServletContext().getResource(req.getRequestURI());
			if (res != null) {
				URLConnection cx = res.openConnection();
				InputStream input = cx.getInputStream();
				if (input != null) {
					OutputStream output = resp.getOutputStream();
					byte[] buffer = new byte[4000];
					for (int len; (len = input.read(buffer)) > 0; ) {
						output.write(buffer, 0, len);
					}
					input.close();
					output.close();
					return;
				}
			}
		}

		if (req.getRequestURI().equals("/robots.txt")) {
			resp.sendText(
					"User-agent: *\n" +
							"Allow: /\n" +
							"\n" +
							"User-agent: ia_archiver\n" +
							"Disallow: /\n"
			);
			return;
		}

		if (!req.getRequestURI().equals("/")) {
			resp.sendRedirect("/", 301);
			return;
		}

		req.setAttribute("lng", null);
		req.setUser(null);
		req.setAttribute("exchange", new Json());
		req.setRobotsIndex(false, true);
		req.setTitle(Settings.SITE_TITLE);
		resp.sendTemplate(req, "/base.html");

	}

}
