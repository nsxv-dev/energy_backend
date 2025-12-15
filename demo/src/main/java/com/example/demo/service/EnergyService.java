package com.example.demo.service;

import com.example.demo.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnergyService {

    private final WebClient webClient;

    /**
     * @param numberOfDays
     * @return a list of average shares, clean percentage and date for each day
     */
    public ResponseEntity<List<EnergyMix>> getDataForNextDays(int numberOfDays) {
        List<EnergyMix> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Call API for data about the day number of days from now
        for (int i = 0; i < numberOfDays; i++) {
            LocalDate date = today.plusDays(i);
            ExternalEnergyApiResponse response = getDataForDay(date);

            if (response != null && response.getData() != null) {
                result.add(calculateAverage(date, response.getData()));
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
    * Calculates best charging wind for amount of hours in next two days (not next 48h)
    * FYI: API doesn't send enough data for next two days, it sends around 75 intervals instead of 96
    * I made calculations for two days starting from 00:00 tomorrow.
    */
    public ResponseEntity<ChargingWindow> getChargingWindow(int hoursOfCharge) {
        int intervalsNeeded = hoursOfCharge * 2;
        int numberOfDays = 2;
        List<EnergyData> energyDataTwoNextDays = new ArrayList<>();
        LocalDate today = LocalDate.now();
        ChargingWindow chargingWindow = new ChargingWindow();

        // Call API for data about the day number of days from now
        for (int i = 0; i < numberOfDays; i++) {
            //API response is for previous day, that's why there is +1
            LocalDate date = today.plusDays(i+1);
            ExternalEnergyApiResponse response = getDataForDay(date);

            if (response != null && response.getData() != null) {
                energyDataTwoNextDays.addAll(response.getData());
            }
        }
        if (energyDataTwoNextDays.size() < intervalsNeeded) {
            throw new IllegalStateException("Not enough energy data to calculate charging window");
        }

        // Set first period as max starting from 1st interval
        EnergyMix maxEnergyMix = calculateAverage(today,energyDataTwoNextDays.subList(0,intervalsNeeded-1));
        EnergyMix newEnergyMix;
        List<EnergyData> intervalsData;

        // Set first available period as window
        chargingWindow.setStartTime(energyDataTwoNextDays.subList(0,intervalsNeeded-1).getFirst().getFrom());
        chargingWindow.setEndTime(energyDataTwoNextDays.subList(0,intervalsNeeded-1).getLast().getTo());
        chargingWindow.setCleanEnergyPercentage(maxEnergyMix.getCleanPercentage());


        // Start from 2nd interval for new window
        for (int i = 1; i + intervalsNeeded <= energyDataTwoNextDays.size(); i++) {
            // New period to calculate
            intervalsData = energyDataTwoNextDays.subList(i,i+intervalsNeeded);
            newEnergyMix = calculateAverage(today,intervalsData);

            // When new cleanPercentage is higher, set is as new maxWindow
            if(newEnergyMix.getCleanPercentage() > maxEnergyMix.getCleanPercentage()){
                maxEnergyMix = newEnergyMix;

                //Set new best window
                chargingWindow.setStartTime(intervalsData.getFirst().getFrom());
                chargingWindow.setEndTime(intervalsData.getLast().getTo());
                chargingWindow.setCleanEnergyPercentage(maxEnergyMix.getCleanPercentage());
            }
        }

        return ResponseEntity.ok(chargingWindow);
    }

    ExternalEnergyApiResponse getDataForDay(LocalDate date){
        // API response is for previous day so we add 1 day to get today and 1 minute to get right first interval of day
        // starting from 00:00 not 23:30 of previous day
        String from = date.plusDays( 1)
                .atTime(0, 1)
                .toString() + "Z";

        return  webClient
                .get()
                .uri("/generation/" + from + "/pt24h")
                .retrieve()
                .bodyToMono(ExternalEnergyApiResponse.class)
                .block();
    }

    private EnergyMix calculateAverage(LocalDate date, List<EnergyData> periodData) {
        // Group values for each fuel type
        Map<String, List<Double>> fuelMap = groupFuelTypes(periodData);

        // Calculate average share for each fuel type
        Map<String, Double> averageShares = calculateAverageShares(fuelMap);

        // If the fuel is "clean" aggregate its share
        List<String> cleanFuels = List.of("hydro", "solar", "wind", "nuclear", "biomass");
        Double cleanPercentage = calculateCleanShare(averageShares, cleanFuels);

        return EnergyMix.builder()
                .date(date.toString())
                .averageShares(averageShares)
                .cleanPercentage(cleanPercentage)
                .build();
    }

    // Groups fuels' values in a map
    private Map<String, List<Double>> groupFuelTypes(List<EnergyData> data){
        Map<String, List<Double>> fuelMap = new HashMap<>();
        for (EnergyData entry : data) {
            for (GenerationMix mix : entry.getGenerationmix()) {
                fuelMap.computeIfAbsent(mix.getFuel(), key -> new ArrayList<>()).add(mix.getPerc());
            }
        }

        return fuelMap;
    }

    private Map<String, Double> calculateAverageShares(Map<String, List<Double>> fuelMap){
        Map<String, Double> averageShares = new HashMap<>();
        fuelMap.forEach((fuel, values) -> {
            double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            avg = Math.round(avg * 100.0) / 100.0;
            averageShares.put(fuel, avg);
        });

        return averageShares;
    }

    private  Double calculateCleanShare(Map<String, Double> averageShares, List<String> cleanFuels) {
        return averageShares.entrySet().stream()
                .filter(e -> cleanFuels.contains(e.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }
}