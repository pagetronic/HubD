/*
 * Copyright (c) 2019. PAGE and Sons
 */
package live.page.web.blobs;

import live.page.web.system.Settings;
import live.page.web.system.json.Json;
import live.page.web.system.socket.SocketConfig;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.*;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Upload Websocket
 */
@ServerEndpoint(value = "/up", configurator = SocketConfig.class)
public class UploadSocket implements ServletContextListener {
	/**
	 * Initial informations about file
	 */
	@OnMessage
	public void onMessage(String msg, boolean isLast, Session session) throws IOException {
		Json data = new Json(msg);
		if (isLast && data.containsKey("name") && data.containsKey("type") && data.containsKey("size") && Settings.FILES_TYPE.contains(data.getString("type"))) {
			File file = File.createTempFile("upload", ".file");
			Map<String, Object> prop = session.getUserProperties();
			prop.put("file", file);
			prop.put("out", new FileOutputStream(file));
			prop.put("name", data.getString("name"));
			prop.put("type", data.getString("type"));
			prop.put("size", data.getInteger("size"));
		} else {
			session.close();
		}
	}

	/**
	 * Receive file after initial informations
	 */
	@OnMessage
	public void onMessage(byte[] bytes, boolean isLast, Session session) throws IOException {
		Map<String, Object> prop = session.getUserProperties();
		File file = (File) prop.get("file");
		FileOutputStream out = (FileOutputStream) prop.get("out");
		if (file == null || out == null) {
			session.close();
		}
		int size = (Integer) prop.get("size");
		out.write(bytes);
		out.flush();
		if (!isLast) {
			session.getBasicRemote().sendText(new Json("percent", file.length() * 100 / size).toString());
			return;
		}

		String name = (String) prop.get("name");
		String type = (String) prop.get("type");
		String user_id = (String) prop.get("user_id");
		String ip = (String) prop.get("ip");

		String blobid = BlobsUtils.putFile(file, user_id, ip, name, type);
		session.getBasicRemote().sendText(new Json("id", blobid).put("name", name).put("type", type).put("src", Settings.getCDNHttp() + "/files/" + blobid).toString());
		session.close();
	}

	@OnClose
	public void onClose(Session session) throws IOException {
		Map<String, Object> prop = session.getUserProperties();
		File file = (File) prop.get("file");
		if (file != null) {
			file.delete();
		}
		FileOutputStream out = (FileOutputStream) prop.get("out");
		if (out != null) {
			out.close();
		}
	}

	/**
	 * Control user authorization and secure connection
	 */
	@OnOpen
	public void onOpen(Session session) throws IOException {
		try {
			Map<String, Object> prop = session.getUserProperties();

			if (prop.get("user_id") == null) {
				session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "PLEASE_LOGIN"));
				return;
			}

			String origine = (String) prop.get("origin");
			if (!session.isSecure() || (origine != null && !origine.endsWith(Settings.STANDARD_HOST))) {
				session.close();
			}

		} catch (Exception e) {
			session.close();
		}
	}

	/**
	 * Close on error for restart procedure
	 */
	@OnError
	public void onError(Session session, Throwable t) throws IOException {
		if (session.isOpen()) {
			session.close();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

}
