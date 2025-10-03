package com.checkout.payment.gateway.acquirer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class BankPaymentRequest {
  @JsonProperty("card_number")
  String cardNumber;

  @JsonProperty("expiry_date")
  String expiryDate;

  String currency;
  Integer amount;
  String cvv;
}
