/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.admin;

import live.page.web.content.posts.utils.ThreadsAggregator;
import live.page.web.system.servlet.HttpServlet;
import live.page.web.system.servlet.wrapper.WebServletRequest;
import live.page.web.system.servlet.wrapper.WebServletResponse;
import live.page.web.system.sessions.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet(urlPatterns = {"/admin/posts"})
public class PostsAdmin extends HttpServlet {


	@Override
	public void doGetEditor(WebServletRequest req, WebServletResponse resp, Users user) throws IOException, ServletException {

		req.setAttribute("active", "admin");

		req.setTitle("Post Admin");
		req.addBreadCrumb("Admin", "/admin");
		req.setBreadCrumbTitle("Post Admin");
		if (req.contains("paging")) {
			req.addBreadCrumb("Post Admin", "/admin/posts");
		}


		req.setAttribute("posts", ThreadsAggregator.getPostsAdmin(req.getString("paging", "last"), req.getString("sort", "date"), 5));

		resp.sendTemplate(req, "/admin/posts.html");

	}

}
