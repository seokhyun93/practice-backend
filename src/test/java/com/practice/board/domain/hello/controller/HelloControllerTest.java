package com.practice.board.domain.hello.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 서버를 띄우지 않고 Controller만 빠르게 테스트한다.
 * IntelliJ에서 클래스 옆 ▶ 버튼으로 실행.
 */
@WebMvcTest(HelloController.class)
class HelloControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void name을_주면_인사말을_반환한다() throws Exception {
        mockMvc.perform(get("/hello").param("name", "석현"))
                .andExpect(status().isOk())
                .andExpect(content().string("안녕하세요 석현"));
    }

    @Test
    void name이_없으면_기본값_손님을_사용한다() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("안녕하세요 손님"));
    }
}
