package com.thinkai4j.example.controller;

import com.thinkai4j.core.api.AiChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AiChat chat;

    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return chat.ask(q);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String q) {
        return chat.stream(q);
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(@RequestParam String q) {
        SseEmitter emitter = new SseEmitter(0L);

        chat.stream(q)
                .doOnNext(content -> {
                    try {
                        emitter.send(content);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(emitter::complete)
                .doOnError(emitter::completeWithError)
                .subscribe();

        return emitter;
    }

    @GetMapping("/provider")
    public String askWithProvider(@RequestParam String q, @RequestParam String provider) {
        return chat.provider(provider).ask(q);
    }

    @GetMapping("/system")
    public String askWithSystem(@RequestParam String q, @RequestParam(defaultValue = "你是一个专业的AI助手") String system) {
        return chat.system(system).ask(q);
    }
}
