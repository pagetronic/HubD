package live.page.hubd.system.utils.ai;


import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.hubd.content.threads.ThreadsAggregator;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import live.page.hubd.system.socket.SocketMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IAUtils {

    public static SocketMessage rewriteQuestion(Json data) {
        int count = 5;
        String context = data.getText("context", "");
        String text = data.getText("text", "");
        List<Json> questions = new ArrayList<>();
        List<Json> previous = data.getListJson("questions");
        if (previous == null) {
            previous = new ArrayList<>();
        }
        if (previous.size() < 15) {
            List<ChatGPT.Message> messages = new ArrayList<>();
            messages.add(new ChatGPT.Message(ChatGPT.Role.system, "Tu es un robot qui génère des listes numérotées classées par ordre d'importance.\n" +
                    "Génère une liste de suggestion de " + count + " questions pour un site de questions/réponses " +
                    " en fonction du contexte \"" + context + "\" et de l'entrée de l'utilisateur." +
                    "Pour chaque réponse que tu donnes, indique la question en 50 caractères maximum sur une ligne et précise la question en beaucoup plus long sur une autre ligne." +
                    "Utilise un langage populaire, clair et simple."));
            messages.add(new ChatGPT.Message(ChatGPT.Role.user, text));
            if (!previous.isEmpty()) {
                for (int j = 0; j < previous.size(); j = j + count) {
                    if (j > 0) {
                        messages.add(new ChatGPT.Message(ChatGPT.Role.user, "Donne moi en " + count + " de plus"));
                    }
                    StringBuilder assistant = new StringBuilder();
                    for (int i = 0; i < previous.size(); i++) {
                        Json question = previous.get(i);
                        assistant.append(i + j + 1).append(") ")
                                .append(question.getString("title", "")).append("\n")
                                .append(question.getString("text", "")).append("\n\n");
                    }
                    messages.add(new ChatGPT.Message(ChatGPT.Role.assistant, assistant.toString()));
                }
                messages.add(new ChatGPT.Message(ChatGPT.Role.user, "Donne moi en " + count + " de plus"));
            }
            String reply = ChatGPT.chatGPT(ChatGPT.Model.gpt3, messages);

            Pattern pattern = Pattern.compile("([0-9]+) ?([.)]) ?(.*)\\n(.*)");
            Matcher match = pattern.matcher(reply);
            while (match.find()) {
                questions.add(new Json("title", match.group(3).replaceAll("^([ \\-]+)", "").trim())
                        .put("text", match.group(4).replaceAll("^([ \\-]+)", "").trim()));
            }
        }
        return new SocketMessage(data.getString("act")).put("questions", questions);
    }

    public static SocketMessage replyQuestion(Json data, Users user) {
        String thread_id = data.getString("thread");
        List<ChatGPT.Message> messages = new ArrayList<>();
        messages.add(new ChatGPT.Message(ChatGPT.Role.system,
                "Tu es une personne qui répond à des questions d'utilisateurs.\n" +
                        "Ne fais pas la description de l'objet de la question, réponds directement à la question sans introduction ni conclusion.\n" +
                        "Ta réponse ne doit pas contenir de listes. Découpe ton texte en paragraphes.\n" +
                        "Tu ne renvois pas vers un professionnel ou un spécialiste.\n" +
                        "N'utilise PAS de mots de liaisons (linking words), de connecteurs ou de liens logiques."));

        List<Json> otherThreads = Db.aggregate("Posts", List.of(
                Aggregates.match(
                        Filters.and(
                                Filters.ne("parents.id", thread_id),
                                Filters.eq("user", user.getId()),
                                Filters.eq("parents.type", "Posts")
                        )
                ),
                Aggregates.sort(Sorts.descending("date")),
                Aggregates.limit(5),
                Aggregates.addFields(new Field<>("thread", new Json("$arrayElemAt", Arrays.asList("$parents.id", 0)))),
                Aggregates.lookup("Posts", "thread", "_id", "thread"),
                Aggregates.unwind("$thread"),
                Aggregates.project(new Json("reply", "$text").put("question", "$thread.title"))
        )).into(new ArrayList<>());

        Json thread = ThreadsAggregator.getThread(thread_id, null, null);


        for (Json otherThread : otherThreads) {
            if (!otherThread.getText("reply", "").isEmpty()) {
                messages.add(new ChatGPT.Message(ChatGPT.Role.user, "Question: " + otherThread.getText("question")));
                messages.add(new ChatGPT.Message(ChatGPT.Role.assistant, otherThread.getText("reply")));
            }
        }
        String question = thread.getString("title", thread.getString("text"));

        messages.add(new ChatGPT.Message(ChatGPT.Role.user, "Question: " + question));

        List<Json> posts = thread.getJson("posts").getListJson("result");
        for (Json post : posts) {
            Json userPost = post.getJson("user");
            if (userPost != null && userPost.getId().equals(user.getId())) {
                messages.add(new ChatGPT.Message(ChatGPT.Role.assistant, post.getText("text")));
            } else {
                messages.add(new ChatGPT.Message(ChatGPT.Role.user,
                        (userPost == null ? "Un utilisateur anonyme" : userPost.getString("name"))
                                + " a répondu : " + post.getText("text")));
            }
        }
        if (messages.get(messages.size() - 1).getRole().equals(ChatGPT.Role.assistant)) {
            messages.add(new ChatGPT.Message(ChatGPT.Role.user, "Dis moi en plus"));
        } else if (!posts.isEmpty()) {
            messages.add(new ChatGPT.Message(ChatGPT.Role.user, posts.size() > 1 ? "Complète les réponses à la dernière question" : "Complète la réponse à la dernière question"));
        }


        String reply = ChatGPT.chatGPT(ChatGPT.Model.gpt4, messages);

        return new SocketMessage(data.getString("act")).put("text", reply);
    }


    public static SocketMessage rewriteText(Json data) {
        String text = data.getText("text", "");
        int min = data.getInteger("min", 100);
        int max = data.getInteger("max", 300);
        List<ChatGPT.Message> messages = new ArrayList<>();
        messages.add(new ChatGPT.Message(ChatGPT.Role.system, "Tu es un robot qui produit des textes d'information.\n" +
                "Ne fais pas de résumé, écris un nouveau texte avec uniquement les informations fournies.\n" +
                "Ne renvois pas vers un professionnel ou un spécialiste.\n" +
                "Réécris complétement le texte fourni par l'utilisateur en préservant les informations essentielles et fournis une réponse de " + min + " à " + max + " mots. "));
        messages.add(new ChatGPT.Message(ChatGPT.Role.user, text));
        String reply = ChatGPT.chatGPT(ChatGPT.Model.gpt4, messages);
        return new SocketMessage(data.getString("act")).put("text", reply);
    }

    public static SocketMessage socket(Json data, Users user) {
        if (data.getString("type").equals("question")) {
            return rewriteQuestion(data);
        }
        if (data.getString("type").equals("reply")) {
            return replyQuestion(data, user);
        }
        if (data.getString("type").equals("rewrite")) {
            return rewriteText(data);

        }
        return null;

    }
}
