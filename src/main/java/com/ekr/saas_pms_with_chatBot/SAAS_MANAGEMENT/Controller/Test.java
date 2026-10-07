package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Test {
    @GetMapping("/api/test/hello")
    public String hello(){
        return "Hello world";
    }
}
