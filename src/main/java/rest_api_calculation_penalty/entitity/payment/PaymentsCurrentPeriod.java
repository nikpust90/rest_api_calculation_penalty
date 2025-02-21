package rest_api_calculation_penalty.entitity.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentsCurrentPeriod {

    private String registrar; //Регистратор
    private LocalDate paymentDate; //ДатаОплаты
    private double paymentAmount; //СуммаОплаты
    private boolean isAdvance; //ЭтоАванс
    private int daysDifference; //РазницаВДнях

}
