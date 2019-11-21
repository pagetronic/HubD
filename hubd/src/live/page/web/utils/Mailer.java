/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.utils.langs.Language;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Mailer {

	public static boolean send(String to, String subject, String message) {

		if (Fx.IS_DEBUG) {
			Fx.log(to);
			Fx.log(subject);
			Fx.log(message);
			return true;
		}
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		try {

			Message mail = new MimeMessage(Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(Settings.SMTP_MAIL_USER, Settings.SMTP_MAIL_PASSWD);
				}
			}));
			mail.setFrom(new InternetAddress(Settings.SMTP_MAIL_USER, Settings.SITE_TITLE));
			mail.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
			mail.setSubject(subject);
			mail.setContent(message, "text/html; charset=utf-8");
			Transport.send(mail);
			return true;

		} catch (Exception e) {
			Fx.log(e.getMessage());
			return false;
		}
	}

	public static boolean sendActivation(String lng, String to, String key) {
		String link = Settings.getFullHttp(lng) + "/profile?activate=" + key;
		link = "<a href=\"" + link + "\">" + link + "</a>";
		return send(to, Language.get("ACTIVATION_MAIL_TITLE", lng), Language.get("ACTIVATION_MAIL_TEXT", lng).replace("%1", link));

	}

}
