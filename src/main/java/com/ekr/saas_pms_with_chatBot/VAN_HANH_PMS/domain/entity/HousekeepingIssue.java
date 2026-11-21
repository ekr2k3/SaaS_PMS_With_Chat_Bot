package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.domain.entity;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Room;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "housekeeping_issue",
        indexes = {
                @Index(
                        name = "idx_housekeeping_issue_room",
                        columnList = "room_id"
                ),
                @Index(
                        name = "idx_housekeeping_issue_created_at",
                        columnList = "created_at"
                )
        }
)
public class HousekeepingIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "housekeeping_issue_id")
    private Long housekeepingIssueId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "issue", nullable = false, columnDefinition = "TEXT")
    private String issue;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getHousekeepingIssueId() {
        return housekeepingIssueId;
    }

    public void setHousekeepingIssueId(Long housekeepingIssueId) {
        this.housekeepingIssueId = housekeepingIssueId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}