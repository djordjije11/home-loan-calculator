# Home Loan Calculation API

## Problem Statement

People evaluating or managing a home loan need reliable, small calculations: the monthly payment, the principal remaining after a period, interest already paid, the repayment period implied by a payment, and the consequences of an additional principal payment. These values are often available only through bank-specific calculators or by manually applying amortisation formulas.

The application currently has no API that exposes these calculations as a consistent, documented service. It needs a stateless public API that uses one clear amortisation model and accepts the current state of a loan, rather than requiring a user's original loan history.

## Solution

Provide a public, stateless Spring Boot API for fixed-rate home-loan calculations. Each request represents a calculation baseline: `principalAmount` and `repaymentPeriod` mean the principal and period at the beginning of that request's calculation, not necessarily the originally approved loan.

The API uses equal monthly annuity payments made at the end of each month. It treats the supplied annual interest rate as a nominal annual percentage and calculates the monthly rate proportionally by dividing it by twelve. A separate endpoint models an immediate, partial additional payment against the current principal and returns both supported choices: reduce the monthly payment or reduce the repayment period.

All monetary amounts are EUR by convention and no currency field is exchanged. Results are illustrative and may differ from a bank's repayment plan.

## User Stories

1. As a home-loan applicant, I want to calculate a monthly payment from a principal amount, annual interest rate, and repayment period, so that I can assess affordability.
2. As a home-loan applicant, I want the monthly-payment calculation to accept the current principal and remaining period, so that I can recalculate a loan after a rate change or another change in circumstances.
3. As a home-loan holder, I want to calculate the principal remaining after a completed repayment period, so that I know my current debt.
4. As a home-loan holder, I want to calculate remaining principal from a calculation baseline rather than my complete historic loan record, so that prior rate changes or additional payments do not prevent a useful calculation.
5. As a home-loan holder, I want to calculate interest paid through a completed repayment period, so that I can distinguish interest cost from principal repayment.
6. As a home-loan holder, I want to calculate the repayment period implied by my principal, interest rate, and monthly payment, so that I can estimate how long repayment will take.
7. As a home-loan holder, I want a repayment-period result to be expressed as a whole number of months rounded up, so that it represents the number of monthly payment periods required to repay the loan.
8. As a home-loan holder, I want to model an immediate additional payment against my current principal, so that I can understand the effects of partial early repayment.
9. As a home-loan holder, I want to see the lower monthly payment available when I keep the same remaining repayment period after an additional payment, so that I can choose lower monthly obligations.
10. As a home-loan holder, I want to see the shorter repayment period available when I keep my current monthly payment after an additional payment, so that I can choose faster repayment.
11. As an API consumer, I want an additional-payment response to include the new principal and clearly separate the two repayment choices, so that I can present both alternatives without mixing their outcomes.
12. As an API consumer, I want consistent English field names and documentation, so that requests are unambiguous.
13. As an API consumer, I want all amounts to use EUR by convention, so that I do not need to provide or interpret a currency field.
14. As an API consumer, I want monetary results to retain up to five decimal places, so that calculations are more useful than cent-only presentation values.
15. As an API consumer, I want invalid numerical inputs to receive a simple HTTP 400 response with a field-level English message, so that I can correct the request.
16. As an API consumer, I want interactive OpenAPI documentation with examples and calculation assumptions, so that I can integrate without reading source code.
17. As an API consumer, I want the calculation endpoints to be publicly callable without authentication, so that a simple calculator client can use them without account management.
18. As a maintainer, I want calculation rules isolated in a small domain module, so that formulas and boundary conditions can be tested independently of HTTP handling.
19. As a maintainer, I want no persistence required for calculations, so that the service remains simple and each request is independent.

## Implementation Decisions

- Create a home-loan-calculation module with a deep domain calculator interface that encapsulates the amortisation formulas and their validation-relevant preconditions. Application services orchestrate calls to that domain calculator; REST adapters only map request and response DTOs.
- Expose five public `POST` endpoints under `/api/v1/home-loan-calculations`:
  - `/monthly-payment`
  - `/remaining-principal`
  - `/paid-interest`
  - `/repayment-period`
  - `/partial-repayment`
