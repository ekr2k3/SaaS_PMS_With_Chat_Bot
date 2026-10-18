package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SUPPORT_TICKET")
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket {

    public enum Status {
        OPEN,
        RESOLVED,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_ticket_id")
    private Long supportTicketId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
