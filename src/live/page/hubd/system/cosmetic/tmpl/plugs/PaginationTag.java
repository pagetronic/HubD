/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.system.cosmetic.tmpl.plugs;

import live.page.hubd.system.db.utils.paginer.Paginer;
import live.page.hubd.system.json.Json;
import live.page.hubd.system.utils.Fx;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;

public class PaginationTag extends Directive {

    @Override
    public String getName() {
        return "pagination";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, MethodInvocationException, ResourceNotFoundException, ParseErrorException {

        try {

            Json paging = (Json) node.jjtGetChild(0).value(context);
            if (paging == null) {
                return false;
            }

            boolean isfirst = (node.jjtGetNumChildren() > 2) ? (Boolean) node.jjtGetChild(2).value(context) : true;
            String anchor = (node.jjtGetNumChildren() > 3) ? "#" + node.jjtGetChild(3).value(context) : "";

            String mediator = (node.jjtGetNumChildren() > 4) ? node.jjtGetChild(4).value(context).toString() : "";
            if (mediator == null || mediator.isEmpty()) {
                mediator = (String) context.get("mediator");
                if (mediator != null) {
                    mediator = mediator.replace("'", "\"");
                }
            }
            String pager = (String) context.get("pager");
            if (pager == null) {
                pager = "paging";
            }

            StringBuilder url = null;

            if (node.jjtGetNumChildren() > 1) {
                url = new StringBuilder((String) node.jjtGetChild(1).value(context));
            } else if (context.containsKey("base_canonical")) {
                URL uri = new URL(context.get("canonical").toString());
                url = new StringBuilder(uri.getPath());
                if (uri.getQuery() != null) {
                    String and = "?";
                    for (String query : uri.getQuery().split("&")) {
                        if (!query.startsWith(pager + "=")) {
                            url.append(and).append(query);
                            and = "&amp;";
                        }
                    }
                }
            } else {
                url = new StringBuilder((String) context.get("requesturi"));
            }

            writer.write(Paginer.getHtml(url.toString(), pager, paging, isfirst, anchor, mediator, context.get("lng").toString()));

        } catch (Exception e) {
            if (Fx.IS_DEBUG) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

}