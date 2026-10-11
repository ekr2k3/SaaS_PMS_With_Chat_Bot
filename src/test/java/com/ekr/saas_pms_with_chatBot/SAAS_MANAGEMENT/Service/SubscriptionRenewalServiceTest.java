package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Subscription;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionRenewalServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private SubscriptionRenewalService renewalService;

    @BeforeEach
    void setUp() {
        // Các mock được MockitoExtension khởi tạo.
    }

    @Test
    void shouldQueryTheNextCalendarMonth() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        when(subscriptionRepository.findRenewalCandidates(
                eq(Subscription.SubscriptionStatus.ACTIVE),
                eq(today),
                eq(LocalDate.of(2026, 11, 1)),
                eq(LocalDate.of(2026, 12, 1))
        )).thenReturn(List.of());

        renewalService.sendRenewalReminders(today);

        verify(subscriptionRepository).findRenewalCandidates(
                Subscription.SubscriptionStatus.ACTIVE,
                today,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 1)
        );

        verifyNoInteractions(mailService);
    }

    @Test
    void shouldSendReminderForValidSubscription() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        Subscription subscription =
                createSubscription(
                        1L,
                        "owner@example.com",
                        "Nguyen Van A",
                        "Hotel ABC",
                        LocalDate.of(2026, 11, 30)
                );

        when(subscriptionRepository.findRenewalCandidates(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of(subscription));

        renewalService.sendRenewalReminders(today);

        verify(mailService).sendSubscriptionRenewalReminder(
                "owner@example.com",
                "Nguyen Van A",
                "Hotel ABC",
                1L,
                LocalDate.of(2026, 11, 30)
        );
    }

    @Test
    void shouldSkipSubscriptionWithInvalidEmail() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        Subscription subscription =
                createSubscription(
                        2L,
                        "not-an-email",
                        "Nguyen Van B",
                        "Hotel XYZ",
                        LocalDate.of(2026, 11, 15)
                );

        when(subscriptionRepository.findRenewalCandidates(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of(subscription));

        renewalService.sendRenewalReminders(today);

        verifyNoInteractions(mailService);
    }

    @Test
    void shouldContinueWhenOneEmailFails() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        Subscription first =
                createSubscription(
                        3L,
                        "first@example.com",
                        "Owner 1",
                        "Hotel 1",
                        LocalDate.of(2026, 11, 10)
                );

        Subscription second =
                createSubscription(
                        4L,
                        "second@example.com",
                        "Owner 2",
                        "Hotel 2",
                        LocalDate.of(2026, 11, 20)
                );

        when(subscriptionRepository.findRenewalCandidates(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of(first, second));

        doThrow(new RuntimeException("SMTP unavailable"))
                .when(mailService)
                .sendSubscriptionRenewalReminder(
                        eq("first@example.com"),
                        any(),
                        any(),
                        eq(3L),
                        any()
                );

        renewalService.sendRenewalReminders(today);

        // Email đầu tiên lỗi, nhưng email thứ hai vẫn được thử gửi.
        verify(mailService).sendSubscriptionRenewalReminder(
                eq("first@example.com"),
                any(),
                any(),
                eq(3L),
                any()
        );

        verify(mailService).sendSubscriptionRenewalReminder(
                eq("second@example.com"),
                any(),
                any(),
                eq(4L),
                any()
        );
    }

    @Test
    void shouldStopGracefullyWhenRepositoryFails() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        when(subscriptionRepository.findRenewalCandidates(
                any(),
                any(),
                any(),
                any()
        )).thenThrow(new RuntimeException("Database unavailable"));

        renewalService.sendRenewalReminders(today);

        verifyNoInteractions(mailService);
    }

    @Test
    void shouldDoNothingWhenThereAreNoCandidates() {
        LocalDate today = LocalDate.of(2026, 10, 28);

        when(subscriptionRepository.findRenewalCandidates(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        renewalService.sendRenewalReminders(today);

        verifyNoInteractions(mailService);
    }

    private Subscription createSubscription(
            Long subscriptionId,
            String email,
            String ownerName,
            String tenantName,
            LocalDate endDate
    ) {
        Account account = Account.builder()
                .accountId(subscriptionId)
                .email(email)
                .name(ownerName)
                .build();

        Tenant tenant = Tenant.builder()
                .tenantId(subscriptionId)
                .name(tenantName)
                .account(account)
                .build();

        return Subscription.builder()
                .subscriptionId(subscriptionId)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(endDate)
                .tenant(tenant)
                .build();
    }
}