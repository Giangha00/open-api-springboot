package com.sem4.openapispringboot.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaRedirectController {

    @GetMapping(value = {
        "/products",
        "/orders",
        "/orders/**",
        "/admin/orders",
        "/admin/reports",
        "/partner/shipping",
        "/swagger"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
