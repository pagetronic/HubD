/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package com.example.web.template;

import live.page.web.template.BaseTemplate;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class MatchesTemplate extends BaseTemplate implements ServletContextListener {

	@Override
	public Class[] getUserDirective() {
		return new Class[]{};
	}

	@Override
	public Class getUserFx() {
		return FxTemplate.class;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		setTemplate(new MatchesTemplate());

	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

	}
}
