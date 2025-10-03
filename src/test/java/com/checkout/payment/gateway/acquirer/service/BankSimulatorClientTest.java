package com.checkout.payment.gateway.acquirer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.acquirer.exception.BankServiceException;
import com.checkout.payment.gateway.acquirer.model.BankPaymentRequest;
import com.checkout.payment.gateway.acquirer.model.BankPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@DisplayName("Bank Simulator Client")
class BankSimulatorClientTest {

  private static final String BASE_URL = "http://localhost:8080";
  private static final String PAYMENTS_ENDPOINT = BASE_URL + "/payments";

  private BankSimulatorClient bankSimulatorClient;
  private MockRestServiceServer mockServer;
  private BankPaymentRequest baseRequest;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
    bankSimulatorClient = new BankSimulatorClient(restTemplate, BASE_URL);

    baseRequest =
        BankPaymentRequest.builder()
            // Ends in odd number for a success case
            .cardNumber("2222405343248877")
            .expiryDate("04/2025")
            .currency("GBP")
            .amount(100)
            .cvv("123")
            .build();
  }

  @Nested
  @DisplayName("when payment processing is successful")
  class SuccessScenarios {

    @Test
    @DisplayName("should send correct payment details and return an authorized response")
    void shouldReturnAuthorizedForSuccessfulPayment() {

      mockServer
          .expect(requestTo(PAYMENTS_ENDPOINT))
          .andExpect(method(HttpMethod.POST))
          .andExpect(jsonPath("$.card_number").value(baseRequest.getCardNumber()))
          .andExpect(jsonPath("$.expiry_date").value(baseRequest.getExpiryDate()))
          .andExpect(jsonPath("$.currency").value(baseRequest.getCurrency()))
          .andExpect(jsonPath("$.amount").value(baseRequest.getAmount()))
          .andExpect(jsonPath("$.cvv").value(baseRequest.getCvv()))
          .andRespond(
              withSuccess()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body("{\"authorized\": true, \"authorization_code\": \"abc123\"}"));

      BankPaymentResponse response = bankSimulatorClient.processPayment(baseRequest);

      assertThat(response.getAuthorized()).isTrue();
      assertThat(response.getAuthorizationCode()).isEqualTo("abc123");
      mockServer.verify();
    }
  }

  @Nested
  @DisplayName("when payment processing fails")
  class FailureScenarios {

    @Test
    @DisplayName("should return a declined response when the bank declines the payment")
    void shouldReturnDeclinedWhenBankDeclines() {

      BankPaymentRequest declinedRequest =
          baseRequest.toBuilder()
              .cardNumber("2222405343248888") // Ends in an even number
              .build();

      mockServer
          .expect(requestTo(PAYMENTS_ENDPOINT))
          .andExpect(method(HttpMethod.POST))
          .andRespond(
              withSuccess()
                  .contentType(MediaType.APPLICATION_JSON)
                  .body("{\"authorized\": false, \"authorization_code\": null}"));

      BankPaymentResponse response = bankSimulatorClient.processPayment(declinedRequest);

      assertThat(response.getAuthorized()).isFalse();
      assertThat(response.getAuthorizationCode()).isNull();
      mockServer.verify();
    }

    @Test
    @DisplayName("should throw BankServiceException when the bank service is not available")
    void shouldThrowExceptionForServiceUnavailable() {
      BankPaymentRequest errorRequest =
          baseRequest.toBuilder().cardNumber("2222405343248880").build();

      mockServer
          .expect(requestTo(PAYMENTS_ENDPOINT))
          .andExpect(method(HttpMethod.POST))
          .andRespond(withServerError().body("Service Temporarily Unavailable"));

      assertThatThrownBy(() -> bankSimulatorClient.processPayment(errorRequest))
          .isInstanceOf(BankServiceException.class)
          .hasMessageContaining("Bank service not available");

      mockServer.verify();
    }
  }
}
