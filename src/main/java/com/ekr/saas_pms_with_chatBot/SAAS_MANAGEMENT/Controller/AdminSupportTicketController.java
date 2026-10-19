
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.SupportTicketStatusRequest;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.SupportTicketAdminResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SupportTicket;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.SupportTicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saas/admin/support-tickets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportTicketController {

    private final SupportTicketService supportTicketService;

    public AdminSupportTicketController(
            SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @GetMapping
    public List<SupportTicketAdminResponse> getAllTickets() {
        return supportTicketService.getAllTickets().stream()
                .map(SupportTicketAdminResponse::from)
                .toList();
    }

    @GetMapping("/{ticketId}")
    public SupportTicketAdminResponse getTicketDetail(
            @PathVariable Long ticketId) {
        try {
            return SupportTicketAdminResponse.from(
                    supportTicketService.getTicketById(ticketId));
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PatchMapping("/{ticketId}/status")
    public SupportTicketAdminResponse updateTicketStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody SupportTicketStatusRequest request) {
        try {
            SupportTicket updated =
                    supportTicketService.updateTicketStatus(
                            ticketId, request.getStatus());

            return SupportTicketAdminResponse.from(updated);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}