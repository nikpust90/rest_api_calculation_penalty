package rest_api_calculation_penalty.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rest_api_calculation_penalty.entitity.PenaltyCalculationRequest;
import rest_api_calculation_penalty.entitity.PenaltyRecord;
import rest_api_calculation_penalty.service.PenaltyCalculatorService;

import java.util.List;

// Контроллер, который принимает запрос от 1С
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class PenaltyCalculatorController {

    private final PenaltyCalculatorService penaltyCalculatorService;

    // HTTP POST запрос с JSON-телом, содержащим все входные данные
    @PostMapping("/calculatePenalties")
    public ResponseEntity<List<PenaltyRecord>> calculatePenalties(
            @RequestBody PenaltyCalculationRequest request) {
        List<PenaltyRecord> result = penaltyCalculatorService.calculatePenalties(request);
        return ResponseEntity.ok(result);
    }
}

