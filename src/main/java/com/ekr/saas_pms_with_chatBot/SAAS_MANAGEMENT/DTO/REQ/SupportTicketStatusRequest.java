
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SupportTicket;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketStatusRequest {

    @NotNull(message = "Status không được để trống")
    private SupportTicket.Status status;
}
