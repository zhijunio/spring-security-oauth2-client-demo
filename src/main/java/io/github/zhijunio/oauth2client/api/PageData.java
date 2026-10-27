package io.github.zhijunio.oauth2client.api;

import java.util.List;

public record PageData<T>(List<T> data, int totalPages, long totalElements, boolean hasNext) {

    public static <T> PageData<T> from(List<T> items, int pageSize, int page) {
        int totalPages = (items.size() + pageSize - 1) / pageSize;
        int from = (int) Math.min((long) page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageData<>(items.subList(from, to), totalPages, items.size(), (long) page + 1 < totalPages);
    }
}
