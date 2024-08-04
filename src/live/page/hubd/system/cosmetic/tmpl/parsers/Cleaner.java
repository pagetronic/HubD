/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.parsers;

import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.Writer;

public class Cleaner extends Directive {

    @Override
    public String getName() {
        return "clean";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) {
        try {
            String text = (String) node.jjtGetChild(0).value(context);
            if (text == null) {
                return true;
            }

            text = text
                    .replaceAll("\\[[A-Za-z]+\\(([^)]+)\\)([^]]+)]", "$2")
                    .replaceAll("<([^>]+)>", "");

            if (node.jjtGetNumChildren() > 1) {
                text = Fx.truncate(text, (int) node.jjtGetChild(1).value(context));
            }
            writer.write(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
