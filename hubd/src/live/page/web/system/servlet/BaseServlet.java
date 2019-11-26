/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.servlet;

import live.page.web.system.Settings;
import live.page.web.system.servlet.wrapper.BaseServletRequest;
import live.page.web.system.servlet.wrapper.BaseServletResponse;
import live.page.web.utils.Fx;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Light weight servlet used for simplified services
 */
public abstract class BaseServlet implements Servlet {

	abstract public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException;

	@Override
	public void service(ServletRequest req, ServletResponse resp) {

		try {
			doService(new BaseServletRequest(req), new BaseServletResponse(resp));
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
			((HttpServletResponse) resp).setStatus(500);
		}
	}

	protected ServletConfig config = null;

	@Override
	public void init(ServletConfig config) {
		this.config = config;
	}

	@Override
	public void destroy() {
	}

	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	@Override
	public String getServletInfo() {
		return Settings.PROJECT_NAME;
	}


}

