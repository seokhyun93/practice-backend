package com.practice.board.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private String code;      // 프론트가 코드로 분기하기 위한 값 (예: "INVALID_INPUT")
    private String message;   // 사람이 읽을 설명 (앱 화면에 그대로 띄울 수도 있음)
}
