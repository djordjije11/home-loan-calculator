package io.github.djordjije11.homeloancalculator.infra.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@WebAppConfiguration
class HomeLoanCalculationControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void calculateMonthlyPayment() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/monthly-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 100000, "annualInterestRate": 1.5, "repaymentPeriod": 120}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyPaymentAmount").value(897.915));
    }

    @Test
    void calculateRemainingPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/remaining-principal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 100000, "annualInterestRate": 1.5, "repaymentPeriod": 120, "elapsedRepaymentPeriod": 72}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principalAmount").value(41807.05088));
    }

    @Test
    void calculatePaidInterest() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/paid-interest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 100000, "annualInterestRate": 1.5, "repaymentPeriod": 120, "elapsedRepaymentPeriod": 72}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestAmount").value(6456.93073));
    }

    @Test
    void calculateRepaymentPeriod() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/repayment-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 50000, "annualInterestRate": 1.5, "monthlyPaymentAmount": 897.915}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repaymentPeriod").value(58));
    }

    @Test
    void calculatePartialRepayment() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/partial-repayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 70000, "annualInterestRate": 1.5, "repaymentPeriod": 84, "additionalPaymentAmount": 20000, "monthlyPaymentAmount": 1019.54}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principalAmount").value(50000.00000))
                .andExpect(jsonPath("$.reduceMonthlyPayment.monthlyPaymentAmount").isNumber())
                .andExpect(jsonPath("$.reduceRepaymentPeriod.repaymentPeriod").isNumber());
    }

    @Test
    void calculateMonthlyPayment_whenPrincipalAmountIsMissing_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/monthly-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"annualInterestRate": 1.5, "repaymentPeriod": 120}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateRepaymentPeriod_whenMonthlyPaymentDoesNotAmortisePrincipal_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/home-loan-calculations/repayment-period")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principalAmount": 100000, "annualInterestRate": 1.5, "monthlyPaymentAmount": 125}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/home-loan-calculations/monthly-payment']").exists());
    }
}
