package com.talktrip.talktrip.global.logger;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

public class P6SpyMultilineFormat implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        String pretty = prettySql(sql);
        return new StringBuilder()
                .append("/* time=").append(elapsed).append("ms, category=").append(category)
                .append(", connection=").append(connectionId).append(" */\n")
                .append(pretty)
                .toString();
    }

    private String prettySql(String sql) {
        // Normalize whitespace first
        String normalized = sql.replaceAll("\\s+", " ").trim();
        // Insert line breaks before common SQL keywords to make it multi-line
        String[] keywords = {
                "select", "from", "where", "group by", "having", "order by", "limit", "offset",
                "inner join", "left join", "right join", "join", "on", "and", "or", "values", "set"
        };
        String pretty = normalized;
        for (String kw : keywords) {
            // case-insensitive replace: add line break before keyword, but avoid double breaks
            pretty = pretty.replaceAll("(?i)\\s+" + kw + "\\s", "\n    " + kw.toUpperCase() + " ");
        }
        // Ensure the very first SELECT (or other starting keyword) is on its own line without indentation
        pretty = pretty.replaceFirst("^(?i)SELECT", "SELECT");
        // Clean up multiple newlines
        pretty = pretty.replaceAll("\n{2,}", "\n");
        return pretty;
    }
}
