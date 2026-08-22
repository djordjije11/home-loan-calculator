package io.github.djordjije11.homeloancalculator.application;

import io.github.djordjije11.homeloancalculator.domain.HomeLoanCalculator;
import io.github.djordjije11.homeloancalculator.domain.PartialRepaymentCalculation;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class HomeLoanCalculationService {

    private final HomeLoanCalculator homeLoanCalculator = new HomeLoanCalculator();

    public BigDecimal calculateMonthlyPayment(BigDecimal principalAmount, BigDecimal annualInterestRate, int repaymentPeriod) {
        return homeLoanCalculator.calculateMonthlyPayment(principalAmount, annualInterestRate, repaymentPeriod);
    }

    public BigDecimal calculateRemainingPrincipal(
            BigDecimal principalAmount, BigDecimal annualInterestRate, int repaymentPeriod, int elapsedRepaymentPeriod) {
        return homeLoanCalculator.calculateRemainingPrincipal(
                principalAmount, annualInterestRate, repaymentPeriod, elapsedRepaymentPeriod);
    }

    public BigDecimal calculatePaidInterest(
            BigDecimal principalAmount, BigDecimal annualInterestRate, int repaymentPeriod, int elapsedRepaymentPeriod) {
        return homeLoanCalculator.calculatePaidInterest(
                principalAmount, annualInterestRate, repaymentPeriod, elapsedRepaymentPeriod);
    }

    public int calculateRepaymentPeriod(BigDecimal principalAmount, BigDecimal annualInterestRate, BigDecimal monthlyPaymentAmount) {
        return homeLoanCalculator.calculateRepaymentPeriod(principalAmount, annualInterestRate, monthlyPaymentAmount);
    }

    public PartialRepaymentCalculation calculatePartialRepayment(
            BigDecimal principalAmount,
            BigDecimal annualInterestRate,
            int repaymentPeriod,
            BigDecimal additionalPaymentAmount,
            BigDecimal monthlyPaymentAmount) {
        return homeLoanCalculator.calculatePartialRepayment(
                principalAmount, annualInterestRate, repaymentPeriod, additionalPaymentAmount, monthlyPaymentAmount);
    }
}
