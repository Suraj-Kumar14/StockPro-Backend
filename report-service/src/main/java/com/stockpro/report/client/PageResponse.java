package com.stockpro.report.client;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content = new ArrayList<>();
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
