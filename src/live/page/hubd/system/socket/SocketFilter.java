/*
 * Copyright (c) 2019. PAGE and Sons
 */

package live.page.hubd.system.socket;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import live.page.hubd.system.servlet.utils.ServletUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Put Ip in parameterMap for sockets
 */
@WebFilter(asyncSupported = true, urlPatterns = {"/up", "/socket"})
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

}
