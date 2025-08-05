package com.talktrip.talktrip.global.util;

import java.util.Map;

public class CountUtil {
    public static boolean containsAllKeywordCounts(String text, Map<String, Integer> requiredCounts) {
        for (Map.Entry<String, Integer> entry : requiredCounts.entrySet()) {
            String keyword = entry.getKey();
            int requiredCount = entry.getValue();

            int actualCount = countKeywordOccurrences(text.toLowerCase(), keyword.toLowerCase());
            if (actualCount < requiredCount) {
                return false;
            }
        }
        return true;
    }

    public static int countKeywordOccurrences(String text, String keyword) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

}
