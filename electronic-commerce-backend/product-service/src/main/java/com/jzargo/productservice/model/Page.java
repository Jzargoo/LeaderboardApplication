package com.jzargo.productservice.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
public class Page<T> {
    private final Metadata metadata;
    private final List<T> content;

    public Page(List<T> content, int page, int ttlEl, int sizeOfPage) {
        this.metadata = new Metadata(
                page,
                sizeOfPage,
                ttlEl
        );

        this.content = content;
    }

    @Data
    public static class Metadata {
        private final int page;
        private final int ttlPages;
        private final int sizeOfPage;
        private final int totalElements;

        public Metadata(int page, int sizeOfPage, int totalElements) {
            this.page = page;
            this.sizeOfPage = sizeOfPage;
            this.totalElements = totalElements;

            if (sizeOfPage > 0) {
                this.ttlPages = (int) Math.ceil( (double) totalElements / sizeOfPage);
            } else {
                this.ttlPages = 0;
            }
        }
    }
}
