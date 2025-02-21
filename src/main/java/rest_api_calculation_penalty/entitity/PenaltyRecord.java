package rest_api_calculation_penalty.entitity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenaltyRecord {
    // Идентификатор лицевого счета
    private String accountId;
    // Начало и конец периода расчёта пени для данной строки
    private String periodStart;
    private String periodEnd;
    // Исходная сумма задолженности для периода
    private double debtAmount;
    // Количество дней, за которые начисляется пеня
    private int days;
    // Применённая ставка (например, 0.0033333)
    private double rate;
    // Рассчитанная сумма пени
    private double penaltySum;
    // Дополнительно можно вернуть информацию по оплате (если применимо)
    private String registrar;
    private double paymentAmount;
    private String paymentDate;





}
