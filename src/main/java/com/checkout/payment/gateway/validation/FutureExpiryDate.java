package com.checkout.payment.gateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureExpiryDateValidator.class)
public @interface FutureExpiryDate {
  String message() default "Expiry Date must be in the future";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
