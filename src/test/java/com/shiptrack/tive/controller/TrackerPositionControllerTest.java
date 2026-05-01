package com.shiptrack.tive.controller;

import com.shiptrack.tive.config.TiveWebhookProperties;
import com.shiptrack.tive.model.TrackerPositionState;
import com.shiptrack.tive.service.TrackerPositionStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackerPositionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TrackerPositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrackerPositionStateService trackerPositionStateService;

    @MockBean
    private TiveWebhookProperties tiveWebhookProperties;

    @Test
    void shouldReturnLatestPositionWhenFound() throws Exception {
        TrackerPositionState state = TrackerPositionState.builder()
                .trackerId("TRACKER-001")
                .latitude(-23.5505)
                .longitude(-46.6333)
                .entryTimeEpoch(1712345678000L)
                .entryTimeUtc("2024-04-05T20:00:00Z")
                .projectedAtEpoch(1712345679000L)
                .build();

        when(trackerPositionStateService.findCurrent("TRACKER-001")).thenReturn(Optional.of(state));

        mockMvc.perform(get("/trackers/TRACKER-001/position").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackerId").value("TRACKER-001"))
                .andExpect(jsonPath("$.latitude").value(-23.5505))
                .andExpect(jsonPath("$.longitude").value(-46.6333));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(trackerPositionStateService.findCurrent("TRACKER-999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/trackers/TRACKER-999/position").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }
}



