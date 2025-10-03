package com.checkout.payment.gateway.acquirer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BankPaymentResponse {
  Boolean authorized;

  @JsonProperty("authorization_code")
  String authorizationCode;
}
