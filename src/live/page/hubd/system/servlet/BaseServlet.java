package live.page.hubd.system.servlet;

import jakarta.servlet.*;
import live.page.hubd.system.Settings;

import java.io.IOException;

public abstract class BaseServlet implements Servlet {

    protected ServletConfig config;


    @Override
    public void service(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (request.isAsyncSupported() && !request.isAsyncStarted()) {
            AsyncContext context = request.startAsync();
            context.start(() -> {
                try {
                    asyncService(context.getRequest(), context.getResponse());
                } catch (IOException | ServletException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        context.complete();
                    } catch (Exception ignore) {
                    }
                }
            });
        } else {
            asyncService(request, response);
        }
    }

    public abstract void asyncService(ServletRequest request, ServletResponse response) throws IOException, ServletException;

    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    @Override
    public String getServletInfo() {
        return Settings.SITE_TITLE;
    }
}
