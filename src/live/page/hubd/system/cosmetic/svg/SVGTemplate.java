package live.page.hubd.system.cosmetic.svg;

/**
 * Function used in templates and class for get simplified SVG, external SVG (use xlink)
 */
public class SVGTemplate {


    public static SVGTemplate init() {
        return new SVGTemplate();
    }

    /**
     * Get string.
     *
     * @param name_id the name id
     * @return the string
     */
    public String get(String name_id) {

        if (!SVGServlet.svgs.containsKey(name_id) || SVGServlet.svgs.get(name_id) == null) {
            return "$svg." + name_id;
        }
        return "<svg version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "viewBox=\"" + SVGServlet.svgs.get(name_id).size + "\">" +
                "<use xlink:href=\"" + SVGServlet.getName() + "#" + name_id + "\"></use>" +
                "</svg>";
    }
}
