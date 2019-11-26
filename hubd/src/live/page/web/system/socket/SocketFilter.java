/*
 * Copyright (c) 2019. PAGE and Sons
 */

package live.page.web.system.socket;

import live.page.web.system.servlet.utils.ServletUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Put Ip in parameterMap for sockets
 */
@WebFilter(urlPatterns = {"/up", "/socket"})
public class SocketFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		Map<String, String[]> params = new HashMap<>(request.getParameterMap());
		params.put("ip", new String[]{ServletUtils.realIp(request)});
		chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
			@Override
			public Map<String, String[]> getParameterMap() {
				return params;
			}
		}, response);
	}

	@Override
	public void destroy() {

	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}
}
