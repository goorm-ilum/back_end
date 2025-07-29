package com.talktrip.talktrip.global.util;

import java.util.List;

public class PaginationUtil {

    public static <T> List<T> paginate(List<T> sourceList, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= sourceList.size()) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + size, sourceList.size());
        return sourceList.subList(fromIndex, toIndex);
    }
}
