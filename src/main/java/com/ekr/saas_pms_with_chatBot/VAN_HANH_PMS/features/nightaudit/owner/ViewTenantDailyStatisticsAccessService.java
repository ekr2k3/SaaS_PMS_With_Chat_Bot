package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.nightaudit.owner;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Tenant;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViewTenantDailyStatisticsAccessService {

    private static final String REQUIRED_PERMISSION =
            "PERM_PROPERTY_DAILY_STATISTICS_VIEW";

    private final AccountRepository accountRepository;
    private final TenantRepository tenantRepository;

    public ViewTenantDailyStatisticsAccessService(
            AccountRepository accountRepository,
            TenantRepository tenantRepository
    ) {
        this.accountRepository = accountRepository;
        this.tenantRepository = tenantRepository;
    }

    public Tenant requireOwnerTenant() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        boolean isOwner = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        "ROLE_OWNER".equals(authority.getAuthority())
                );

        if (!isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only OWNER can view Tenant Daily Statistics"
            );
        }

        boolean hasPermission = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        REQUIRED_PERMISSION.equals(authority.getAuthority())
                );

        if (!hasPermission) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Missing permission: PROPERTY_DAILY_STATISTICS_VIEW"
            );
        }

        Account account = accountRepository.findByEmail(
                authentication.getName()
        );

        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Owner account not found"
            );
        }

        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Owner account is inactive"
            );
        }

        return tenantRepository.findByAccount(account)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tenant not found"
                ));
    }
}
