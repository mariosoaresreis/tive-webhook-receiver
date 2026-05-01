package com.shiptrack.tive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Materialized latest position for a tracker.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackerPositionState {
    private String trackerId;
    private Double latitude;
    private Double longitude;
    private Long entryTimeEpoch;
    private String entryTimeUtc;
    private Long projectedAtEpoch;
}
