package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.request.CreateMovementFromEventRequest;
import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.MovementSearchRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse;
import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.dto.response.MovementSummaryResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public interface MovementService {

    MovementResponse createMovement(CreateMovementRequest request, Long actorId);

    MovementResponse createMovementFromEvent(CreateMovementFromEventRequest request);

    MovementResponse getMovementById(Long movementId);

    MovementResponse getMovementByNumber(String movementNumber);

    Page<MovementResponse> getAllMovements(int page, int size, String sortBy, String sortDir);

    Page<MovementResponse> searchMovements(MovementSearchRequest request);

    Page<MovementResponse> getMovementsByProduct(Long productId, int page, int size);

    Page<MovementResponse> getMovementsByWarehouse(Long warehouseId, int page, int size);

    Page<MovementResponse> getMovementsByReference(String referenceType, String referenceId, int page, int size);

    Page<MovementResponse> getMovementsByUser(Long userId, int page, int size);

    MovementResponse reverseMovement(Long movementId, ReverseMovementRequest request, Long actorId);

    MovementSummaryResponse getMovementSummary(LocalDateTime fromDate, LocalDateTime toDate);

    List<MovementResponse> getRecentMovements(int limit);

    MovementAnalyticsResponse getMovementAnalytics(LocalDateTime fromDate, LocalDateTime toDate);

    byte[] exportMovementsToCsv(MovementSearchRequest request);
}
