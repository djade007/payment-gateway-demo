package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PaymentResponse {
  UUID id;
  PaymentStatus status;
  int cardNumberLastFour;
  int expiryMonth;
  int expiryYear;
  String currency;
  int amount;
}
