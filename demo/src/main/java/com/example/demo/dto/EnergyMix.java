package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class EnergyMix {
    private String date;
    private Map<String, Double> averageShares;
    private double cleanPercentage;
}