package com.neurolearn.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The app shows question text as plain text (no LaTeX renderer), matching the seeded
 * questions' style ("x^2", "√", "±"). Models often answer in LaTeX anyway, so generated
 * text is converted: {@code $2x^{2} \pm \sqrt{8}$} → {@code 2x^2 ± √(8)}.
 */
public final class MathText {

    private static final Map<String, String> COMMANDS = new LinkedHashMap<>();
    static {
        COMMANDS.put("\\pm", "±");
        COMMANDS.put("\\mp", "∓");
        COMMANDS.put("\\times", "×");
        COMMANDS.put("\\cdot", "·");
        COMMANDS.put("\\div", "÷");
        COMMANDS.put("\\leq", "≤");
        COMMANDS.put("\\le", "≤");
        COMMANDS.put("\\geq", "≥");
        COMMANDS.put("\\ge", "≥");
        COMMANDS.put("\\neq", "≠");
        COMMANDS.put("\\ne", "≠");
        COMMANDS.put("\\approx", "≈");
        COMMANDS.put("\\Delta", "Δ");
        COMMANDS.put("\\alpha", "α");
        COMMANDS.put("\\beta", "β");
        COMMANDS.put("\\pi", "π");
        COMMANDS.put("\\infty", "∞");
        COMMANDS.put("\\Rightarrow", "⇒");
        COMMANDS.put("\\rightarrow", "→");
        COMMANDS.put("\\to", "→");
        COMMANDS.put("\\quad", " ");
        COMMANDS.put("\\,", " ");
        COMMANDS.put("\\;", " ");
        COMMANDS.put("\\left", "");
        COMMANDS.put("\\right", "");
        COMMANDS.put("\\%", "%");
        COMMANDS.put("\\$", "₹");
    }

    private static final Pattern FRAC = Pattern.compile("\\\\[dt]?frac\\{([^{}]*)\\}\\{([^{}]*)\\}");
    private static final Pattern SQRT = Pattern.compile("\\\\sqrt\\{([^{}]*)\\}");
    private static final Pattern TEXT = Pattern.compile("\\\\(?:text|mathrm|mathbf|textbf|mathit)\\{([^{}]*)\\}");
    private static final Pattern SUPER = Pattern.compile("\\^\\{([^{}]*)\\}");
    private static final Pattern SUB = Pattern.compile("_\\{([^{}]*)\\}");
    private static final Pattern INLINE = Pattern.compile("\\$\\$?([^$]+?)\\$\\$?");
    /** Operators, LaTeX commands, or a digit/letter pair like "2x" — signs that $...$ wraps maths. */
    private static final Pattern MATH_SIGNAL = Pattern.compile("[\\\\^=+*/<>_]|\\d[a-zA-Z]|[a-zA-Z]\\d|^\\s*[a-zA-Z]\\s*$");
    private static final Pattern PAREN_DELIMS =Pattern.compile("\\\\[()\\[\\]]");

    private MathText() {
    }

    public static String toPlain(String s) {
        if (s == null || s.isEmpty()) return s;
        String out = s;
        // Strip $...$ / $$...$$ delimiters (keep content) — but only when the content looks like maths,
        // so "costs $15 and $9" (two amounts) is not mistaken for one LaTeX span.
        out = INLINE.matcher(out).replaceAll(m -> Matcher.quoteReplacement(
                MATH_SIGNAL.matcher(m.group(1)).find() ? m.group(1) : m.group(0)));
        out = PAREN_DELIMS.matcher(out).replaceAll("");
        out = TEXT.matcher(out).replaceAll(m -> Matcher.quoteReplacement(m.group(1)));
        // Inner-most first, repeated so nested \frac{a_{1}}{\sqrt{..}} resolves in one call.
        for (int i = 0; i < 4; i++) {
            out = SUPER.matcher(out).replaceAll(m -> Matcher.quoteReplacement("^" + wrapIfLong(m.group(1))));
            out = SUB.matcher(out).replaceAll(m -> Matcher.quoteReplacement("_" + wrapIfLong(m.group(1))));
            out = SQRT.matcher(out).replaceAll(m -> Matcher.quoteReplacement("√(" + m.group(1) + ")"));
            out = FRAC.matcher(out).replaceAll(m -> Matcher.quoteReplacement(wrap(m.group(1)) + "/" + wrap(m.group(2))));
        }
        out = out.replace("\\sqrt", "√");
        for (Map.Entry<String, String> e : COMMANDS.entrySet()) {
            out = out.replace(e.getKey(), e.getValue());
        }
        // A leftover "$15"-style amount → rupees, matching the Indian-classroom setting.
        out = out.replaceAll("\\$\\s?(\\d)", "₹$1");
        return out.replaceAll("[ \\t]{2,}", " ").trim();
    }

    private static String wrap(String s) {
        String t = s.trim();
        // Only a plain number or a single letter is safe without brackets ("/2a" would read as (/2)·a).
        return t.matches("\\d+(\\.\\d+)?|[A-Za-z]") ? t : "(" + t + ")";
    }

    private static String wrapIfLong(String s) {
        String t = s.trim();
        return t.length() <= 1 || t.matches("\\d+") ? t : "(" + t + ")";
    }
}
