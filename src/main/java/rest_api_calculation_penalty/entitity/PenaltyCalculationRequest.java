package rest_api_calculation_penalty.entitity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rest_api_calculation_penalty.entitity.payment.Payment;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenaltyCalculationRequest {
    // Период начисления в формате "yyyy-MM-dd" (например, "2025-01-31")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate periodStart;
    // Период начисления в формате "yyyy-MM-dd" (например, "2025-01-31")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate periodEnd;
    // Список лицевых счетов
    private List<Account> accounts;
    // Начальные остатки задолженности по счетам
    private List<InitialBalances> initialBalances;
    // Оплаты по счетам
    private List<Payment> payments;
    //Начисления на месяц в целом за период
    private List<AccrualOfMonth> accrualOfMonths;
}
