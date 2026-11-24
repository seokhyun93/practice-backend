package com.practice.board.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    /**
     * 작성일·수정일을 DB 컬럼(DATETIME(6))과 같은 마이크로초(6자리)로 맞춘다.
     * 리눅스의 Java는 나노초(9자리)까지 재서, 저장 직후 값과 DB에서 다시 읽은 값이 달라지기 때문.
     */
    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
    }
}
