package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class EnergyData {
    private String from;
    private String to;
    private List<GenerationMix> generationmix;
}