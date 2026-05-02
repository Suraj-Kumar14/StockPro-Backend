package com.stockpro.movementservice.dto.response;

public record MovementExportResponse(
        String fileName,
        byte[] content,
        String contentType) {
}
