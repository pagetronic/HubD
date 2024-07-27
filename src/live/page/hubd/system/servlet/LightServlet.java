/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import live.page.hubd.system.Settings;
import live.page.hubd.system.servlet.wrapper.BaseServletRequest;
import live.page.hubd.system.servlet.wrapper.BaseServletResponse;

import java.io.IOException;

/**
 * Light weight servlet used for simplified services
 */
public abstract class LightServlet extends BaseServlet {

    protected ServletConfig config = null;

    /**
     * Test the service needed, API or Standard web
     */
    @Override
    public void asyncService(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        doService(new BaseServletRequest(request), new BaseServletResponse(response));
    }

    abstract public void doService(BaseServletRequest req, BaseServletResponse resp) throws IOException, ServletException;


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

