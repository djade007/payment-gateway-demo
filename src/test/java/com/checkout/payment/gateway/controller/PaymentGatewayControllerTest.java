package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.acquirer.model.BankPaymentResponse;
import com.checkout.payment.gateway.acquirer.service.BankSimulatorClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.Year;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Payment Gateway Controller")
class PaymentGatewayControllerTest {

  private static int FUTURE_YEAR;

  @Autowired private MockMvc mvc;
  @Autowired PaymentsRepository paymentsRepository;

  @MockBean private BankSimulatorClient bankSimulatorClient;

  @BeforeAll
  static void setUpClass() {
    FUTURE_YEAR = Year.now().getValue() + 5;
  }

  @Nested
  @DisplayName("GET /payment/{id}")
  class GetPayment {

    @Test
    @DisplayName("should return payment when ID exists")
    void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
      PaymentResponse payment =
          PaymentResponse.builder()
              .id(UUID.randomUUID())
              .amount(10)
              .currency("USD")
              .status(PaymentStatus.AUTHORIZED)
              .expiryMonth(12)
              .expiryYear(2024)
              .cardNumberLastFour("4321")
              .build();

      paymentsRepository.add(payment);

      mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
          .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
          .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
          .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
          .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
          .andExpect(jsonPath("$.amount").value(payment.getAmount()));
    }

    @Test
    @DisplayName("should return 404 when ID does not exist")
    void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
      mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Page not found"));
    }
  }

  @Nested
  @DisplayName("POST /payments")
  class ProcessPayment {

    @Nested
    @DisplayName("when request is valid")
    class ValidRequest {

      @Test
      @DisplayName("should return 'Authorized' when bank approves payment")
      void shouldReturnAuthorizedOnSuccessfulPayment() throws Exception {
        when(bankSimulatorClient.processPayment(any()))
            .thenReturn(
                BankPaymentResponse.builder().authorized(true).authorizationCode("abc123").build());

        String validPaymentRequest =
            """
                    {
                      "card_number": "2222405343248877",
                      "expiry_month": 12,
                      "expiry_year": %d,
                      "currency": "GBP",
                      "amount": 100,
                      "cvv": "123"
                    }
                    """
                .formatted(FUTURE_YEAR);

        mvc.perform(
                post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validPaymentRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("Authorized"))
            .andExpect(jsonPath("$.cardNumberLastFour").value("8877"));
      }

      @Test
      @DisplayName("should return 'Declined' when bank declines payment")
      void shouldReturnDeclinedWhenBankRejectsPayment() throws Exception {

        when(bankSimulatorClient.processPayment(any()))
            .thenReturn(BankPaymentResponse.builder().authorized(false).build());

        String paymentToBeDeclinedRequest =
            """
                    {
                      "card_number": "532241234324676",
                      "expiry_month": 12,
                      "expiry_year": %d,
                      "currency": "GBP",
                      "amount": 100,
                      "cvv": "123"
                    }
                    """
                .formatted(FUTURE_YEAR);

        mvc.perform(
                post("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(paymentToBeDeclinedRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Declined"))
            .andExpect(jsonPath("$.id").exists());
      }
    }

    @Nested
    @DisplayName("when request is invalid")
    class InvalidRequest {

      @DisplayName("should return 400 Bad Request")
      @ParameterizedTest(name = "when {0}")
      @MethodSource(
          "com.checkout.payment.gateway.controller.PaymentGatewayControllerTest#invalidPaymentRequestProvider")
      void shouldRejectInvalidPaymentRequests(
          String ignoredScenario, String invalidRequest, String expectedMessage) throws Exception {
        mvc.perform(
                post("/payments").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(expectedMessage));
      }
    }
  }

  private static Stream<Arguments> invalidPaymentRequestProvider() {
    return Stream.of(
        Arguments.of(
            "card number is too short",
            """
            { "card_number": "123", "expiry_month": 12, "expiry_year": %d, "currency": "GBP", "amount": 100, "cvv": "123" }
            """
                .formatted(FUTURE_YEAR),
            "Card Number must be between 14-19 digits"),
        Arguments.of(
            "expiry date is in the past",
            """
            { "card_number": "2222405343248877", "expiry_month": 1, "expiry_year": 2020, "currency": "GBP", "amount": 100, "cvv": "123" }
            """,
            "Expiry Date must be in the future"),
        Arguments.of(
            "currency is not supported",
            """
            { "card_number": "2222405343248877", "expiry_month": 12, "expiry_year": %d, "currency": "JPY", "amount": 100, "cvv": "123" }
            """
                .formatted(FUTURE_YEAR),
            "Currency must be one of the supported types (GBP, EUR, USD)"),
        Arguments.of(
            "amount is zero",
            """
            { "card_number": "2222405343248877", "expiry_month": 12, "expiry_year": %d, "currency": "GBP", "amount": 0, "cvv": "123" }
            """
                .formatted(FUTURE_YEAR),
            "Amount must be greater than zero"),
        Arguments.of(
            "CVV is invalid",
            """
            { "card_number": "2222405343248877", "expiry_month": 12, "expiry_year": %d, "currency": "GBP", "amount": 100, "cvv": "12" }
            """
                .formatted(FUTURE_YEAR),
            "CVV must be 3 or 4 digits"));
  }
}
