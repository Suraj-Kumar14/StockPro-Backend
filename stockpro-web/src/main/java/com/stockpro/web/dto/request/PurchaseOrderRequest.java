package com.stockpro.web.dto.request;

import com.stockpro.web.dto.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    private Long poId;

    @NotNull(message = "Supplier is required.")
    @Positive(message = "Supplier is invalid.")
    private Long supplierId;

    @NotNull(message = "Warehouse is required.")
    @Positive(message = "Warehouse is invalid.")
    private Long warehouseId;

    @NotNull(message = "Created by user is required.")
    @Positive(message = "Created by user is invalid.")
    private Long createdById;

    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expectedDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate receivedDate;

    private String notes;
    private String referenceNumber;

    @Valid
    @NotEmpty(message = "At least one line item is required.")
    private List<PurchaseLineItemRequest> lineItems = new ArrayList<>();
}
