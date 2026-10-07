package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.SubscriptionPlanCreateRequest;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.SubscriptionPlanResponse;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.SubscriptionPlan;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saas")
public class SubscriptionPlanController {

    @Autowired
    private SubscriptionPlanRepository subscriptionPlanRepository;

    @PostMapping("/admin/createPlan")
    public ResponseEntity<?> createPlan(
            @RequestBody SubscriptionPlanCreateRequest req
    ) {

        // 1. Tạo Entity từ Request DTO
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .duration(req.getDuration())
                .durationUnit(req.getDurationUnit())
                .maxUsers(req.getMaxUsers())
                .maxProperties(req.getMaxProperties())
                .maxStorage(req.getMaxStorage())
                .maxRooms(req.getMaxRooms())
                .status(SubscriptionPlan.PlanStatus.ACTIVE)
                .build();

        // 2. Lưu Entity vào Database
        SubscriptionPlan savedPlan =
                subscriptionPlanRepository.save(plan);

        // 3. Tạo Response DTO
        SubscriptionPlanResponse response =
                SubscriptionPlanResponse.builder()
                        .planId(savedPlan.getPlanId())
                        .name(savedPlan.getName())
                        .description(savedPlan.getDescription())
                        .price(savedPlan.getPrice())
                        .duration(savedPlan.getDuration())
                        .durationUnit(savedPlan.getDurationUnit())
                        .maxUsers(savedPlan.getMaxUsers())
                        .maxProperties(savedPlan.getMaxProperties())
                        .maxStorage(savedPlan.getMaxStorage())
                        .maxRooms(savedPlan.getMaxRooms())
                        .status(savedPlan.getStatus())
                        .build();

        // 4. Trả Response
        return ResponseEntity.ok(response);
    }


    // XEM LIST PLAN
    @GetMapping("/user/Plan/List")
    public List<SubscriptionPlan> listPlan(){
        return subscriptionPlanRepository.findAll();
    }
    // Xem chi tiết 1 PLAN
    // Điền trên queryString phần sau ?
    @GetMapping("/user/Plan/Detail")
    public SubscriptionPlan PlanDetail(@RequestParam Long id){
        return subscriptionPlanRepository.findByPlanId(id);
    }
}
