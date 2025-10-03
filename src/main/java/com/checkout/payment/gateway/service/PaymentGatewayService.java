package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.acquirer.model.BankPaymentRequest;
import com.checkout.payment.gateway.acquirer.model.BankPaymentResponse;
import com.checkout.payment.gateway.acquirer.service.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;

  public PaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Processing payment request: {}", paymentRequest);

    BankPaymentRequest bankRequest =
        BankPaymentRequest.builder()
            .cardNumber(paymentRequest.getCardNumber())
            .expiryDate(paymentRequest.getExpiryDate())
            .currency(paymentRequest.getCurrency())
            .amount(paymentRequest.getAmount())
            .cvv(paymentRequest.getCvv())
            .build();

    BankPaymentResponse bankResponse = acquiringBankClient.processPayment(bankRequest);

    PaymentResponse response =
        PaymentResponse.builder()
            .id(UUID.randomUUID())
            .status(
                bankResponse.getAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED)
            .cardNumberLastFour(Integer.parseInt(paymentRequest.getCardNumberLastFour()))
            .expiryMonth(paymentRequest.getExpiryMonth())
            .expiryYear(paymentRequest.getExpiryYear())
            .currency(paymentRequest.getCurrency())
            .amount(paymentRequest.getAmount())
            .build();

    paymentsRepository.add(response);

    LOG.debug("Payment successfully processed with ID: {}", response.getId());
    return response;
  }
}
