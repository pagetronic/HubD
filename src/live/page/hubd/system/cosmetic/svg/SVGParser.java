package live.page.hubd.system.cosmetic.svg;

import live.page.hubd.system.Settings;
import live.page.hubd.system.utils.FilesRepos;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Parse and manipulate SVG for easy use
 */
public class SVGParser {


    /**
     * Make.
     */
    public static Map<String, SVGData> getAllSvgs() {

        Map<String, SVGData> data = new HashMap<>();

        try {

            // More
            for (String dir : List.of("more")) {
                for (File file : FilesRepos.listFiles("res/svg/" + dir, "svg")) {
                    String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    str = str.replaceAll("</?g>", "");
                    String id = file.getName().replace(".svg", "");
                    Elements icon = Jsoup.parse(str).select("svg");
                    if (icon.attr("viewBox").isEmpty()) {
                        continue;
                    }
                    String size = icon.attr("viewBox");
                    id = dir + "_" + id;
                    if (icon.select(" > *").size() == 1) {
                        icon.select("> *").first().attr("id", id);

                        data.put(id, new SVGData(size, icon.html()));
                    } else {
                        Element g = new Element("g");
                        g.attr("id", id);
                        g.html(icon.html());
                        data.put(id, new SVGData(size, g.outerHtml()));
                    }
                }
            }


            //MI Google
            for (File file : FilesRepos.listFiles("res/svg/mi", "svg")) {
                String[] dirs = file.getCanonicalPath().replace(Settings.REPO + "/res/svg/mi/", "")
                        .split("/", 3);
                String id = "mi_" + dirs[1];
                String str = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                str = str.replaceAll("</?g>", "");
                Elements icon = Jsoup.parse(str).select("svg");
                if (icon.attr("viewBox").isEmpty()) {
                    continue;
                }

                String size = icon.attr("viewBox");
                if (icon.select("> *").size() == 1) {
                    icon.select("> *").first().attr("id", id);
                    data.put(id, new SVGData(size, icon.html()));
                } else {
                    Element g = new Element("g");
                    g.attr("id", id);
                    g.html(icon.html());
                    data.put(id, new SVGData(size, g.outerHtml()));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }


    public static class SVGData {
        final String size;
        final String src;

        SVGData(String size, String src) {
            this.size = size;
            this.src = src;
        }
    }
}
