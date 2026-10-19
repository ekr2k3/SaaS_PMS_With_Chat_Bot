
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SupportTicket;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupportTicketAdminResponse {

    private Long supportTicketId;
    private String content;
    private SupportTicket.Status status;
    private Long accountId;
    private String accountName;
    private String accountEmail;

    public static SupportTicketAdminResponse from(
            SupportTicket ticket) {
        return new SupportTicketAdminResponse(
                ticket.getSupportTicketId(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getAccount().getAccountId(),
                ticket.getAccount().getName(),
                ticket.getAccount().getEmail()
        );
    }
}