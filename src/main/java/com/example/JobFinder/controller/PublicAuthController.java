package com.example.JobFinder.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicAuthController {

    @GetMapping("/login")
    public String redirectLogin() {
        return "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String redirectRegister() {
        return "redirect:/auth/register";
    }
}