- Use `@PreAuthorize("permitAll()")` for these public controller methods, with the required Spring Security configuration permitting the endpoints. No users, roles, API keys, or persisted sessions are introduced.
- Add OpenAPI/Swagger support. All endpoint and DTO documentation is in English and includes units, validation constraints, examples, assumptions, and the short disclaimer: “Results are illustrative and may differ from your bank's repayment plan.”
- Use the error-handling library `io.github.wimdeblauwe:error-handling-spring-boot-starter:5.1.1` for simple standardised error responses. Invalid requests return HTTP 400 with basic English field-level messages; no detailed error domain is required.
- Use JSON numbers for all numeric request and response values. Represent monetary amounts and interest rates with `BigDecimal` in Java; never use `double` or `float`.
- All monetary amounts are EUR by application convention. Do not include a currency request field or a currency response field.
- `annualInterestRatePercentage` is a positive nominal annual rate represented as a percentage. For example, `4.5` means 4.5% per year. The formula derives `monthlyInterestRate` as `annualInterestRatePercentage / 12 / 100`. Interest rates equal to zero or below are invalid in v1.
- The repayment model is an equal-monthly-annuity loan, with a payment at the end of every month. Each individual request uses a single, fixed annual interest rate; variable rates, Euribor schedules, rate resets, day-count conventions, and grace periods are excluded.
- A calculation baseline is defined by the request. `principalAmount` means the principal at the start of that calculation. `repaymentPeriod` means the number of months from that same point. This lets a client use a current balance and remaining period after prior loan changes.
- The monthly-payment calculation accepts `principalAmount`, `annualInterestRatePercentage`, and `repaymentPeriod`; it returns `monthlyPaymentAmount`.
- The remaining-principal calculation accepts `principalAmount`, `annualInterestRatePercentage`, `repaymentPeriod`, and `elapsedRepaymentPeriod`; it calculates the annuity payment internally and returns the principal remaining after the elapsed number of payments.
- The paid-interest calculation accepts the same inputs as remaining-principal and returns the cumulative regular interest paid through `elapsedRepaymentPeriod`. It calculates the annuity payment internally rather than accepting a potentially inconsistent payment amount.
- The repayment-period calculation accepts `principalAmount`, `annualInterestRatePercentage`, and `monthlyPaymentAmount`; it returns `repaymentPeriod`. The formula may produce a fractional number of months, but the API returns the result rounded up to a positive whole month.
- The partial-repayment calculation accepts `principalAmount`, `annualInterestRatePercentage`, `repaymentPeriod`, `additionalPaymentAmount`, and `monthlyPaymentAmount`. These values represent the current state of the loan. `additionalPaymentAmount` is immediately subtracted from principal before the next scheduled payment.
- The partial-repayment response returns the new `principalAmount` plus two explicitly separated scenarios: `reduceMonthlyPayment` contains the recalculated `monthlyPaymentAmount` for the unchanged repayment period, and `reduceRepaymentPeriod` contains the recalculated `repaymentPeriod` for the unchanged monthly payment.
- `repaymentPeriod` and `elapsedRepaymentPeriod` are whole numbers expressed in months. `elapsedRepaymentPeriod` is in the inclusive range from zero to `repaymentPeriod`; zero represents the baseline before any payment and the full repayment period represents loan completion.
- Monetary request values are required, positive, and capped at `9999999` in accordance with repository conventions. `additionalPaymentAmount` is strictly smaller than `principalAmount` because complete early repayment is not part of v1.
- A supplied `monthlyPaymentAmount` must be greater than the first month's interest. Otherwise the principal cannot be amortised and the repayment-period and partial-repayment calculations reject the request.
- Keep higher internal `BigDecimal` precision while calculating. Monetary response values are rounded to at most five decimal places using `HALF_UP`. Repayment-period results are rounded upward to the next whole month.
- Add no database, repositories, entities, migrations, or stateful calculation history. Each endpoint is a pure request-response calculation.
- Do not enable CORS in v1. The API can be publicly called server-to-server; browser cross-origin access will be configured only when a concrete client origin is known.

## Testing Decisions

- Test observable calculation behaviour, validations, rounding, and HTTP contracts rather than private formula implementation details.
- Add domain unit tests for every formula: monthly payment, remaining principal, paid interest, repayment period, and both partial-repayment alternatives.
- Test normal examples, all accepted boundary cases, `HALF_UP` monetary rounding, and upward rounding of fractional repayment periods.
- Test `elapsedRepaymentPeriod` at zero, an intermediate value, and the full repayment period.
- Test additional payments against a current loan state, ensuring the returned principal equals the input principal minus the additional payment and that each scenario preserves the correct value: period for lower-payment and payment for shorter-period calculations.
- Test domain guards for missing values, non-positive amounts and rates, invalid periods, elapsed periods outside the allowed range, additional payment equal to or greater than principal, and payment amounts that cannot amortise the principal.
- Add Spring Boot controller integration tests for all five public endpoints, valid JSON requests, expected JSON responses, OpenAPI availability, and simple HTTP 400 validation responses from the configured error library.
- Use `@SpringBootTest(webEnvironment = NONE)` for non-web integration scenarios and Spring Boot controller integration tests for HTTP behaviour, following repository conventions.
- No database integration tests are needed because this slice has no persistence.

## Out of Scope

- Persistence of loans, calculations, users, or calculation history.
- Authentication, authorisation beyond public access, API keys, and user accounts.
- CORS configuration.
- Currency conversion, non-EUR currencies, exchange rates, and EUR/RSD indexing.
- Bank-specific contract reproduction, dates, actual-day calculations, 360/365/366 day-count conventions, or bank-specific rounding per instalment.
- Variable interest-rate schedules, Euribor lookup or changes, rate resets, and a credit-for-young-people subsidy model.
- Grace periods, equal-principal repayment plans, irregular payments, late payments, payment holidays, penalties, and default interest.
- Loan processing fees, insurance, account fees, taxes, notary costs, or effective-interest-rate calculations.
- Full repayment schedules, future total-interest calculations, full early repayment, and any calculator beyond the five defined endpoints.

## Assumptions and Open Questions

- There are no open questions that block implementation.
- Clients that need to represent a changed loan state will first obtain or otherwise know the current principal and remaining period, then use those values as the next request's calculation baseline.

## Further Notes

- The primary use case is an informative home-loan calculator, not a replacement for a lender's contractual repayment plan.
- The service remains intentionally small: its value is a stable, clearly documented API contract around a single consistent amortisation model.
