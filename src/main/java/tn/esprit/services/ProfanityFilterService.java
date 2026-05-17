package tn.esprit.services;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityFilterService {

    // Liste simple de gros mots français à compléter/adapter selon le besoin
    private static final List<String> BAD_WORDS = Arrays.asList(
            "con",
            "connard",
            "connasse",
            "merde",
            "putain",
            "pute",
            "salope",
            "salaud",
            "enculé",
            "encule",
            "fdp",
            "fuck"
    );

    private ProfanityFilterService() {
    }

    public static String filter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (String bad : BAD_WORDS) {
            if (bad == null || bad.isEmpty()) {
                continue;
            }
            String stars = repeat('*', bad.length());
            String pattern = "(?i)\\b" + Pattern.quote(bad) + "\\b";
            result = result.replaceAll(pattern, stars);
        }
        return result;
    }

    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}

