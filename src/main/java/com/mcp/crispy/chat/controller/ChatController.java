package com.mcp.crispy.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/crispy")
public class ChatController {

    @GetMapping("/chat")
    public String chat(Principal principal) {
        if (principal != null) {
            return "chat/chat";
        } else {
            return "redirect:/crispy/login";
        }
    }
}
