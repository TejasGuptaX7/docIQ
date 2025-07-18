package com.vectormind.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "drive", required = false) String driveStatus) {
        // Just redirect to the frontend
        return "redirect:http://localhost:5173/dashboard" + (driveStatus != null ? "?drive=" + driveStatus : "");
    }
}