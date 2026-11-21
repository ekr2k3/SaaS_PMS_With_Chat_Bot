
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.seeder;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.PaymentGatewayCatalog;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PaymentGatewayCatalogRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentGatewayCatalogSeeder
        implements ApplicationRunner {

    private final PaymentGatewayCatalogRepository repository;

    public PaymentGatewayCatalogSeeder(
            PaymentGatewayCatalogRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (repository.existsByName("STRIPE")) {
            return;
        }

        PaymentGatewayCatalog stripe =
                PaymentGatewayCatalog.builder()
                        .name("STRIPE")
                        .description(
                                "Stripe Payment Gateway"
                        )
                        .status("ACTIVE")
                        .build();

        repository.save(stripe);
    }
}