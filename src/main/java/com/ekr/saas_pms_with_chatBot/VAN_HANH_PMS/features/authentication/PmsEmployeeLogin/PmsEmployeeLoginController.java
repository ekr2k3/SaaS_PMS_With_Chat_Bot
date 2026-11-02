package com.ekr.saas_pms_with_chatBot.VAN_HANH_PMS.features.authentication.PmsEmployeeLogin;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pms/auth")
public class PmsEmployeeLoginController {

    private final PmsEmployeeLoginService loginService;

    public PmsEmployeeLoginController(
            PmsEmployeeLoginService loginService
    ) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<PmsEmployeeLoginResponse> login(
            @Valid @RequestBody PmsEmployeeLoginRequest request
    ) {
        return ResponseEntity.ok(
                loginService.login(request)
        );
    }
}