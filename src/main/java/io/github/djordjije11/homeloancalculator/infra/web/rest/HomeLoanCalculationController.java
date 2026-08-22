package io.github.djordjije11.homeloancalculator.infra.web.rest;

import io.github.djordjije11.homeloancalculator.application.HomeLoanCalculationService;
import io.github.djordjije11.homeloancalculator.domain.PartialRepaymentCalculation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home-loan-calculations")
@Tag(name = "Home loan calculations", description = "Illustrative fixed-rate monthly-annuity home-loan calculations. Results may differ from your bank's repayment plan.")
public class HomeLoanCalculationController {

    private final HomeLoanCalculationService homeLoanCalculationService;

    public HomeLoanCalculationController(HomeLoanCalculationService homeLoanCalculationService) {
        this.homeLoanCalculationService = homeLoanCalculationService;
    }

    @PostMapping("/monthly-payment")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Calculate a monthly payment", description = "Uses a proportional monthly interest rate derived from the annual percentage interest rate.")
    public MonthlyPaymentResponse calculateMonthlyPayment(@Valid @RequestBody MonthlyPaymentRequest request) {
        return new MonthlyPaymentResponse(roundAmount(homeLoanCalculationService.calculateMonthlyPayment(
                request.principalAmount(), request.annualInterestRate(), request.repaymentPeriod())));
    }

    @PostMapping("/remaining-principal")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Calculate remaining principal", description = "Calculates principal after the specified number of completed monthly payments.")
    public PrincipalResponse calculateRemainingPrincipal(@Valid @RequestBody ElapsedRepaymentRequest request) {
        return new PrincipalResponse(roundAmount(homeLoanCalculationService.calculateRemainingPrincipal(
                request.principalAmount(), request.annualInterestRate(), request.repaymentPeriod(), request.elapsedRepaymentPeriod())));
    }

    @PostMapping("/paid-interest")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Calculate paid interest", description = "Calculates cumulative regular interest through the specified number of completed monthly payments.")
    public InterestResponse calculatePaidInterest(@Valid @RequestBody ElapsedRepaymentRequest request) {
        return new InterestResponse(roundAmount(homeLoanCalculationService.calculatePaidInterest(
                request.principalAmount(), request.annualInterestRate(), request.repaymentPeriod(), request.elapsedRepaymentPeriod())));
    }

    @PostMapping("/repayment-period")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Calculate a repayment period", description = "Returns the required whole number of months, rounded up.")
    public RepaymentPeriodResponse calculateRepaymentPeriod(@Valid @RequestBody RepaymentPeriodRequest request) {
        return new RepaymentPeriodResponse(homeLoanCalculationService.calculateRepaymentPeriod(
                request.principalAmount(), request.annualInterestRate(), request.monthlyPaymentAmount()));
    }

    @PostMapping("/partial-repayment")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("permitAll()")
    @Operation(summary = "Calculate partial repayment options", description = "Applies an immediate additional payment to principal and returns lower-payment and shorter-period alternatives.")
    public PartialRepaymentResponse calculatePartialRepayment(@Valid @RequestBody PartialRepaymentRequest request) {
        PartialRepaymentCalculation calculation = homeLoanCalculationService.calculatePartialRepayment(
                request.principalAmount(),
                request.annualInterestRate(),
                request.repaymentPeriod(),
                request.additionalPaymentAmount(),
                request.monthlyPaymentAmount());
        return new PartialRepaymentResponse(
                roundAmount(calculation.principalAmount()),
                new ReduceMonthlyPaymentResponse(roundAmount(calculation.monthlyPaymentAmount())),
                new ReduceRepaymentPeriodResponse(calculation.repaymentPeriod()));
    }

    private BigDecimal roundAmount(BigDecimal amount) {
        return amount.setScale(5, RoundingMode.HALF_UP);
    }
}
