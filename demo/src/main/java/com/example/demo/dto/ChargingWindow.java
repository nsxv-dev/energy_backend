package com.example.demo.dto;

import lombok.Data;

@Data
public class ChargingWindow {
    private String startTime;
    private String endTime;
    private Double cleanEnergyPercentage;
}
