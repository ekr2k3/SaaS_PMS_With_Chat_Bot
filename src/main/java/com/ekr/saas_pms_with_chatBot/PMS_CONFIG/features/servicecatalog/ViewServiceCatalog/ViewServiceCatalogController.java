
package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.features.servicecatalog.ViewServiceCatalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services/catalog")
public class ViewServiceCatalogController {

    private final ViewServiceCatalogService viewServiceCatalogService;

    public ViewServiceCatalogController(
            ViewServiceCatalogService viewServiceCatalogService
    ) {
        this.viewServiceCatalogService = viewServiceCatalogService;
    }

    @GetMapping
    public ServiceCatalogDataResponse getCatalog() {
        return new ServiceCatalogDataResponse(
                viewServiceCatalogService.getActiveServices()
        );
    }

    public record ServiceCatalogDataResponse(
            List<ServiceCatalogResponse> data
    ) {
    }
}