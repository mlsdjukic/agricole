package com.example.alarms.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/ui")
public class WebController {

    @GetMapping()
    public Mono<Rendering> index(final Model model) {
        return Mono.just(Rendering.view("ui/index")
                .modelAttribute("name", "Boske").build());


    }
}