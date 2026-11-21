# practice-backend

Spring Boot 4 / Java 17 / Gradle 연습용 게시판 백엔드.
회사 프로젝트(h2care-backend)에서 발견한 문제를 처음부터 피하도록 구조와 규칙을 정해 둔다.

## 패키지 구조

```
com.practice.board
├── BoardApplication.java
├── global/                    ← 모든 기능이 함께 쓰는 것
│   ├── exception/             ← ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
│   └── config/                ← (필요해지면) Security, JPA 등 설정
└── domain/                    ← 기능별 패키지. 기능 하나 = 폴더 하나
    └── {기능}/
        ├── controller/        ← HTTP 요청·응답만 담당. Repository 직접 호출 금지
        ├── service/           ← 비즈니스 로직, @Transactional
        ├── repository/        ← DB 접근 (JPA)
        ├── entity/            ← DB 테이블과 매핑되는 클래스
        └── dto/               ← 요청·응답 객체 (XxxRequest, XxxResponse)
```

테스트(`src/test`)는 main과 **같은 패키지 경로**에 둔다.
예) `domain/hello/controller/HelloController` → `domain/hello/controller/HelloControllerTest`

## 규칙 (회사 프로젝트에서 배운 것)

| # | 회사 프로젝트 문제 | 이 프로젝트 규칙 |
|---|---|---|
| 1 | Service 하나가 2,000줄 넘음 | Service는 **300줄 넘으면 역할별로 분리** (예: `PostQueryService`, `PostCommandService`) |
| 2 | 예약 코드가 `member` 폴더에 있음 | 클래스는 **이름에 맞는 `domain/{기능}`**에만 둔다 |
| 3 | 연습·데모 코드가 `src/main`에 남아 prod에 배포됨 | 연습 코드는 다 쓰면 **삭제**. 실험은 `src/test`에서 |
| 4 | 결제·예약 테스트 없음 | **API를 추가하면 Controller 테스트도 함께** 작성 |
| 5 | 주석 처리된 옛날 코드가 쌓임 | 안 쓰는 코드는 **지운다**. 이력은 git에 남는다 |
| 6 | `anyRequest().permitAll()` | Security를 붙일 때 **기본은 차단**, 공개할 URL만 명시적으로 허용 |

회사 프로젝트에서 잘 되어 있던 것은 그대로 따른다.
- Entity에 `@Data`, `@Setter` 금지. 값 변경은 의미 있는 메서드로 (`post.updateTitle(...)`)
- 에러 응답은 `GlobalExceptionHandler` 한 곳에서 처리. Controller에서 try-catch 하지 않음
- DB 스키마 변경은 Flyway 마이그레이션으로 관리 (3단계 이후 도입)

## 학습 방식

사용자가 백엔드를 처음 배우는 중이다. 미리 만들어 주지 말고, **문제를 직접 겪은 뒤 왜 필요한지 이해하고 직접 작성**하도록 안내한다.
- `global/exception` (공통 에러 처리): 아직 없음. 에러 응답이 제각각인 문제를 겪어 본 뒤 직접 만든다.

## 설정 파일 (프로파일)

| 파일 | 용도 |
|---|---|
| `application.yml` | 공통 설정. 프로파일 미지정 시 `local`로 동작 |
| `application-local.yml` | 내 맥 개발용 |
| `application-prod.yml` | (AWS 배포 단계에서 추가) 비밀번호는 파일에 쓰지 않고 환경변수로 |

## 실행

```bash
./gradlew bootRun     # 서버 실행
./gradlew test        # 테스트 실행
```
