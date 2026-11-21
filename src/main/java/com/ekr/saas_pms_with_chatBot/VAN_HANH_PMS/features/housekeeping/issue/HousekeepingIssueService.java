package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.issue;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoomRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.UserRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity.HousekeepingIssue;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.repository.HousekeepingIssueRepository;
import com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.housekeeping.HousekeepingAccessService;
import com.ekr.saas_pms_with_chatBot.filter.PmsEmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HousekeepingIssueService {

    private final HousekeepingIssueRepository issueRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final HousekeepingAccessService accessService;

    public HousekeepingIssueService(
            HousekeepingIssueRepository issueRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            HousekeepingAccessService accessService
    ) {
        this.issueRepository = issueRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<HousekeepingIssueResponse> getIssues(
            Long propertyId
    ) {

        accessService.requirePropertyAccess(propertyId);

        return issueRepository
                .findAllByRoom_RoomType_Property_PropertyIdOrderByCreatedAtDesc(
                        propertyId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HousekeepingIssueResponse createIssue(
            Long propertyId,
            Long roomId,
            CreateHousekeepingIssueRequest request
    ) {

        PmsEmployeePrincipal employee =
                accessService.requireHousekeeper();

        accessService.requirePropertyAccess(propertyId);

        Room room = roomRepository
                .findByRoomIdAndRoomType_Property_PropertyId(
                        roomId,
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found in this Property"
                ));

        User creator = userRepository
                .findByUserIdAndProperty_PropertyId(
                        employee.userId(),
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Employee account not found in this Property"
                ));

        HousekeepingIssue issue = new HousekeepingIssue();

        issue.setRoom(room);
        issue.setCreatedBy(creator);
        issue.setIssue(request.issue().trim());

        HousekeepingIssue savedIssue =
                issueRepository.save(issue);

        return toResponse(savedIssue);
    }

    @Transactional
    public HousekeepingIssueResponse updateResolution(
            Long propertyId,
            Long issueId,
            UpdateHousekeepingIssueResolutionRequest request
    ) {

        accessService.requirePropertyAccess(propertyId);

        HousekeepingIssue issue = issueRepository
                .findByHousekeepingIssueIdAndRoom_RoomType_Property_PropertyId(
                        issueId,
                        propertyId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Housekeeping issue not found"
                ));

        issue.setResolution(request.resolution().trim());

        return toResponse(issue);
    }

    private HousekeepingIssueResponse toResponse(
            HousekeepingIssue issue
    ) {

        Room room = issue.getRoom();
        User creator = issue.getCreatedBy();

        return new HousekeepingIssueResponse(
                issue.getHousekeepingIssueId(),
                room.getRoomId(),
                room.getRoomNumber(),
                issue.getIssue(),
                issue.getResolution(),
                creator.getUserId(),
                creator.getFullName(),
                issue.getCreatedAt()
        );
    }
}