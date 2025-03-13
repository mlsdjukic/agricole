package com.example.alarms.controllers;

import com.example.alarms.services.AlarmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
public class WebController {

    private final AlarmService alarmService;

    public WebController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping
    public Mono<Rendering> index(final Model model) {
        return alarmService.getAll()
                .collectList() // Convert the Flux<AlarmResponseDTO> to Mono<List<AlarmResponseDTO>>
                .map(alarms -> Rendering.view("ui/index")
                        .modelAttribute("alarms", alarms)
                        .build());
    }

}
