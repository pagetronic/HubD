/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic.tmpl;

import live.page.web.system.Settings;
import live.page.web.system.cosmetic.svg.SVGTemplate;
import live.page.web.system.cosmetic.tmpl.parsers.Cleaner;
import live.page.web.system.cosmetic.tmpl.parsers.PageParser;
import live.page.web.system.cosmetic.tmpl.parsers.PostParser;
import live.page.web.system.cosmetic.tmpl.plugs.*;
import live.page.web.utils.Fx;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class BaseTemplate {

	private static BaseTemplate template;
	private static final VelocityEngine engine = new VelocityEngine();
	private static VelocityContext context = null;

	public static BaseTemplate getTemplate() {
		return template;
	}

	public static void setTemplate(BaseTemplate template) {
		BaseTemplate.template = template;
	}

	private VelocityContext getContext() {
		if (context == null) {
			init();
		}

		VelocityContext ctx = new VelocityContext(context);
		ctx.put("svg", new SVGTemplate());
		return ctx;
	}

	public void process(String file, Map<String, ?> attributes, Writer writer) {

		VelocityContext context = getContext();
		for (Entry<String, ?> att : attributes.entrySet()) {
			context.put(att.getKey(), att.getValue());
		}
		engine.getTemplate(file).merge(context, writer);

	}

	public static String processToString(String file, Map<String, Object> attributes) {
		StringWriter writer = new StringWriter();

		VelocityContext context = template.getContext();
		for (Entry<String, Object> att : attributes.entrySet()) {
			context.put(att.getKey(), att.getValue());
		}
		engine.getTemplate(file).merge(context, writer);

		try {
			writer.close();
		} catch (Exception ignored) {
		}

		writer.flush();
		return writer.toString();
	}

	private void init() {

		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
		List<String> htmls = new ArrayList<>();
		htmls.add(Settings.REPO + "/html");
		if (!Settings.NOUI) {
			htmls.add(Settings.HUB_REPO + "/html");
		}
		engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, StringUtils.join(htmls, ", "));

		engine.setProperty(RuntimeConstants.INPUT_ENCODING, StandardCharsets.UTF_8.displayName());

		List<String> userdirective = new ArrayList<>();
		userdirective.add(LangTag.class.getCanonicalName());
		userdirective.add(FlushPen.class.getCanonicalName());
		userdirective.add(ForEach.class.getCanonicalName());
		userdirective.add(TagDate.class.getCanonicalName());
		userdirective.add(TagSince.class.getCanonicalName());
		userdirective.add(PostParser.class.getCanonicalName());
		userdirective.add(PageParser.class.getCanonicalName());
		userdirective.add(Cleaner.class.getCanonicalName());
		userdirective.add(NumberTag.class.getCanonicalName());
		userdirective.add(SizeTag.class.getCanonicalName());
		userdirective.add(PaginationTag.class.getCanonicalName());
		userdirective.add(ModuloTag.class.getCanonicalName());

		Class[] userdirectiveadd = getUserDirective();
		if (userdirectiveadd != null && userdirectiveadd.length>0) {
			for (Class uda : userdirectiveadd) {
				userdirective.add(uda.getCanonicalName());
			}
		}
		engine.setProperty("userdirective", String.join(",", userdirective));
		engine.setProperty(RuntimeConstants.SPACE_GOBBLING, "structured");
		engine.setProperty(RuntimeConstants.VM_LIBRARY_AUTORELOAD, "true");

		if (Fx.IS_DEBUG) {
			engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false");
			engine.setProperty(RuntimeConstants.VM_PERM_ALLOW_INLINE_REPLACE_GLOBAL, "true");
			engine.setProperty("file.resource.loader.modificationCheckInterval", "0");
		} else {
			engine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
			engine.setProperty("file.resource.loader.modificationCheckInterval", ((int) Settings.CTRL_PERIOD) + "");
			engine.setProperty(RuntimeConstants.PARSER_POOL_SIZE, "40");
		}

		engine.init();

		context = new VelocityContext();

		context.put("http_cdn", Settings.getCDNHttp());
		context.put("ui_logo", Settings.getCDNHttp() + Settings.UI_LOGO);
		context.put("http_host", Settings.getFullHttp());
		context.put("langs_domains", Settings.LANGS_DOMAINS.toList());
		context.put("site_title", Settings.SITE_TITLE);
		context.put("logo_title", Settings.LOGO_TITLE);
		context.put("analytics", Settings.ANALYTICS);
		context.put("logo", Settings.getLogo());
		context.put("debug", Fx.IS_DEBUG);
		context.put("Fxm", getUserFx());
		context.put("Fx", FxTemplate.class);
		context.put("theme_color", Settings.THEME_COLOR);

	}

	public abstract Class[] getUserDirective();

	public abstract Class getUserFx();
}

