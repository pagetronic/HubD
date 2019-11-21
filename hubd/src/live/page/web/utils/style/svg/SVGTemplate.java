/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils.style.svg;

import live.page.web.utils.style.SVGServlet;

public class SVGTemplate {

	public static String get(String name_id) {

		return "<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
				"viewBox=\"" + SVGParser.getSize(name_id) + "\">" +
				"<use xlink:href=\"" + SVGServlet.getName() + "#" + name_id + "\"></use>" +
				"</svg>";
	}
}
