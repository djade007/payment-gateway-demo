package com.checkout.payment.gateway.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment Request Validation")
class PostPaymentRequestValidationTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setup() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    if (validatorFactory != null) {
      validatorFactory.close();
    }
  }

  @Test
  @DisplayName("should pass for a valid request")
  void shouldPassForValidRequest() {
    PostPaymentRequest request = createValidRequest();
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertThat(violations).isEmpty();
  }

  @Nested
  @DisplayName("for Card Number")
  class CardNumberValidation {

    @Test
    @DisplayName("should fail when card number is null")
    void shouldFailWhenCardNumberIsNull() {
      PostPaymentRequest request = createValidRequest().toBuilder().cardNumber(null).build();
      assertHasViolation(request, "Card Number is required");
    }

    @ParameterizedTest(name = "should fail for invalid card number: {0}")
    @ValueSource(strings = {"12345", "123456789012345678901", "12345invalid"})
    void shouldFailForInvalidCardNumber(String cardNumber) {
      PostPaymentRequest request = createValidRequest().toBuilder().cardNumber(cardNumber).build();
      assertHasViolation(request, "Card Number must be between 14-19 digits");
    }
  }


  @Nested
  @DisplayName("for Expiry Date")
  class ExpiryDateValidation {

    @Test
    @DisplayName("should fail when expiry month is null")
    void shouldFailWhenExpiryMonthIsNull() {
      PostPaymentRequest request = createValidRequest().toBuilder().expiryMonth(null).build();
      assertHasViolation(request, "Expiry Month is required");
    }

    @Test
    @DisplayName("should fail when expiry year is null")
    void shouldFailWhenExpiryYearIsNull() {
      PostPaymentRequest request = createValidRequest().toBuilder().expiryYear(null).build();
      assertHasViolation(request, "Expiry Year is required");
    }

    @ParameterizedTest(name = "should fail for out of range expiry month: {0}")
    @ValueSource(ints = {0, 13})
    void shouldFailWhenExpiryMonthIsOutOfRange(Integer month) {
      PostPaymentRequest request = createValidRequest().toBuilder().expiryMonth(month).build();
      assertHasViolation(request, "Expiry Month must be between 1 and 12");
    }

    @Test
    @DisplayName("should fail when expiry date is in the past")
    void shouldFailForPastExpiryDate() {
      YearMonth pastDate = YearMonth.now().minusMonths(1);
      PostPaymentRequest request =
          createValidRequest().toBuilder()
              .expiryMonth(pastDate.getMonthValue())
              .expiryYear(pastDate.getYear())
              .build();

      assertHasViolation(request, "Expiry Date must be in the future");
    }

    @Test
    @DisplayName("should pass when expiry date is the current month")
    void shouldPassForCurrentMonthExpiryDate() {
      YearMonth currentDate = YearMonth.now();
      PostPaymentRequest request =
          createValidRequest().toBuilder()
              .expiryMonth(currentDate.getMonthValue())
              .expiryYear(currentDate.getYear())
              .build();

      Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("for Currency")
  class CurrencyValidation {

    @Test
    @DisplayName("should fail when currency is null")
    void shouldFailWhenCurrencyIsNull() {
      PostPaymentRequest request = createValidRequest().toBuilder().currency(null).build();
      assertHasViolation(request, "Currency is required");
    }

    @ParameterizedTest(name = "should fail for unsupported currency: {0}")
    @ValueSource(strings = {"ABC", "NGN", "KES", "RWF", "TZS"})
    void shouldFailForInvalidCurrency(String currency) {
      PostPaymentRequest request = createValidRequest().toBuilder().currency(currency).build();
      assertHasViolation(request, "Currency must be one of the supported types (GBP, EUR, USD)");
    }

    @ParameterizedTest(name = "should pass for supported currency: {0}")
    @ValueSource(strings = {"GBP", "EUR", "USD"})
    void shouldPassForSupportedCurrencies(String currency) {
      PostPaymentRequest request = createValidRequest().toBuilder().currency(currency).build();
      Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("for Amount")
  class AmountValidation {

    @Test
    @DisplayName("should fail when amount is null")
    void shouldFailForNullAmount() {
      PostPaymentRequest request = createValidRequest().toBuilder().amount(null).build();
      assertHasViolation(request, "Amount is required");
    }

    @ParameterizedTest(name = "should fail for non-positive amount: {0}")
    @ValueSource(ints = {0, -100, -30, -10})
    void shouldFailForNonPositiveAmount(Integer amount) {
      PostPaymentRequest request = createValidRequest().toBuilder().amount(amount).build();
      assertHasViolation(request, "Amount must be greater than zero");
    }
  }

  @Nested
  @DisplayName("for CVV")
  class CvvValidation {

    @Test
    @DisplayName("should fail when CVV is null")
    void shouldFailWhenCvvIsNull() {
      PostPaymentRequest request = createValidRequest().toBuilder().cvv(null).build();
      assertHasViolation(request, "CVV is required");
    }

    @ParameterizedTest(name = "should fail for invalid CVV: {0}")
    @ValueSource(strings = {"1", "12", "12345", "12a"})
    void shouldFailForInvalidCvv(String cvv) {
      PostPaymentRequest request = createValidRequest().toBuilder().cvv(cvv).build();
      assertHasViolation(request, "CVV must be 3 or 4 digits");
    }

    @ParameterizedTest(name = "should pass for valid CVV: {0}")
    @ValueSource(strings = {"123", "1234"})
    void shouldPassForValidCvv(String cvv) {
      PostPaymentRequest request = createValidRequest().toBuilder().cvv(cvv).build();
      Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
      assertThat(violations).isEmpty();
    }
  }

  @Nested
  @DisplayName("for Model Helper Methods")
  class ModelHelperMethodsTest {
    @Test
    @DisplayName("getCardNumberLastFour() should return the last four digits")
    void shouldGetLastFourDigits() {
      PostPaymentRequest request =
          createValidRequest().toBuilder().cardNumber("1234567890123456").build();
      assertThat(request.getCardNumberLastFour()).isEqualTo("3456");
    }

    @Test
    @DisplayName("getExpiryDate() should format the date correctly as MM/yyyy")
    void shouldFormatExpiryDate() {
      PostPaymentRequest request =
          createValidRequest().toBuilder().expiryMonth(5).expiryYear(2028).build();
      assertThat(request.getExpiryDate()).isEqualTo("05/2028");
    }
  }

  // Helper methods
  private PostPaymentRequest createValidRequest() {
    return PostPaymentRequest.builder()
        .cardNumber("1234567898765432")
        .expiryMonth(5)
        .expiryYear(YearMonth.now().getYear() + 5)
        .currency("GBP")
        .amount(92)
        .cvv("123")
        .build();
  }

  private void assertHasViolation(PostPaymentRequest request, String expectedMessage) {
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);
    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getMessage().contains(expectedMessage));
  }
}
