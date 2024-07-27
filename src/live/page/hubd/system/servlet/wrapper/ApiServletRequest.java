/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.servlet.wrapper;

import jakarta.servlet.ServletRequest;
import live.page.hubd.system.Settings;

public class ApiServletRequest extends BaseServletRequest {
    public ApiServletRequest(ServletRequest request) {
        super(request);
    }

    public boolean isXml() {
        if (getAttribute("xml") != null) {
            return (boolean) getAttribute("xml");
        } else {
            return false;
        }
    }

    @Override

    public String getLng() {
        if (contains("lng") && Settings.getLangs().contains(getString("lng", null))) {
            return getString("lng", lng);
        }
        return super.getLng();

    }
}
