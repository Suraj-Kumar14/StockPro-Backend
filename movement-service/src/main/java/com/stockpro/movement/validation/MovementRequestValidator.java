package com.stockpro.movement.validation;

import com.stockpro.movement.client.ProductServiceClient;
import com.stockpro.movement.dto.request.MovementSearchRequest;
import com.stockpro.movement.dto.request.RecordMovementRequest;
import com.stockpro.movement.dto.response.ProductSummaryResponse;
import com.stockpro.movement.exception.ExternalServiceException;
import com.stockpro.movement.exception.InvalidMovementRequestException;
import com.stockpro.movement.exception.MovementNotFoundException;
import com.stockpro.movement.exception.UnauthorizedMovementAccessException;
import com.stockpro.movement.security.AuthenticatedUser;
import feign.FeignException;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MovementRequestValidator {

    private final ProductServiceClient productServiceClient;
    private final boolean productValidationEnabled;

    public MovementRequestValidator(ProductServiceClient productServiceClient,
            @Value("${movement.validation.product-enabled:true}") boolean productValidationEnabled) {
        this.productServiceClient = productServiceClient;
        this.productValidationEnabled = productValidationEnabled;
    }

    public void validateRecordRequest(RecordMovementRequest request, AuthenticatedUser authenticatedUser) {
        if (request == null) {
            throw new InvalidMovementRequestException("Movement payload is required.");
        }

        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new UnauthorizedMovementAccessException("Authenticated user information is required.");
        }

        if (request.getPerformedBy() != null && !request.getPerformedBy().equals(authenticatedUser.userId())) {
            throw new UnauthorizedMovementAccessException(
                    "performedBy must match the authenticated user in the JWT token.");
        }

        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMovementRequestException("quantity must be greater than zero.");
        }

        if (request.getBalanceAfter() == null || request.getBalanceAfter().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidMovementRequestException("balanceAfter cannot be negative.");
        }

        validateProductExists(request.getProductId());
    }

    public void validateSearchRequest(MovementSearchRequest request) {
        if (request == null) {
            throw new InvalidMovementRequestException("Search payload is required.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new InvalidMovementRequestException("endDate cannot be before startDate.");
        }
    }

    private void validateProductExists(Long productId) {
        if (!productValidationEnabled) {
            return;
        }

        try {
            ProductSummaryResponse product = productServiceClient.getProductById(productId);
            if (product.getIsActive() != null && !product.getIsActive()) {
                throw new InvalidMovementRequestException("Product is inactive: " + productId);
            }
        } catch (FeignException.NotFound exception) {
            throw new MovementNotFoundException("Product not found with id: " + productId);
        } catch (InvalidMovementRequestException exception) {
            throw exception;
        } catch (FeignException exception) {
            throw new ExternalServiceException("Unable to validate product using product-service.", exception);
        }
    }
}
