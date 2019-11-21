/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils.impl;

import live.page.web.servlet.wrapper.WebServletRequest;
import live.page.web.servlet.wrapper.WebServletResponse;
import live.page.web.utils.json.Json;

import java.io.IOException;

public interface GetPageImpl {
	void doGetPage(WebServletRequest req, WebServletResponse resp, Json page) throws IOException;

}
