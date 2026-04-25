package com.stockpro.web.viewmodel;

import com.stockpro.web.dto.response.ApiPageResponse;
import java.util.ArrayList;
import java.util.List;

public record PageViewModel<T>(
        List<T> items,
        int page,
        int size,
        int totalPages,
        long totalElements,
        boolean first,
        boolean last,
        boolean empty) {

    public static <T> PageViewModel<T> from(ApiPageResponse<T> response) {
        return new PageViewModel<>(
                response.getContent() == null ? List.of() : response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalPages(),
                response.getTotalElements(),
                response.isFirst(),
                response.isLast(),
                response.isEmpty());
    }

    public static <T> PageViewModel<T> fromList(List<T> items, int page, int size) {
        List<T> safeItems = items == null ? List.of() : items;
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, safeItems.size());
        int toIndex = Math.min(fromIndex + safeSize, safeItems.size());
        List<T> content = new ArrayList<>(safeItems.subList(fromIndex, toIndex));
        int totalPages = safeItems.isEmpty() ? 1 : (int) Math.ceil((double) safeItems.size() / safeSize);

        return new PageViewModel<>(
                content,
                safePage,
                safeSize,
                totalPages,
                safeItems.size(),
                safePage == 0,
                safePage >= totalPages - 1,
                content.isEmpty());
    }
}
