/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.servlet;

import live.page.web.servlet.wrapper.BaseServletRequest;
import live.page.web.servlet.wrapper.BaseServletResponse;
import live.page.web.utils.Settings;

import javax.servlet.*;
import java.io.IOException;

/**
 * Light weight servlet
 */
public abstract class BaseServlet implements Servlet {

	abstract public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException;

	@Override
	public void service(ServletRequest req_, ServletResponse resp_) throws IOException {


		BaseServletRequest req = new BaseServletRequest(req_);
		BaseServletResponse resp = new BaseServletResponse(resp_);

		try {
			doService(req, resp);
		} catch (Exception e) {
			resp.setStatus(500);
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

