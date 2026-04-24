package com.stockpro.movement.service;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.enums.MovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public interface MovementService {

    MovementResponse recordMovement(RecordMovementRequest request);

    List<MovementResponse> getByProduct(Long productId);

    List<MovementResponse> getByWarehouse(Long warehouseId);

    List<MovementResponse> getByType(MovementType movementType);

    List<MovementResponse> getByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    List<MovementResponse> getByReference(Long referenceId);

    List<MovementResponse> getMovementHistory(Long productId, Long warehouseId);

    BigDecimal getStockIn(Long productId);

    BigDecimal getStockOut(Long productId);

    Page<MovementResponse> getAllMovements(int page, int size, String sortBy, String sortDir);

    Page<MovementResponse> searchMovements(MovementSearchRequest request);
}
