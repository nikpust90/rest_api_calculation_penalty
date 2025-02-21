package rest_api_calculation_penalty.entitity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitialBalances {
    private String accountId;
    private double initialDebt;
}
