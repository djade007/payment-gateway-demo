package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@Tag(name = "Payment Gateway", description = "Card processing and payment retrieval operations")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping("/payments")
  @Operation(
      summary = "Process a card payment",
      description =
          "Process a card payment request by validating card details and communicating with an acquiring bank")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment processed successfully (Authorized or Declined)",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid payment request",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PostPaymentRequest request) {
    PaymentResponse response = paymentGatewayService.processPayment(request);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping("/payment/{id}")
  @Operation(
      summary = "Retrieve a payment details",
      description = "Retrieve details of a previously processed payment using an ID")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Payment found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PaymentResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Payment not found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
      })
  public ResponseEntity<PaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }
}
