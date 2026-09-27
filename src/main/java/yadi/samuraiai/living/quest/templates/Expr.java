package yadi.samuraiai.living.quest.templates;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The tiny expression language of templates: text with {@code {variable}} placeholders, and quantities that are a number, a
 * variable, or a variable times/plus a number ({@code {quantity}*1.5}, {@code {severity}*40+10}). Unknown variables stay
 * visible in text (so a missing variable is noticed) and count as 0 in numbers.
 */
public final class Expr {
    private static final Pattern VAR = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)}");

    private Expr() { }

    public static String fill(String text, Map<String, String> vars) {
        if (text == null || text.isEmpty()) return "";
        Matcher m = VAR.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(vars.getOrDefault(m.group(1), m.group(0))));
        m.appendTail(sb);
        return sb.toString();
    }

    public static double number(String expr, Map<String, String> vars) {
        if (expr == null || expr.isBlank()) return 0;
        String e = fill(expr.trim(), vars).replace(" ", "");
        try {
            double total = 0;
            for (String term : e.split("(?=[+])|(?<=[0-9.])(?=-)")) {
                if (term.isEmpty()) continue;
                double product = 1;
                for (String factor : term.split("\\*")) product *= Double.parseDouble(factor.replace("+", ""));
                total += product;
            }
            return total;
        } catch (NumberFormatException ex) { return 0; }
    }
}
