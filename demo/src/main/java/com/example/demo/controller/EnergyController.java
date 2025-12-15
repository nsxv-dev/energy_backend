package com.example.demo.controller;

import com.example.demo.dto.ChargingWindow;
import com.example.demo.dto.EnergyMix;
import com.example.demo.service.EnergyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EnergyController {

    private final EnergyService energyService;

    @GetMapping("/energy-mix")
    public ResponseEntity<List<EnergyMix>> getEnergyMix() {
        try {
            return energyService.getDataForNextDays(3);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/charge-window")
    @Validated
    public ResponseEntity<ChargingWindow> getChargingWindow(@RequestParam("hoursOfCharge") @Min(1) @Max(6) int hoursOfCharge) {
        try {
            return energyService.getChargingWindow(hoursOfCharge);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}