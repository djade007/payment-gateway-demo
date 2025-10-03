## How to Run

1. Start the bank simulator:
   ```bash
   docker-compose up -d
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```

The application will be available at `http://localhost:8090`

## API Documentation

For documentation openAPI is included, and it can be found under the following url: *
*http://localhost:8090/swagger-ui/index.html**

## Design Decisions

I focused on a few core principles while building this project: **security first**, **writing clean,
testable code**, and **planning for the future**.

### Validation

To ensure only valid data reaches the core logic, I set up a strong validation layer right at the
entry point.

* **Simple Rules**: For straightforward checks like making sure a field isn't empty (`@NotNull`) or
  a card number has the right format (`@Pattern`), I used standard Jakarta Bean Validation
  annotations.
* **Complex Rules**: For trickier logic, like ensuring a card's expiry date is in the future, I
  wrote a **custom validator** (`@FutureExpiryDate`). This allowed me to handle logic that involved
  multiple fields, keeping the validation rules neat and organized.
* **User-Friendly Errors**: If validation fails, the user gets a clear `400 Bad Request` error
  telling them exactly which field is wrong.

### Security Strategy

* I made sure that **full card numbers and CVVs are never, ever stored** in the database or logs.
* The only card detail persisted is the **last four digits**, which is safe to display to the user
  for reference.
* To prevent race conditions, I used a **ConcurrentHashMap** in the repository layer, ensuring
  thread-safe payment storage and retrieval.

### Immutable DTOs

I designed all data transfer objects (DTOs) to be **immutable** using Lombok's `@Value` annotation.
This means that once a request or response object is created, it cannot be changed.

* **It prevents bugs**: Accidental modifications of data are impossible.
* **It's thread-safe**: This makes the application more reliable, especially under load.
* **It keeps testing simple**: For tests, I enabled the `toBuilder()` method, which makes it easy to
  create variations of objects without sacrificing immutability.

### Integrating with the Bank Simulator

To make the system flexible, I coded to an **interface** (`AcquiringBankClient`) rather than
directly to the bank simulator implementation. This design pattern means we
could easily swap in a different bank provider in the future without having
to rewrite the main payment logic. The current implementation uses `RestTemplate` to communicate
with the bank simulator and error handling in case the bank service is down.

### My Comprehensive Testing Strategy

I followed a **Test-Driven Development (TDD)** approach and created:

* **Unit Tests**: To check small, isolated pieces of logic, like my custom validation rules.
* **Integration Tests**: To verify that my application communicates correctly with the bank
  simulator. I used `MockRestServiceServer` here to avoid making real network calls.
* **End-to-End Tests**: To simulate a full user request from the API endpoint all the way to the
  response, ensuring all the pieces work together perfectly. I used Spring Boot's testing tools and
  `MockMvc` for this.

---

## Assumptions I Made

1. **Storage**: An in-memory `ConcurrentHashMap` was sufficient for storing payment data for this exercise.
2. **Currency & Amount**: I limited support to GBP, EUR, and USD. To handle money safely and avoid
   common floating-point rounding errors, `int` is used for the currency amount. It is also assumed
   that the
   value will not bigger than `Integer.MAX_VALUE`
3. **No Authentication**: I assumed that merchant authentication was not needed for this version.
4. **No Idempotency**: I didn't implement idempotency keys to handle duplicate requests, as it was
   out of scope.

---

## Potential Improvements

* **Persistent Storage**: Move from the in-memory map to a proper database like PostgreSQL.
* **Authentication & Security**: Implement API keys for merchants to properly identify and authorize
  who are using the gateway.
* **Idempotency**: Add support for idempotency keys so that a merchant can safely retry a request
  without creating a duplicate payment.
* **Resiliency and API Protection**: Integrating a resilience library like Resilience4J to enhance
  API
  protection by using circuit breakers to prevent cascading failures and rate limiters to avoid
  overwhelming external services.
