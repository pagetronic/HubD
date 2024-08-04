/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;

public class JsonTag extends Directive {

    @Override
    public String getName() {
        return "jsonTag";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            if (node.jjtGetNumChildren() > 0) {
                String key = node.jjtGetChild(0).literal();
                Json data = (Json) node.jjtGetChild(0).value(context);
                if (key == null) {
                    return true;
                }
                writer.write("<script key=\"" + key.substring(1) + "\" type=\"application/json\">\n" +
                        data.toString(!Fx.IS_DEBUG) + "\n</script>");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
