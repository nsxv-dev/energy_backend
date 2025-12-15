package com.example.demo.service;

import com.example.demo.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

class EnergyServiceTest {

    private EnergyService energyService;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class);
        energyService = spy(new EnergyService(webClient));
    }

    @Test
    void testGetDataForNextDays() {
        // Mock API response
        ExternalEnergyApiResponse mockResponse = new ExternalEnergyApiResponse();
        List<EnergyData> dataList = new ArrayList<>();

        // Prepare 3 EnergyData objects
        String[] fuels = {"hydro", "solar", "wind", "nuclear", "biomass", "other", "imports", "coal", "gas"};
        double[] percents = {10, 20, 15, 5, 10, 5, 5, 20, 10};

        for (int i = 0; i < 3; i++) {
            EnergyData data = new EnergyData();
            data.setFrom("2025-12-15T0" + i + ":00Z");
            data.setTo("2025-12-15T0" + (i + 1) + ":30Z");

            List<GenerationMix> mixes = new ArrayList<>();
            for (int j = 0; j < fuels.length; j++) {
                GenerationMix mix = new GenerationMix();
                mix.setFuel(fuels[j]);
                mix.setPerc(percents[j]);
                mixes.add(mix);
            }
            data.setGenerationmix(mixes);
            dataList.add(data);
        }

        mockResponse.setData(dataList);

        // Mock internal call
        doReturn(mockResponse).when(energyService).getDataForDay(any(LocalDate.class));

        ResponseEntity<List<EnergyMix>> response = energyService.getDataForNextDays(3);

        assertNotNull(response);
        assertEquals(3, response.getBody().size());

        EnergyMix firstDay = response.getBody().get(0);
        assertEquals("2025-12-15", firstDay.getDate());

        // Check averageShares for each fuel
        for (int j = 0; j < fuels.length; j++) {
            assertEquals(percents[j], firstDay.getAverageShares().get(fuels[j]));
        }

        // Check cleanPercentage (10+20+15+5+10 = 60)
        assertEquals(60.0, firstDay.getCleanPercentage());
    }

    @Test
    void testGetChargingWindow() {
        int hoursOfCharge = 2;

        // Prepare mock API response
        ExternalEnergyApiResponse mockResponse = new ExternalEnergyApiResponse();
        List<EnergyData> intervals = new ArrayList<>();

        String[] fuels = {"hydro", "solar", "wind", "nuclear", "biomass", "other", "imports", "coal", "gas"};
        double[] percents1 = {10, 20, 15, 5, 10, 5, 5, 20, 10};
        double[] percents2 = {15, 15, 20, 10, 10, 5, 5, 10, 10};

        // Create 4 intervals
        for (int i = 0; i < 4; i++) {
            EnergyData data = new EnergyData();
            data.setFrom("2025-12-15T0" + i + ":00Z");
            data.setTo("2025-12-15T0" + (i + 1) + ":30Z");

            List<GenerationMix> mixes = new ArrayList<>();
            double[] percents = i % 2 == 0 ? percents1 : percents2;
            for (int j = 0; j < fuels.length; j++) {
                GenerationMix mix = new GenerationMix();
                mix.setFuel(fuels[j]);
                mix.setPerc(percents[j]);
                mixes.add(mix);
            }
            data.setGenerationmix(mixes);
            intervals.add(data);
        }

        mockResponse.setData(intervals);

        // Mock internal call
        doReturn(mockResponse).when(energyService).getDataForDay(any(LocalDate.class));

        ResponseEntity<ChargingWindow> response = energyService.getChargingWindow(hoursOfCharge);

        assertNotNull(response);
        ChargingWindow window = response.getBody();
        assertNotNull(window.getStartTime());
        assertNotNull(window.getEndTime());

        // Calculate expected clean percentage for the first window
        double totalClean = 0;
        for (int i = 0; i < 4; i++) {
            double[] percents = i % 2 == 0 ? percents1 : percents2;
            totalClean += percents[0] + percents[1] + percents[2] + percents[3] + percents[4];
        }
        double expectedClean = Math.round((totalClean / 4) * 100.0) / 100.0;

        assertEquals(expectedClean, window.getCleanEnergyPercentage());
    }
}