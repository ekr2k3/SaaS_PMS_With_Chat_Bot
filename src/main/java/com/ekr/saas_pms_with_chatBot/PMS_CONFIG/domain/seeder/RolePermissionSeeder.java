package com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.seeder;

import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Permission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.Role;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RolePermission;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.entity.RolePermissionId;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.PermissionRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RolePermissionRepository;
import com.ekr.saas_pms_with_chatBot.PMS_CONFIG.domain.repository.RoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class RolePermissionSeeder implements ApplicationRunner {

    private static final String OWNER = "OWNER";
    private static final String MANAGER = "MANAGER";
    private static final String RECEPTIONIST = "RECEPTIONIST";
    private static final String HOUSEKEEPER = "HOUSEKEEPER";
    private static final String ACCOUNTANT = "ACCOUNTANT";
    private static final String FNB = "F&B";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionSeeder(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedRoles();
        seedPermissionsAndRoleMappings();
        cleanLegacyPermissions();
    }

    private void seedRoles() {
        seedRole(OWNER, "Chủ sở hữu Property");
        seedRole(MANAGER, "Quản lý");
        seedRole(RECEPTIONIST, "Lễ tân");
        seedRole(HOUSEKEEPER, "Nhân viên buồng phòng");
        seedRole(ACCOUNTANT, "Kế toán");
        seedRole(FNB, "Nhân viên ẩm thực và đồ uống");
    }

    private void seedRole(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(description)
                    .status(Role.RoleStatus.ACTIVE)
                    .build());
        }
    }

    private void seedPermissionsAndRoleMappings() {
        for (PermissionDefinition definition : defaultPermissions()) {
            Permission permission = permissionRepository
                    .findByCode(definition.code())
                    .orElseGet(() -> permissionRepository.save(
                            Permission.builder()
                                    .code(definition.code())
                                    .name(definition.code())
                                    .status(Permission.PermissionStatus.ACTIVE)
                                    .build()
                    ));

            if (permission.getStatus()
                    != Permission.PermissionStatus.ACTIVE) {
                continue;
            }

            /*
             * NIGHT_AUDIT permissions phải chỉ thuộc MANAGER.
             * Xóa mapping cũ của các Role khác trước khi seed.
             */
            if (definition.code().startsWith("NIGHT_AUDIT_")) {
                Role manager = roleRepository.findByName(MANAGER)
                        .orElseThrow(() -> new IllegalStateException(
                                "Role seed missing: " + MANAGER
                        ));

                rolePermissionRepository
                        .deleteAllByPermission_PermissionIdAndRole_RoleIdNot(
                                permission.getPermissionId(),
                                manager.getRoleId()
                        );
            }

            if ("PROPERTY_DAILY_STATISTICS_VIEW".equals(
                    definition.code()
            )) {
                Role owner = roleRepository.findByName(OWNER)
                        .orElseThrow(() -> new IllegalStateException(
                                "Role seed missing: " + OWNER
                        ));

                rolePermissionRepository
                        .deleteAllByPermission_PermissionIdAndRole_RoleIdNot(
                                permission.getPermissionId(),
                                owner.getRoleId()
                        );
            }

            for (String roleName : definition.roleNames()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalStateException(
                                "Role seed missing: " + roleName
                        ));

                if (role.getStatus() != Role.RoleStatus.ACTIVE) {
                    continue;
                }

                boolean exists = rolePermissionRepository
                        .existsByRole_RoleIdAndPermission_PermissionId(
                                role.getRoleId(),
                                permission.getPermissionId()
                        );

                if (!exists) {
                    RolePermissionId id = new RolePermissionId(
                            role.getRoleId(),
                            permission.getPermissionId()
                    );

                    rolePermissionRepository.save(
                            RolePermission.builder()
                                    .id(id)
                                    .role(role)
                                    .permission(permission)
                                    .build()
                    );
                }
            }
        }
    }

    private void cleanLegacyPermissions() {
        /*
         * F36 là chức năng chỉ xem.
         * PROPERTY_REPORT_CONFIRM không còn được sử dụng.
         */
        permissionRepository.findByCode("PROPERTY_REPORT_CONFIRM")
                .ifPresent(permission -> {
                    rolePermissionRepository
                            .deleteAllByPermission_PermissionId(
                                    permission.getPermissionId()
                            );

                    permission.setStatus(
                            Permission.PermissionStatus.INACTIVE
                    );

                    permissionRepository.save(permission);
                });
    }

    private List<PermissionDefinition> defaultPermissions() {
        List<PermissionDefinition> definitions = new ArrayList<>();

        add(definitions, "RESERVATION_VIEW", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "RESERVATION_CREATE", RECEPTIONIST);
        add(definitions, "RESERVATION_UPDATE", RECEPTIONIST);
        add(definitions, "RESERVATION_CANCEL", RECEPTIONIST);
        add(definitions, "ROOM_VIEW", RECEPTIONIST, HOUSEKEEPER);
        add(definitions, "ROOM_STATUS_UPDATE", HOUSEKEEPER);
        add(definitions, "HOUSEKEEPING_VIEW", RECEPTIONIST, HOUSEKEEPER);
        add(definitions, "HOUSEKEEPING_ISSUE_CREATE", RECEPTIONIST, HOUSEKEEPER);
        add(definitions, "PAYMENT_VIEW", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "PAYMENT_CREATE", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "CHECKIN_EXECUTE", RECEPTIONIST);
        add(definitions, "GUEST_VIEW", RECEPTIONIST, ACCOUNTANT, FNB);
        add(definitions, "GUEST_CREATE", RECEPTIONIST);
        add(definitions, "MENU_VIEW", RECEPTIONIST, ACCOUNTANT, FNB);
        add(definitions, "FB_REQUEST_CREATE", FNB);
        add(definitions, "FB_REQUEST_APPROVE");
        add(definitions, "FB_STOCK_UPDATE", FNB);
        add(definitions, "FOLIO_VIEW", RECEPTIONIST, ACCOUNTANT, FNB);
        add(definitions, "FOLIO_CHARGE_CREATE", RECEPTIONIST, ACCOUNTANT, FNB);
        add(definitions, "GUEST_SERVICE_CREATE", RECEPTIONIST, FNB);
        add(definitions, "REFUND_VIEW", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "REFUND_CREATE", RECEPTIONIST);
        add(definitions, "REFUND_EXECUTE");
        add(definitions, "REFUND_UPDATE");
        add(definitions, "REFUND_CALCULATE", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "NO_SHOW_VIEW", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "NO_SHOW_NOTIFY", RECEPTIONIST);
        add(definitions, "NO_SHOW_CONFIRM", RECEPTIONIST);
        add(definitions, "CHECKOUT_VIEW", RECEPTIONIST, ACCOUNTANT);
        add(definitions, "CHECKOUT_EXECUTE", RECEPTIONIST);

        // Night Audit: chỉ Manager.
        addOnly(definitions, "NIGHT_AUDIT_VIEW", MANAGER);
        addOnly(definitions, "NIGHT_AUDIT_ADJUST", MANAGER);
        addOnly(definitions, "NIGHT_AUDIT_CONFIRM", MANAGER);

        // F36: Owner chỉ xem Daily Statistics của Tenant.
        addOnly(definitions, "PROPERTY_DAILY_STATISTICS_VIEW", OWNER);

        // F37 - giữ nguyên permission riêng.
        add(definitions, "PROPERTY_MONITOR_VIEW");

        add(definitions, "CHATBOT_HOTEL_INFO",
                RECEPTIONIST, HOUSEKEEPER, ACCOUNTANT, FNB);
        add(definitions, "CHATBOT_REALTIME_INFO",
                RECEPTIONIST, HOUSEKEEPER, ACCOUNTANT, FNB);
        add(definitions, "CHATBOT_REPORT_CHECK", ACCOUNTANT);
        add(definitions, "CHATBOT_OPERATION_QUERY",
                RECEPTIONIST, HOUSEKEEPER, ACCOUNTANT, FNB);
        add(definitions, "CHATBOT_GUIDE_QUERY",
                RECEPTIONIST, HOUSEKEEPER, ACCOUNTANT, FNB);

        return definitions;
    }

    /*
     * Permission mặc định: OWNER + MANAGER + các role bổ sung.
     */
    private void add(
            List<PermissionDefinition> definitions,
            String code,
            String... additionalRoleNames
    ) {
        Set<String> roles = new LinkedHashSet<>();
        roles.add(OWNER);
        roles.add(MANAGER);
        roles.addAll(List.of(additionalRoleNames));

        definitions.add(new PermissionDefinition(
                code,
                Set.copyOf(roles)
        ));
    }

    /*
     * Permission chỉ cấp cho chính xác các Role được truyền vào.
     * Không tự động thêm OWNER hoặc MANAGER.
     */
    private void addOnly(
            List<PermissionDefinition> definitions,
            String code,
            String... roleNames
    ) {
        definitions.add(new PermissionDefinition(
                code,
                Set.copyOf(List.of(roleNames))
        ));
    }

    private record PermissionDefinition(
            String code,
            Set<String> roleNames
    ) {
    }
}
