package com.practice.board.domain.hello.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HelloResponse {
    private String message;
    private String name;
}
