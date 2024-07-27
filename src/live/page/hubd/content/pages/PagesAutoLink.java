package live.page.hubd.content.pages;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import live.page.hubd.system.db.Db;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.sessions.Users;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PagesAutoLink {

    /**
     * AutoLink all Pages
     *
     * @param id       of the Page
     * @param keywords to set to the Page
     * @param user     do the update
     * @return Pages liked
     */
    public static List<String> keywords(String id, List<String> keywords, Users user) {

        List<String> linkeds = new ArrayList<>();
        if (keywords == null || keywords.isEmpty()) {
            return linkeds;
        }
        MongoCursor<Json> pages = Db.find("Pages",
                Filters.and(Filters.ne("_id", id),
                        Filters.regex("text", Pattern.compile("(" + id + "|" + StringUtils.join(keywords, "|") + ")", Pattern.CASE_INSENSITIVE))
                )
        ).iterator();

        while (pages.hasNext()) {
            Json page = pages.next();

            String original = page.getText("text", "");
            String text = page.getText("text", "");

            text = cleanLink(text, id);


            List<String> groups = new ArrayList<>();
            for (String pat : new String[]{"\\[([^]]+)]", "<a[^>]+>([^<]+)</a>", "=([^\n]+)=([ ]+)?\n"}) {
                Pattern pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    text = text.replace(matcher.group(), "@@@###" + groups.size() + "###@@@");
                    groups.add(matcher.group());
                }
            }

            for (String keyword : keywords) {
                if (keyword.isEmpty()) {
                    continue;
                }
                Pattern pattern = Pattern.compile("([\\r\\n\\t ,’'ʼ(]|^)(" + keyword + ")([.,!?;) ])", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String start = matcher.group(1);
                    String title = matcher.group(2);
                    String punct = matcher.group(3);

                    text = matcher.replaceFirst(start + "[Pages(" + id + ") " + title + "]" + punct);

                    break;
                }
            }

            for (int i = groups.size() - 1; i >= 0; i--) {
                text = text.replace("@@@###" + i + "###@@@", groups.get(i));
            }

            if (!original.equals(text)) {
                Json revision = new Json().put("origine", page.getId()).put("text", text).put("edit", new Date());
                if (user != null) {
                    revision.put("editor", user.getId());
                }
                Db.save("Revisions", revision);
                Db.updateOne("Pages", Filters.eq("_id", page.getId()), new Json().put("$set", new Json().put("text", text).put("update", new Date())));
                linkeds.add("/" + page.getString("url"));
            }
        }

        pages.close();


        return linkeds;
    }

    /**
     * Clean all links
     *
     * @param text to clean
     * @param id   of the link to clean
     * @return text cleaned
     */
    private static String cleanLink(String text, String id) {
        for (String pat : new String[]{"\\[Pages\\(" + id + "\\) ?([^]]+)]", "<a original=\"" + id + "\"[^>]+>([^<]+)</a>"}) {
            Pattern pattern = Pattern.compile(pat, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                text = matcher.replaceAll("$1");
            }
        }
        return text;
    }


}
