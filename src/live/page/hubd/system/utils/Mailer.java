/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;

import java.util.Properties;

public class Mailer {

    /**
     * Send an activation code
     *
     * @param lng  where requested
     * @param to   user
     * @param code to send
     * @return true if send successful
     */
    public static boolean sendActivation(String lng, String to, String code) {
        String link = Settings.getFullHttp(lng) + "/profile?activate=" + code;
        if (Fx.IS_DEBUG) {
            Fx.log(link);
        }
        link = "<a href=\"" + link + "\">" + link + "</a>";

        return send(to, Language.get("ACTIVATION_EMAIL_TITLE", lng), Language.get("ACTIVATION_EMAIL_TEXT", lng).replace("%1", link));

    }

    /**
     * Send an email
     *
     * @param recipient of the message
     * @param subject   of the message
     * @param message   to send
     * @return true if send successful
     */
    public static boolean send(String recipient, String subject, String message) {

        if (Fx.IS_DEBUG) {
            Fx.log(recipient);
            Fx.log(subject);
            Fx.log(message);
        }


        try {
            Properties props = new Properties();

            if (Settings.SMTP_MAIL_USER != null || Settings.SMTP_MAIL_PASSWD != null) {
                props.put("mail.smtp.auth", true);
            }
            if (Settings.SMTP_MAIL_HOST != null) {
                props.put("mail.smtp.host", Settings.SMTP_MAIL_HOST);
                props.put("mail.smtp.ssl.trust", "*");
            }

            props.put("mail.smtp.port", String.valueOf(Settings.SMTP_MAIL_PORT));

            props.put("mail.smtp.starttls.enable", String.valueOf(Settings.SMTP_MAIL_TLS));

            Message mail = new MimeMessage(Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(Settings.SMTP_MAIL_USER, Settings.SMTP_MAIL_PASSWD);
                }
            }));
            mail.setFrom(new InternetAddress(Settings.SMTP_MAIL_USER, Settings.SITE_TITLE));
            mail.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            mail.setSubject(subject);
            mail.setContent(message, "text/html; charset=utf-8");
            Transport.send(mail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
