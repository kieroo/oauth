package com.example.oauth.util;

import java.util.HashMap;
import java.util.Map;

public final class HtmlEscaper {
    private HtmlEscaper() {
    }

    public static String escape(String input) {
        Map<String, String> escapes = new HashMap<>();
        escapes.put("&", "&amp;");
        escapes.put("<", "&lt;");
        escapes.put(">", "&gt;");
        escapes.put("\"", "&quot;");
        escapes.put("'", "&#39;");
        String escaped = input;
        for (Map.Entry<String, String> entry : escapes.entrySet()) {
            escaped = escaped.replace(entry.getKey(), entry.getValue());
        }
        return escaped;
    }
}
