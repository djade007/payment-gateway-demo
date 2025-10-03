package com.checkout.payment.gateway.acquirer.service;

import com.checkout.payment.gateway.acquirer.model.BankPaymentRequest;
import com.checkout.payment.gateway.acquirer.model.BankPaymentResponse;

public interface AcquiringBankClient {

  BankPaymentResponse processPayment(BankPaymentRequest request);
}
