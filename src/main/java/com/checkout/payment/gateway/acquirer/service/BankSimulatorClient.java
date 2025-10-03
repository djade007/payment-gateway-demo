package com.checkout.payment.gateway.acquirer.service;

import com.checkout.payment.gateway.acquirer.exception.BankServiceException;
import com.checkout.payment.gateway.acquirer.model.BankPaymentRequest;
import com.checkout.payment.gateway.acquirer.model.BankPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class BankSimulatorClient implements AcquiringBankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankSimulatorClient.class);

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public BankSimulatorClient(
      RestTemplate restTemplate, @Value("${acquiring-bank.simulator.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  @Override
  public BankPaymentResponse processPayment(BankPaymentRequest request) {
    try {
      LOG.debug("Sending payment request to bank simulator: {}", request);

      ResponseEntity<BankPaymentResponse> response =
          restTemplate.postForEntity(bankUrl + "/payments", request, BankPaymentResponse.class);

      LOG.debug("Received response from bank: {}", response.getBody());
      return response.getBody();

    } catch (HttpServerErrorException e) {
      LOG.error("Bank service not available: {}", e.getMessage());
      throw new BankServiceException("Bank service not available", e);
    } catch (RestClientException e) {
      LOG.error("Error connecting to bank: {}", e.getMessage());
      throw new BankServiceException("Error connecting to bank", e);
    }
  }
}
