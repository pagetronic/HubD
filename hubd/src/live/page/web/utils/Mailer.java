/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.utils;

import live.page.web.system.Language;
import live.page.web.system.Settings;

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


		try {
			Properties props = new Properties();
			props.put("mail.smtp.starttls.enable", Settings.SMTP_MAIL_TLS);

			if (Settings.SMTP_MAIL_USER != null || Settings.SMTP_MAIL_PASSWD != null) {
				props.put("mail.smtp.auth", "true");
			}
			if (Settings.SMTP_MAIL_HOST != null) {
				props.put("mail.smtp.host", Settings.SMTP_MAIL_HOST);
			}
			if (Settings.SMTP_MAIL_PORT != null) {
				props.put("mail.smtp.port", Settings.SMTP_MAIL_PORT);
			}

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
