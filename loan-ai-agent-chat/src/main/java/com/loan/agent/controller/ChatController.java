package com.loan.agent.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatLanguageModel chatModel;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String reply = chatModel.generate(request.getMessage());
        return Map.of("reply", reply);
    }
}
