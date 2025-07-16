package com.vectormind.api;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "drive", required = false) String driveStatus, Model model) {
        if ("connected".equals(driveStatus)) {
            model.addAttribute("message", "Google Drive connected successfully! Files are being synchronized.");
            model.addAttribute("messageType", "success");
        }
        return "dashboard";
    }
}
