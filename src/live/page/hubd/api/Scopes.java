package live.page.hubd.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scopes class, can be manipulated from external configuration
 */
public class Scopes {

    public final static List<String> scopes = new ArrayList<>();

    static {
        scopes.addAll(Arrays.asList(
                "email",
                "pm",
                "threads",
                "accounts"
        ));
    }

    public static List<String> sort(List<String> scopes) {
        List<String> sorted = new ArrayList<>();
        scopes.forEach(scope -> {
            if (scopes.contains(scope)) {
                sorted.add(scope);
            }
        });
        return sorted;
    }

}
