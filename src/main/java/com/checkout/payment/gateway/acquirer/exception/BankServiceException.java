package com.checkout.payment.gateway.acquirer.exception;

public class BankServiceException extends RuntimeException {
  public BankServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
