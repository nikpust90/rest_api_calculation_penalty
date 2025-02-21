package rest_api_calculation_penalty.entitity.payment;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private String accountId;
    private String registrar;
    // Дата оплаты в формате "yyyy-MM-dd"
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate paymentDate;

    private double paymentAmount;

    // Флаг, является ли оплата авансом (при необходимости)
    private boolean isAdvance;
}
