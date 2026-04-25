package com.stockpro.web.controller;

import com.stockpro.web.service.UiContextService;
import java.time.Year;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelControllerAdvice {

    private final UiContextService uiContextService;

    public GlobalModelControllerAdvice(UiContextService uiContextService) {
        this.uiContextService = uiContextService;
    }

    @ModelAttribute("appName")
    public String appName() {
        return "StockPro";
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return Year.now().getValue();
    }

    @ModelAttribute("headerModel")
    public Object headerModel() {
        return uiContextService.buildHeader();
    }
}
