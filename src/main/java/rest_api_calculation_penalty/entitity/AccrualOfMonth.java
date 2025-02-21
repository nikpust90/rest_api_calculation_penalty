package rest_api_calculation_penalty.entitity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccrualOfMonth {
    private String accountId; //лицевой счет

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate beginMonth; // Начало месяца

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate endMonth; // Конец месяца

    private double amountAccrual; //СуммаНачисления
}
