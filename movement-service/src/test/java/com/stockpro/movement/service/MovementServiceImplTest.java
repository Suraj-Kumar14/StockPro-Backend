package com.stockpro.movement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.MovementResponse;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.enums.MovementType;
import com.stockpro.movement.exception.InvalidMovementRequestException;
import com.stockpro.movement.mapper.MovementMapper;
import com.stockpro.movement.repository.MovementRepository;
import com.stockpro.movement.security.AuthenticatedUser;
import com.stockpro.movement.security.SecurityUtils;
import com.stockpro.movement.service.impl.MovementServiceImpl;
import com.stockpro.movement.validation.MovementRequestValidator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private MovementRequestValidator movementRequestValidator;

    @Mock
    private SecurityUtils securityUtils;

    private MovementMapper movementMapper;

    private MovementServiceImpl movementService;

    @BeforeEach
    void setUp() {
        movementMapper = new MovementMapper();
        movementService = new MovementServiceImpl(
                movementRepository,
                movementMapper,
                movementRequestValidator,
                securityUtils,
                "MOVEMENT-SERVICE");
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordMovementShouldUseAuthenticatedUserAndPersist() {
        RecordMovementRequest request = RecordMovementRequest.builder()
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_IN)
                .quantity(new BigDecimal("5.0000"))
                .referenceId(30L)
                .referenceType("PURCHASE_ORDER")
                .unitCost(new BigDecimal("100.0000"))
                .performedBy(99L)
                .notes("Received goods")
                .movementDate(LocalDateTime.of(2026, 4, 23, 11, 0))
                .balanceAfter(new BigDecimal("15.0000"))
                .build();

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                99L,
                "user@stockpro.com",
                "ADMIN",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(securityUtils.getCurrentUser()).thenReturn(authenticatedUser);
        when(movementRepository.save(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            movement.setMovementId(1L);
            movement.setCreatedAt(LocalDateTime.of(2026, 4, 23, 11, 1));
            return movement;
        });

        MovementResponse response = movementService.recordMovement(request);

        assertThat(response.getMovementId()).isEqualTo(1L);
        assertThat(response.getPerformedBy()).isEqualTo(99L);
        assertThat(response.getMovementType()).isEqualTo(MovementType.STOCK_IN);
        verify(movementRequestValidator).validateRecordRequest(request, authenticatedUser);
    }

    @Test
    void searchMovementsShouldReturnMappedPage() {
        MovementSearchRequest request = MovementSearchRequest.builder()
                .productId(10L)
                .page(0)
                .size(10)
                .sortBy("movementDate")
                .sortDir("desc")
                .build();

        StockMovement movement = StockMovement.builder()
                .movementId(1L)
                .productId(10L)
                .warehouseId(20L)
                .movementType(MovementType.STOCK_OUT)
                .quantity(new BigDecimal("2.0000"))
                .performedBy(99L)
                .movementDate(LocalDateTime.of(2026, 4, 23, 12, 0))
                .balanceAfter(new BigDecimal("13.0000"))
                .createdAt(LocalDateTime.of(2026, 4, 23, 12, 1))
                .build();

        Page<StockMovement> entityPage = new PageImpl<>(List.of(movement));
        when(movementRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

        Page<MovementResponse> responsePage = movementService.searchMovements(request);

        assertThat(responsePage.getContent()).hasSize(1);
        assertThat(responsePage.getContent().get(0).getMovementId()).isEqualTo(1L);
        verify(movementRequestValidator).validateSearchRequest(request);
    }

    @Test
    void getAllMovementsShouldRejectUnsupportedSortField() {
        assertThatThrownBy(() -> movementService.getAllMovements(0, 20, "unsupportedField", "desc"))
                .isInstanceOf(InvalidMovementRequestException.class)
                .hasMessageContaining("Unsupported sortBy value");
    }
}
