
package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SupportTicket;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SupportTicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public SupportTicket createTicket(Long accountId, String question) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Account not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setContent(question);
        ticket.setStatus(SupportTicket.Status.OPEN);
        ticket.setAccount(account);

        return supportTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository
                .findAllByOrderBySupportTicketIdDesc();
    }

    @Transactional(readOnly = true)
    public SupportTicket getTicketById(Long ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Support ticket not found: " + ticketId));
    }

    @Transactional
    public SupportTicket updateTicketStatus(
            Long ticketId, SupportTicket.Status status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status không được để trống");
        }

        if (status == SupportTicket.Status.OPEN) {
            throw new IllegalArgumentException(
                    "SaaS Admin chỉ được đánh dấu ticket là "
                            + "RESOLVED hoặc CLOSED");
        }

        SupportTicket ticket = getTicketById(ticketId);
        ticket.setStatus(status);

        return supportTicketRepository.save(ticket);
    }
}
