package com.talktrip.talktrip.global.util;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

public class SortUtil {
    public static Sort buildSort(List<String> sortParams) {
        String property = sortParams.get(0);
        String directionStr = sortParams.get(1);
        try {
            Sort.Direction direction = Sort.Direction.fromString(directionStr);
            return Sort.by(new Sort.Order(direction, property));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort direction: " + directionStr, e);
        }
    }
}

