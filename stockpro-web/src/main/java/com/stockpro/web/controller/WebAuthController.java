package com.stockpro.web.controller;

import com.stockpro.web.security.RoleConstants;
import com.stockpro.web.util.SecurityUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebAuthController {

    @GetMapping("/")
    public String home() {
        return SecurityUtils.getCurrentUser()
                .map(user -> {
                    if (RoleConstants.ADMIN.equals(user.getRole())) {
                        return "redirect:/admin/dashboard";
                    }
                    if (RoleConstants.PURCHASE_OFFICER.equals(user.getRole())) {
                        return "redirect:/purchase/orders";
                    }
                    return "redirect:/inventory/dashboard";
                })
                .orElse("redirect:/login");
    }

    @GetMapping("/login")
    public String login() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }
}
