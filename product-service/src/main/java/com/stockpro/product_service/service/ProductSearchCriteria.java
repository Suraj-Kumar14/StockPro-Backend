package com.stockpro.product_service.service;

public record ProductSearchCriteria(
        String keyword,
        String name,
        String category,
        String brand,
        int page,
        int size) {
}
