package live.page.hubd.system.utils.ai;


import live.page.hubd.system.Settings;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

public class ChatGPT {
    private final static String url = "https://api.openai.com/v1/chat/completions";
    private final static String apiKey = Settings.OPEN_AI_KEY;

    public static String chatGPT(Model model, List<Message> messages) {
        return chatGPT(model, messages, 0, 0, 0);
    }

    public static String chatGPT(Model model, List<Message> messages, double temperature, double frequency_penalty, double presence_penalty) {

        Json body = new Json("model", model.key).put("messages", messages)
                .put("frequency_penalty", frequency_penalty).put("presence_penalty", presence_penalty).put("temperature", temperature);
        String id = Fx.md5(body.toString(true));
        Json cache = Db.findById("IACache", id);
        if (cache != null) {
            return cache.getText("content");
        }

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body.toString(true));
            writer.flush();
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();
            String content = new Json(response.toString()).getListJson("choices").get(0).getJson("message").getText("content");
            if (content != null) {
                body.put("_id", id);
                body.put("content", content);
                body.put("date", new Date());
                Db.save("IACache", body);
            }
            return content;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum Role {
        system, user, assistant
    }

    public enum Model {
        gpt4("gpt-4"), gpt3("gpt-3.5-turbo");
        final String key;

        Model(String key) {
            this.key = key;
        }
    }

    public static class Message extends Json {
        public Message(Role role, String content) {
            put("role", role.name());
            put("content", content);
        }

        Role getRole() {
            return Role.valueOf(getString("role"));
        }

        String getContent() {
            return getText("content");
        }
    }
}
