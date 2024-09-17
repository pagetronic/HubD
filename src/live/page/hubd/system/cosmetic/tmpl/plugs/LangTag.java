/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.Language;
import live.page.hubd.system.Settings;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;

public class LangTag extends Directive {

    @Override
    public String getName() {
        return "lang";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, MethodInvocationException, ResourceNotFoundException, ParseErrorException {

        try {
            String key = String.valueOf(node.jjtGetChild(0).value(context));
            Object lng = context.get("lng");
            if (lng == null) {
                lng = "fr";
            }
            if (node.jjtGetNumChildren() > 1 && Settings.getLangs().contains(node.jjtGetChild(1).value(context).toString())) {
                lng = node.jjtGetChild(1).value(context).toString();
            }

            String str = Language.get(key, lng.toString());

            int item = 1;
            if (node.jjtGetNumChildren() > 1) {
                Object value = node.jjtGetChild(1).value(context);
                if (value.getClass().isAssignableFrom(String.class)) {
                    if (value.toString().equalsIgnoreCase("ucfirst")) {
                        str = Fx.ucfirst(str);
                        item = 2;
                    } else if (value.toString().equalsIgnoreCase("lowercase")) {
                        str = str.toLowerCase();
                        item = 2;
                    } else if (value.toString().equalsIgnoreCase("uppercase")) {
                        str = str.toUpperCase();
                        item = 2;
                    }
                }
            }
            for (int i = item; i < node.jjtGetNumChildren(); i++) {
                Object value = node.jjtGetChild(i).value(context);
                if (value == null) {
                    str = "$NULL@" + key;
                } else {
                    str = str.replace("%" + ((i - item) + 1), String.valueOf(value));
                }
            }
            writer.write(str);
        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

}
