package com.example.alarms.controllers;

import com.example.alarms.dto.AlarmWithTypeAndClass;
import com.example.alarms.services.AlarmService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
public class WebController {

    private final AlarmService alarmService;

    public WebController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<AlarmWithTypeAndClass> alarms = new ArrayList<>();
        alarmService.getAll().collectList()
                .map(list -> {
                    // use list here
                    alarms.addAll(list);
                    return list;
                }).subscribe(); // or cache / blocking call

        model.addAttribute("alarms", alarms);
        return "ui/index";  // thymeleaf template ui/index.html
    }

}
