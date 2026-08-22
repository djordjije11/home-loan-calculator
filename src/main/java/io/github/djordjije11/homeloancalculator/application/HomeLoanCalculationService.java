package io.github.djordjije11.homeloancalculator.application;

import io.github.djordjije11.homeloancalculator.domain.HomeLoanCalculator;
import io.github.djordjije11.homeloancalculator.domain.PartialRepaymentCalculation;
import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class HomeLoanCalculationService {

    private final HomeLoanCalculator homeLoanCalculator;

    public BigDecimal calculateMonthlyPayment(BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod) {
        return homeLoanCalculator.calculateMonthlyPayment(principalAmount, annualInterestRatePercentage, repaymentPeriod);
    }

    public BigDecimal calculateRemainingPrincipal(
            BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod, int elapsedRepaymentPeriod) {
        return homeLoanCalculator.calculateRemainingPrincipal(
                principalAmount, annualInterestRatePercentage, repaymentPeriod, elapsedRepaymentPeriod);
    }

    public BigDecimal calculatePaidInterest(
            BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, int repaymentPeriod, int elapsedRepaymentPeriod) {
        return homeLoanCalculator.calculatePaidInterest(
                principalAmount, annualInterestRatePercentage, repaymentPeriod, elapsedRepaymentPeriod);
    }

    public int calculateRepaymentPeriod(BigDecimal principalAmount, BigDecimal annualInterestRatePercentage, BigDecimal monthlyPaymentAmount) {
        return homeLoanCalculator.calculateRepaymentPeriod(principalAmount, annualInterestRatePercentage, monthlyPaymentAmount);
    }

    public PartialRepaymentCalculation calculatePartialRepayment(
            BigDecimal principalAmount,
            BigDecimal annualInterestRatePercentage,
            int repaymentPeriod,
            BigDecimal additionalPaymentAmount,
            BigDecimal monthlyPaymentAmount) {
        return homeLoanCalculator.calculatePartialRepayment(
                principalAmount, annualInterestRatePercentage, repaymentPeriod, additionalPaymentAmount, monthlyPaymentAmount);
    }
}
