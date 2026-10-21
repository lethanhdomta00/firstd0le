package com.erp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // Forward any front-end route to index.html so Vue Router can handle it
    @GetMapping(value = {
        "/login",
        "/products",
        "/users",
        "/dashboard",
        "/{path:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
