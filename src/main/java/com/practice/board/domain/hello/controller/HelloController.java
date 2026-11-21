package com.practice.board.domain.hello.controller;

import com.practice.board.domain.hello.dto.HelloRequest;
import com.practice.board.domain.hello.dto.HelloResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "손님") String name) {
        return "안녕하세요 " + name;
    }

    @GetMapping("/hello/json")
    public HelloResponse helloJson(@RequestParam(defaultValue = "안녕하세요") String message, @RequestParam String name) {
        return new HelloResponse(message, name);
    }

    @PostMapping("/hello")
    public HelloResponse helloPost(@RequestBody HelloRequest request) {
        String message = request.getName() + "님은 " + request.getAge() + "살입니다";
        return new HelloResponse(message, request.getName());
    }
}
