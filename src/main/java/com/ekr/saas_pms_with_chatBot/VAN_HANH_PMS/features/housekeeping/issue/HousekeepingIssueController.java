package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.issue;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/housekeeping")
public class HousekeepingIssueController {

    private final HousekeepingIssueService issueService;

    public HousekeepingIssueController(
            HousekeepingIssueService issueService
    ) {
        this.issueService = issueService;
    }

    @GetMapping("/issues")
    public ResponseEntity<List<HousekeepingIssueResponse>> getIssues(
            @PathVariable Long propertyId
    ) {

        return ResponseEntity.ok(
                issueService.getIssues(propertyId)
        );
    }

    @PostMapping("/rooms/{roomId}/issues")
    public ResponseEntity<HousekeepingIssueResponse> createIssue(
            @PathVariable Long propertyId,
            @PathVariable Long roomId,
            @Valid @RequestBody CreateHousekeepingIssueRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(issueService.createIssue(
                        propertyId,
                        roomId,
                        request
                ));
    }

    @PatchMapping("/issues/{issueId}/resolution")
    public ResponseEntity<HousekeepingIssueResponse> updateResolution(
            @PathVariable Long propertyId,
            @PathVariable Long issueId,
            @Valid @RequestBody
            UpdateHousekeepingIssueResolutionRequest request
    ) {

        return ResponseEntity.ok(
                issueService.updateResolution(
                        propertyId,
                        issueId,
                        request
                )
        );
    }
}