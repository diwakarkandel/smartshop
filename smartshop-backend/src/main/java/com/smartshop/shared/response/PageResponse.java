package com.smartshop.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(source.getContent().stream().map(mapper).toList())
                .page(source.getNumber())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .first(source.isFirst())
                .last(source.isLast())
                .build();
    }

    public static <T> PageResponse<T> from(Page<T> source) {
        return from(source, Function.identity());
    }
}