package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionRenewalSchedulerTest {

    @Mock
    private SubscriptionRenewalService renewalService;

    @InjectMocks
    private SubscriptionRenewalScheduler scheduler;

    @Test
    void shouldCallRenewalService() {
        scheduler.runRenewalReminder();

        verify(renewalService, times(1))
                .sendRenewalReminders();
    }

    @Test
    void shouldNotPropagateUnexpectedServiceException() {
        doThrow(new RuntimeException("Unexpected error"))
                .when(renewalService)
                .sendRenewalReminders();

        // Scheduler tự bắt exception và ghi log.
        scheduler.runRenewalReminder();

        verify(renewalService, times(1))
                .sendRenewalReminders();
    }
}