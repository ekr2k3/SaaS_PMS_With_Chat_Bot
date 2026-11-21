package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository;

import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.HousekeepingIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HousekeepingIssueRepository
        extends JpaRepository<HousekeepingIssue, Long> {

    List<HousekeepingIssue>
    findAllByRoom_RoomType_Property_PropertyIdOrderByCreatedAtDesc(
            Long propertyId
    );

    Optional<HousekeepingIssue>
    findByHousekeepingIssueIdAndRoom_RoomType_Property_PropertyId(
            Long issueId,
            Long propertyId
    );
}