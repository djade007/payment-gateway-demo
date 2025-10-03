package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.FutureExpiryDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@FutureExpiryDate
public class PostPaymentRequest implements Serializable {

  @NotNull(message = "Card Number is required")
  @Pattern(regexp = "^[0-9]{14,19}$", message = "Card Number must be between 14-19 digits")
  @JsonProperty("card_number")
  private String cardNumber;

  @NotNull(message = "Expiry Month is required")
  @Min(value = 1, message = "Expiry Month must be between 1 and 12")
  @Max(value = 12, message = "Expiry Month must be between 1 and 12")
  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @NotNull(message = "Expiry Year is required")
  @JsonProperty("expiry_year")
  private Integer expiryYear;

  @NotNull(message = "Currency is required")
  @Size(min = 3, max = 3, message = "Currency must be 3 characters")
  @Pattern(
      regexp = "^(GBP|EUR|USD)$",
      message = "Currency must be one of the supported types (GBP, EUR, USD)")
  private String currency;

  @NotNull(message = "Amount is required")
  @Min(value = 1, message = "Amount must be greater than zero")
  private Integer amount;

  @NotNull(message = "CVV is required")
  @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
  private String cvv;

  @JsonIgnore
  public String getExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }

  @JsonIgnore
  public String getCardNumberLastFour() {
    return cardNumber != null && cardNumber.length() >= 4
        ? cardNumber.substring(cardNumber.length() - 4)
        : "";
  }
}
