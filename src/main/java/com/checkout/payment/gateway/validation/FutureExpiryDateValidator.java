package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class FutureExpiryDateValidator
    implements ConstraintValidator<FutureExpiryDate, PostPaymentRequest> {

  @Override
  public boolean isValid(PostPaymentRequest request, ConstraintValidatorContext context) {
    // Single Responsibility Principle.
    // Allow null fields to be validated by @NotNull to prevent duplicate checks
    if (hasInvalidFields(request)) {
      return true;
    }

    YearMonth expiryDate = YearMonth.of(request.getExpiryYear(), request.getExpiryMonth());
    YearMonth currentDate = YearMonth.now();

    return expiryDate.isAfter(currentDate) || expiryDate.equals(currentDate);
  }

  private boolean hasInvalidFields(PostPaymentRequest request) {
    if (request.getExpiryMonth() == null || request.getExpiryYear() == null) {
      return true;
    }

    return request.getExpiryMonth() < 1 || request.getExpiryMonth() > 12;
  }
}
