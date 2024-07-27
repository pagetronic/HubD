package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.json.Json;
import live.page.hubd.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;

/**
 * The type Tag since.
 */
public class TMPLPre extends Directive {

    @Override
    public String getName() {
        return "pre";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        if (!Fx.IS_DEBUG) {
            return false;
        }
        try {
            Json data = (Json) node.jjtGetChild(0).value(context);
            writer.write("<details class=\"card\">");
            writer.write("<summary>" + node.jjtGetChild(0).literal() + "</summary>");
            writer.write("<pre>");
            writer.write(data.toString(false));
            writer.write("</pre>");
            writer.write("</details>");
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
