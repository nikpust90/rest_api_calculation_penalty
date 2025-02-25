package rest_api_calculation_penalty.controllers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rest_api_calculation_penalty.entitity.PenaltyCalculationRequest;
import rest_api_calculation_penalty.entitity.PenaltyRecord;
import rest_api_calculation_penalty.service.PenaltyCalculatorService;

import java.util.List;

// Контроллер, который принимает запрос от 1С


import java.util.List;

@Slf4j // <-- Добавь это
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class PenaltyCalculatorController {

    private final PenaltyCalculatorService penaltyCalculatorService;

    @PostMapping("/calculatePenalties")
    public ResponseEntity<List<PenaltyRecord>> calculatePenalties(
            @RequestBody PenaltyCalculationRequest request) {
        log.info("Received request: {}", request); // Лог входного JSON

        List<PenaltyRecord> result;
        try {
            result = penaltyCalculatorService.calculatePenalties(request);
            log.info("Calculation successful. Returning {} records.", result.size());
        } catch (Exception e) {
            log.error("Error during penalty calculation", e);
            return ResponseEntity.status(500).body(null); // Ошибка сервера
        }

        return ResponseEntity.ok(result);
    }

    //Этот контроллер принимает JSON-запрос с объектом { "name": "пример", "value": 123 }
    // и возвращает JSON-ответ { "message": "Получено: пример, 123" }.
    @PostMapping("/data")
    public ResponseData receiveData(@RequestBody RequestData requestData) {
        // Обрабатываем полученные данные
        String message = "Получено: " + requestData.getName() + ", " + requestData.getValue();
        return new ResponseData(message);
    }

    // Вспомогательный класс для приема JSON
    @Setter
    @Getter
    public static class RequestData {
        private String name;
        private int value;

    }

    // Вспомогательный класс для отправки JSON
    @Setter
    @Getter
    public static class ResponseData {
        private String message;

        public ResponseData(String message) { this.message = message; }

    }


}


