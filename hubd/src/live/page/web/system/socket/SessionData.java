/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.socket;

import javax.websocket.Session;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SessionData {

	private final Session session;
	private String user_id = null;
	private String ip = null;
	private String lng = null;
	private final Date date = new Date();

	private final List<String> elements = new ArrayList<>();

	private StringWriter temp_message = new StringWriter();

	public SessionData(Session session) {
		this.session = session;

	}

	public Date getDate() {
		return date;
	}

	public Session getSession() {
		return session;
	}

	public String getUserId() {
		return user_id;
	}

	public String getIp() {
		return ip;
	}

	public void setUserId(String user_id) {
		this.user_id = user_id;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public List<String> getElements() {
		return elements;
	}

	public void addElement(String element) {
		if (!elements.contains(element)) {
			elements.add(element);
		}
	}

	public void removeElement(String element) {
		elements.remove(element);
	}

	public void addMsg(String message) {
		temp_message.append(message);
	}

	public void send(SocketMessage message) {
		if (session.isOpen()) {
			session.getAsyncRemote().sendText(message.toString());
		}
	}

	public String getMsg() {
		try {
			return temp_message.toString();
		} finally {
			clearMsg();
		}
	}

	public void clearMsg() {
		try {
			temp_message.close();
		} catch (Exception ignore) {
		}
		temp_message = new StringWriter();
	}

	public void setLang(String lng) {
		this.lng = lng;
	}

	public String getLang() {
		return lng;
	}

	public boolean isOpen() {
		return session.isOpen();
	}

	private final List<String> aborts = new ArrayList<>();

	public void abort(String act) {
		aborts.add(act);
	}

	public boolean isAbort(String act) {
		try {
			return aborts.contains(act);
		} finally {
			aborts.remove(act);
		}
	}

}
