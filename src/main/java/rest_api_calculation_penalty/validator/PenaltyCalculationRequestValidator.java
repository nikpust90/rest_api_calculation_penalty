package rest_api_calculation_penalty.validator;


import rest_api_calculation_penalty.entitity.PenaltyCalculationRequest;
import rest_api_calculation_penalty.entitity.Account;

import java.util.Objects;

public class PenaltyCalculationRequestValidator {

    public static void validate(PenaltyCalculationRequest request) {
        // Используем Objects.requireNonNull для проверки на null
        Objects.requireNonNull(request, "PenaltyCalculationRequest не должен быть null");
        Objects.requireNonNull(request.getPeriodStart(), "Дата начала периода не должна быть null");
        Objects.requireNonNull(request.getPeriodEnd(), "Дата окончания периода не должна быть null");

        if (request.getAccounts() == null || request.getAccounts().isEmpty()) {
            throw new IllegalArgumentException("Список счетов не должен быть null или пуст");
        }

        // Проверка каждого счета (с использованием var из Java 10+)
        for (var account : request.getAccounts()) {
            Objects.requireNonNull(account, "Каждый счет должен быть инициализирован");
            Objects.requireNonNull(account.getId(), "Идентификатор счета не должен быть null");
        }
    }
}