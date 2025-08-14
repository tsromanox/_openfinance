// SwaggerController.java (simplificado)
package br.com.openfinance.participants.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden
public class SwaggerController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/docs")
    public String redirectDocsToSwagger() {
        return "redirect:/swagger-ui.html";
    }
}
