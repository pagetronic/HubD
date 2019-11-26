/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic.svg;

import live.page.web.system.Settings;
import live.page.web.utils.Fx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Parse and manipulate SVG for easy use
 */
public class SVGParser {

	private static final Map<String, String> sizes = new HashMap<>();
	private static final Map<String, String> svgs = new HashMap<>();

	static {
		make();
	}

	public static void make() {
		try {
			//MI Google
			sizes.clear();
			svgs.clear();
			StringWriter allmisvg = new StringWriter();
			for (File file : Fx.listFiles(Settings.HUB_REPO + "/res/svg/mi", "svg")) {
				String str = FileUtils.readFileToString(file);
				allmisvg.append(str);
			}
			allmisvg.close();
			Elements icons = Jsoup.parse(allmisvg.toString()).select("svg:not([xmlns])");
			icons.select("title").remove();
			icons.select("*").removeAttr("width").removeAttr("height").removeAttr("y").removeAttr("x").removeAttr("style").removeAttr("fill").removeAttr("fill-opacity").removeAttr("stroke");
			for (Element icon : icons) {
				String id = icon.attr("id");
				if (id != null) {
					Elements path = icon.select("path");
					if (path.size() == 1) {
						List<String> ids = Arrays.asList(id.split("_"));
						String new_id = "mi_" + StringUtils.join(ids.subList(1, ids.size() - 1).toArray(), "_");
						String icong = path.first().outerHtml();
						icong = icong.replace("\n", "").replace("> <", "><")
								.replace("></path>", " />").replace("\" />", "\"/>");
						icong = icong.replace("<path", "<path id=\"" + new_id + "\"");
						svgs.put(new_id, icong);
						sizes.put(new_id, icon.attr("viewBox"));
					}
				}
			}

			//FontAwesomeone

			StringWriter allfasvg = new StringWriter();
			for (File file : Fx.listFiles(Settings.HUB_REPO + "/res/svg/fa", "svg")) {
				String str = FileUtils.readFileToString(file);
				allfasvg.append(str);
			}
			allfasvg.close();
			Elements symbols = Jsoup.parse(allfasvg.toString()).select("symbol");
			symbols.select("title").remove();
			symbols.select("*").removeAttr("width").removeAttr("height").removeAttr("y").removeAttr("x").removeAttr("style").removeAttr("fill").removeAttr("fill-opacity").removeAttr("stroke");
			for (Element symbol : symbols) {
				String new_id = symbol.attr("id");
				if (new_id != null) {
					Elements path = symbol.select("path");
					if (path.size() == 1) {
						new_id = "fa_" + new_id.replace("fa-", "").replace("-", "_");
						String icong = path.first().outerHtml();
						icong = icong.replace("\n", "").replace("> <", "><")
								.replace("></path>", " />").replace("\" />", "\"/>");
						icong = icong.replace("<path", "<path id=\"" + new_id + "\"");
						svgs.put(new_id, icong);
						sizes.put(new_id, symbol.attr("viewBox"));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getSize(String key) {
		return sizes.get(key);
	}

	public static String get(String name) {
		return svgs.get(name);
	}

	public static Map<String, String> getSvgs() {
		return svgs;
	}

	public static Map<String, String> getSizes() {
		return sizes;
	}


}
