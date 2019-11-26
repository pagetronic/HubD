/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet.wrapper;

import live.page.web.system.sessions.Users;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public abstract class AddToServletRequest implements ServletContextListener {
	private static AddToServletRequest clss = null;

	protected static void seed(Users user, WebServletRequest req) {
		if (clss != null) {
			clss.seeder(user, req);
		}
	}

	protected void setClass(AddToServletRequest clss) {
		AddToServletRequest.clss = clss;
	}

	protected abstract void seeder(Users user, WebServletRequest req);

	protected abstract AddToServletRequest init();

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		clss = init();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}
}
