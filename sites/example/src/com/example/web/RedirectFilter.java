/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.example.web;

import live.page.web.servlet.utils.ServletUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter(asyncSupported = true, urlPatterns = {"/", "/*"})
public class RedirectFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {


		String host = req.getServerName();
		String requestURI = ((HttpServletRequest) req).getRequestURI();
		if (host == null) {
			host = "127.0.0.1";
		}

		if (host.equals("en.renseigner.com")) {
			ServletUtils.redirect301("https://uk.nextmatches.com" + requestURI, resp);
			return;
		}

		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(FilterConfig filterConfig) {

	}

}
