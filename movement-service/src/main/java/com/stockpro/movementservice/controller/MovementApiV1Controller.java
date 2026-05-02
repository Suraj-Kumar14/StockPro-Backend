package com.stockpro.movementservice.controller;

import com.stockpro.movementservice.dto.request.CreateMovementRequest;
import com.stockpro.movementservice.dto.request.MovementSearchRequest;
import com.stockpro.movementservice.dto.request.ReverseMovementRequest;
import com.stockpro.movementservice.dto.response.MovementAnalyticsResponse;
import com.stockpro.movementservice.dto.response.MovementResponse;
import com.stockpro.movementservice.dto.response.MovementSummaryResponse;
import com.stockpro.movementservice.enums.MovementDirection;
import com.stockpro.movementservice.enums.MovementType;
import com.stockpro.movementservice.enums.ReferenceType;
import com.stockpro.movementservice.security.AuthenticatedUser;
import com.stockpro.movementservice.service.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Immutable stock movement audit APIs")
@SecurityRequirement(name = "bearerAuth")
public class MovementApiV1Controller {

    private final MovementService movementService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Create a manual stock movement", description = "Creates an immutable movement record. Existing movements cannot be edited or deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movement created"),
            @ApiResponse(responseCode = "400", description = "Invalid movement request", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<MovementResponse> createMovement(@Valid @RequestBody CreateMovementRequest request, Authentication authentication) {
        return ResponseEntity.status(201).body(movementService.createMovement(request, actorId(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get paginated movement history")
    public ResponseEntity<Page<MovementResponse>> getAllMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "movementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(movementService.getAllMovements(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Search movement history with filters")
    public ResponseEntity<Page<MovementResponse>> searchMovements(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) MovementDirection direction,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) java.math.BigDecimal minQuantity,
            @RequestParam(required = false) java.math.BigDecimal maxQuantity,
            @RequestParam(required = false) Boolean isReversal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "movementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        MovementSearchRequest request = MovementSearchRequest.builder()
                .keyword(keyword)
                .productId(productId)
                .warehouseId(warehouseId)
                .movementType(movementType)
                .direction(direction)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .performedBy(performedBy)
                .fromDate(fromDate)
                .toDate(toDate)
                .minQuantity(minQuantity)
                .maxQuantity(maxQuantity)
                .isReversal(isReversal)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
        return ResponseEntity.ok(movementService.searchMovements(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get a movement by id")
    public ResponseEntity<MovementResponse> getMovementById(@PathVariable Long id) {
        return ResponseEntity.ok(movementService.getMovementById(id));
    }

    @GetMapping("/number/{movementNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get a movement by movement number")
    public ResponseEntity<MovementResponse> getMovementByNumber(@PathVariable String movementNumber) {
        return ResponseEntity.ok(movementService.getMovementByNumber(movementNumber));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get movements by product")
    public ResponseEntity<Page<MovementResponse>> getMovementsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(movementService.getMovementsByProduct(productId, page, size));
    }

    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Get movements by warehouse")
    public ResponseEntity<Page<MovementResponse>> getMovementsByWarehouse(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(movementService.getMovementsByWarehouse(warehouseId, page, size));
    }

    @GetMapping("/reference")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','OFFICER')")
    @Operation(summary = "Get movements by reference")
    public ResponseEntity<Page<MovementResponse>> getMovementsByReference(
            @RequestParam ReferenceType referenceType,
            @RequestParam String referenceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(movementService.getMovementsByReference(referenceType.name(), referenceId, page, size));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    @Operation(summary = "Get movements by user", description = "Warehouse staff can only access their own movement history.")
    public ResponseEntity<Page<MovementResponse>> getMovementsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (hasRole(authentication, "STAFF") && !userId.equals(actorId(authentication))) {
            throw new org.springframework.security.access.AccessDeniedException("Staff can only view their own movements");
        }
        return ResponseEntity.ok(movementService.getMovementsByUser(userId, page, size));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Reverse a movement", description = "Creates a reversal movement. Existing movement records remain immutable.")
    public ResponseEntity<MovementResponse> reverseMovement(
            @PathVariable Long id,
            @Valid @RequestBody ReverseMovementRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(movementService.reverseMovement(id, request, actorId(authentication)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get movement summary")
    public ResponseEntity<MovementSummaryResponse> getMovementSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(movementService.getMovementSummary(toStart(fromDate), toEnd(toDate)));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Get movement analytics")
    public ResponseEntity<MovementAnalyticsResponse> getMovementAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(movementService.getMovementAnalytics(toStart(fromDate), toEnd(toDate)));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Export movement history to CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) MovementType movementType,
            @RequestParam(required = false) MovementDirection direction,
            @RequestParam(required = false) ReferenceType referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) Long performedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) java.math.BigDecimal minQuantity,
            @RequestParam(required = false) java.math.BigDecimal maxQuantity,
            @RequestParam(defaultValue = "movementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        MovementSearchRequest request = MovementSearchRequest.builder()
                .keyword(keyword)
                .productId(productId)
                .warehouseId(warehouseId)
                .movementType(movementType)
                .direction(direction)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .performedBy(performedBy)
                .fromDate(fromDate)
                .toDate(toDate)
                .minQuantity(minQuantity)
                .maxQuantity(maxQuantity)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .size(500)
                .build();
        byte[] content = movementService.exportMovementsToCsv(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.attachment().filename("movements-export.csv").build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    private Long actorId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal.userId();
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    private LocalDateTime toStart(LocalDate value) {
        return value != null ? value.atStartOfDay() : null;
    }

    private LocalDateTime toEnd(LocalDate value) {
        return value != null ? value.plusDays(1).atStartOfDay().minusNanos(1) : null;
    }
}
