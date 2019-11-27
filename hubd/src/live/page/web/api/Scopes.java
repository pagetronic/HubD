package live.page.web.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Scopes class, can be manipulated from external configuration
 */
public class Scopes {

	public final static List<String> scopes = new ArrayList<>();

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
