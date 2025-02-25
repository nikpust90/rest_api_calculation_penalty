package rest_api_calculation_penalty.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rest_api_calculation_penalty.entitity.PenaltyCalculationRequest;
import rest_api_calculation_penalty.entitity.PenaltyRecord;
import rest_api_calculation_penalty.service.PenaltyCalculatorService;

import java.util.List;

// Контроллер, который принимает запрос от 1С
@Slf4j
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
}

