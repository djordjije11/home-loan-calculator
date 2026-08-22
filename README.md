# Home Loan Calculator

Spring Boot application for calculating home loan payments.

The API provides illustrative calculations for fixed-rate home loans with equal monthly annuity payments. All monetary amounts are expressed in EUR by convention. Results may differ from your bank's repayment plan.

## Requirements

* JDK 25

## Running

You can start the application with:

```bash
./gradlew bootRun
```

or by running the `HomeLoanCalculatorApplication` main class from your IDE.

## API documentation

After starting the application, interactive API documentation is available at:

* http://localhost:8080/swagger-ui/index.html

The OpenAPI JSON document is available at:

* http://localhost:8080/v3/api-docs

## API usage

All calculation endpoints accept `POST` requests under `/api/v1/home-loan-calculations`:

* `/monthly-payment`
* `/remaining-principal`
* `/paid-interest`
* `/repayment-period`
* `/partial-repayment`

For example, calculate the monthly payment for a 100,000 EUR loan at 4.5% annual interest over 120 months:

```bash
curl --request POST http://localhost:8080/api/v1/home-loan-calculations/monthly-payment \
  --header 'Content-Type: application/json' \
  --data '{
    "principalAmount": 100000,
    "annualInterestRatePercentage": 4.5,
    "repaymentPeriod": 120
  }'
```

The annual interest rate is supplied as a percentage. The application uses a proportional monthly rate: annual interest rate divided by 12.

For the complete API contract, validation rules, and calculation assumptions, see [the home-loan calculation API specification](specs/home-loan-calculation/001-home-loan-calculation-api.md).
