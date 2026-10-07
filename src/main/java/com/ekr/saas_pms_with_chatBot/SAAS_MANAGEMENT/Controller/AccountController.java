package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.LoginRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.RegisterRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/saas/user")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO request) {
        return accountService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        return accountService.login(request);
    }
}
