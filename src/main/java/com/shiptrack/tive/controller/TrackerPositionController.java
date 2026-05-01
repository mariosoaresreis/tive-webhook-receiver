package com.shiptrack.tive.controller;

import com.shiptrack.tive.model.TrackerPositionState;
import com.shiptrack.tive.service.TrackerPositionStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trackers")
@RequiredArgsConstructor
public class TrackerPositionController {

    private final TrackerPositionStateService trackerPositionStateService;

    @GetMapping("/{trackerId}/position")
    public ResponseEntity<TrackerPositionState> getCurrentPosition(@PathVariable String trackerId) {
        return trackerPositionStateService.findCurrent(trackerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
