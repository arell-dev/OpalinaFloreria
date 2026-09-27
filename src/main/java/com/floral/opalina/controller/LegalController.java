package com.floral.opalina.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {
    @GetMapping("/terminos")
    public String terminos() {
        return "legal/terminos";
    }

    @GetMapping("/privacidad")
    public String privacidad() {
        return "legal/privacidad";
    }
}
