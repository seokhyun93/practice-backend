# 학습 노트: N+1 · Docker · CI/CD · AWS

이 프로젝트를 만들면서 배운 개념을 정리한 노트입니다.
각 개념은 **한 줄 요약 → 우리 코드에서는 → 기억할 것** 순서로 적었습니다.

## 목차
1. [JPA와 N+1](#1-jpa와-n1)
2. [Docker](#2-docker)
3. [CI: GitHub Actions](#3-ci-github-actions)
4. [시간 다루기](#4-시간-다루기)
5. [AWS 배포](#5-aws-배포)
6. [CD: 자동 배포](#6-cd-자동-배포)
7. [보안 습관](#7-보안-습관)
8. [스스로 점검하기](#8-스스로-점검하기)

---

## 1. JPA와 N+1

### JPA
**한 줄 요약:** Java 코드를 SQL로 번역해서 DB에 보내 주는 통역사. 실제로 일하는 구현체는 Hibernate.

| 우리가 쓴 Java | JPA가 만드는 SQL |
|---|---|
| `postRepository.findById(80)` | `select ... from post where id=80` |
| `findTop10ByOrderByIdDesc()` | 메서드 **이름을 읽고** SQL 생성 |
| `post.update(...)` | 트랜잭션이 끝날 때 `update ...` (변경 감지) |

**기억할 것:** 통역사가 DB에 **몇 번** 말을 거는지는 코드에 보이지 않는다. → 콘솔의 SQL 로그(`show-sql: true`)를 봐야 한다.

### N+1 문제
**한 줄 요약:** 목록 1번 조회 + 항목마다 1번씩 추가 조회 = SQL이 N+1번 나가는 성능 문제.

```
글 10개 조회 (1번) + 글마다 댓글 수 조회 (10번) = 11번 + 전체 개수 1번 = 12번
```

코드는 짧고 멀쩡해 보이는데 SQL이 폭발하는 게 특징이다. 50개면 52번, 동시 접속 100명이면 5,200번.

#### 종류 ①: 내가 반복문 안에서 쿼리를 부른 경우
```java
// ❌ 글마다 countByPostId → N번
posts.map(post -> new PostSummaryResponse(post, commentRepository.countByPostId(post.getId())));
```
**해결:** id를 모아서 한 번에 묻고, `Map`으로 짝을 맞춘다.
```sql
select post_id, count(*) from comment where post_id in (80, 79, ...) group by post_id
```
⚠️ `group by` 함정: 댓글이 **0개인 글은 결과에 아예 없다** → `getOrDefault(id, 0L)`로 채운다.

#### 종류 ②: JPA가 몰래 쿼리를 부른 경우 (지연 로딩)
```java
@ManyToOne(fetch = FetchType.LAZY)   // 댓글을 가져올 때 글은 일단 안 가져옴 (빈 껍데기 = 프록시)
private Post post;

comment.getPost().getId()      // 껍데기도 id는 앎 → 쿼리 없음
comment.getPost().getTitle()   // 제목은 모름 → 이 순간 select ... from post where id=?
```
**해결:** `join fetch`로 "댓글 가져올 때 글도 같이 가져와"라고 미리 말한다.
```java
@Query("select c from Comment c join fetch c.post order by c.id desc limit 10")
```
- `join fetch` 뒤에는 **관계**(`c.post`)만 올 수 있다. 칸(`c.post.title`)은 안 된다.
- 필요한 칸만 받고 싶으면 **프로젝션**(인터페이스 + `as 별칭`)을 쓴다.

#### 왜 처음부터 EAGER(항상 같이 가져오기)로 하지 않나
제목이 필요 없는 API에서도 매번 글을 가져오게 된다. → **기본은 LAZY, 필요한 곳에서만 fetch join.**

#### 한 번 가져온 건 기억한다 (영속성 컨텍스트)
최근 댓글 10개 중 3개가 같은 글에 달려 있으면, 그 글은 **한 번만** 조회된다. 그래서 11번이 아니라 9번이 나왔다.

### 기억할 것
> **목록 API를 만들면 SQL이 몇 번 나가는지 꼭 세어 본다.** 화면 결과가 멀쩡해도 속은 느릴 수 있다.

---

## 2. Docker

### Docker란
**한 줄 요약:** 프로그램을 "실행에 필요한 것 전부"와 함께 상자에 담아서, 어느 컴퓨터에서든 똑같이 켜지게 하는 도구.

| 단어 | 비유 | 우리 프로젝트 |
|---|---|---|
| **이미지** | 포장된 박스 (붕어빵 틀) | `mysql:8.4`, 우리 서버 이미지 |
| **컨테이너** | 박스를 열어 **실행 중인** 것 | 돌아가는 MySQL, 우리 서버 |
| **볼륨** | 박스 **밖의** 보관함 | DB 데이터. 박스를 버려도 남음 |
| **Colima** | 맥에서 상자를 돌려 주는 작은 리눅스 | 맥은 리눅스가 아니라서 필요 |

### 상자 안에 넣는 것 / 안 넣는 것
| 상자 안 (어디서나 같아야 함) | 상자 밖 (곳마다 다르거나 지켜야 함) |
|---|---|
| Java 17, 우리 jar, 기본 설정 | **비밀번호** → 환경변수(`.env`) |
| | **데이터** → 볼륨 |
| | **DB 주소** → 환경변수(`SPRING_DATASOURCE_URL`) |

같은 상자를 내 맥에서는 내 맥 DB로, AWS에서는 AWS DB로 연결할 수 있는 이유다.

### JDK · JRE · jar
| | 비유 | 하는 일 |
|---|---|---|
| `.java` | 반죽 | 우리가 쓴 코드 |
| **JDK** | 오븐 + 조리 도구 | 코드를 구워서(컴파일) jar로 만듦 |
| **jar** | 포장된 빵 | 프로그램 전체가 든 파일 하나. `java -jar`로 실행 |
| **JRE** | 진열대 | jar를 **실행만** 함. 더 가벼움 |

### 멀티 스테이지 빌드
```dockerfile
FROM eclipse-temurin:17-jdk AS build      # 1단계: 공장 (JDK)
COPY . .
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre               # 2단계: 배송 (JRE)
ENV TZ=Asia/Seoul
COPY --from=build /app/build/libs/board-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- jar 만들기까지 상자 안에서 → **`bootJar`를 깜빡해서 옛날 jar가 들어가는 실수**가 사라짐
- 최종 이미지에는 JRE + jar만 → 가볍고, 소스코드가 없음

### docker-compose
**한 줄 요약:** 상자 여러 개를 한 파일에 적어 두고 한 줄로 같이 띄우는 도구.
- 같은 compose 안의 상자끼리는 **서비스 이름으로** 부른다 (`jdbc:mysql://mysql:3306/board`)
- `healthcheck` + `depends_on: condition: service_healthy` → **DB가 준비된 뒤에** 서버를 켠다
- `docker compose down`은 컨테이너만 지움. `down -v`는 **볼륨(데이터)까지** 지움 ⚠️

### 상자 안의 `localhost`는 상자 자신이다
```
[app 상자] localhost:3306 → 자기 안을 찾음 → MySQL 없음 💥
```
IntelliJ로 실행할 때는 `localhost` = 맥이라서 됐던 것. 상자에서는 **서비스 이름**을 써야 한다.

### `.dockerignore`
Docker판 `.gitignore`. `COPY . .` 할 때 `.env`, `secret-local.yml`이 상자에 들어가지 않게 막는다.
**비밀 파일을 막는 규칙은 처음 복사하기 전에 만들어야 한다.** 나중에 만들면 이미 복사된 것을 따로 찾아 지워야 한다.

### Git과 Docker
| | 공유하는 것 | 비유 |
|---|---|---|
| Git | 코드 (Dockerfile, compose 포함) | 레시피 |
| Docker | 코드를 돌리는 환경 | 주방 |

- `docker-compose.yml`을 Git에 올리면, 팀원은 `git pull` → `.env` 만들기 → `docker compose up`으로 **같은 환경**을 얻는다.
- 단, **데이터는 공유되지 않는다.** 각자 자기 DB를 가진다. 같이 보려면 공용 dev DB가 필요하다.
- `.env`는 Git에 안 올리므로 빈칸 견본 `.env.example`을 올린다.

### 자주 쓴 명령
| 명령 | 뜻 |
|---|---|
| `docker compose up -d --build` | 이미지를 새로 만들고 뒤에서 실행 |
| `docker compose ps` / `logs app` | 상태 / 로그 보기 |
| `docker compose exec app printenv TZ` | 실행 중인 상자 안에서 명령 실행 |
| `docker system df` | 무엇이 얼마나 차지하는지 |
| `docker rm` / `rmi` / `volume rm` | 컨테이너 / 이미지 / **볼륨(데이터, 되돌릴 수 없음)** 삭제 |

---

## 3. CI: GitHub Actions

### CI란
**한 줄 요약:** push할 때마다 GitHub의 깨끗한 리눅스 컴퓨터가 테스트를 자동으로 돌려 주는 것.
`.github/workflows/ci.yml`에 "언제(`on`), 어디서(`runs-on`), 무엇을(`steps`)"을 적는다.

### 초록불 ≠ 안전
`./gradlew test` 줄이 빠졌는데도 ✅가 떴다. GitHub는 **시킨 일만** 하고, 시킨 일을 다 했으면 성공이라고 한다.
→ CI 결과를 볼 때는 **무엇을 돌렸는지**까지 확인한다.

### GitHub 컴퓨터는 텅 비어 있다
| 테스트 | CI 결과 | 이유 |
|---|---|---|
| `@WebMvcTest` | ✅ | 가짜 Service(`@MockitoBean`) → DB 불필요 |
| `@SpringBootTest` | ❌ → 해결 | 진짜 DB가 필요한데 없음 → **서비스 컨테이너**로 MySQL 8.4를 띄움 |

H2(가짜 메모리 DB)는 MySQL 문법이 다를 수 있어서 쓰지 않았다. **운영과 같은 DB로 테스트**하는 것이 원칙.

### CI의 실력 = 테스트의 실력
테스트가 없으면 CI는 컴파일 정도만 확인한다. 그래서 "API를 추가하면 테스트도 함께"가 규칙이다.
CI가 진짜로 빛난 순간: **맥에서는 통과하고 리눅스에서만 실패하는 문제**를 배포 전에 잡았다 (→ 4장).

### 로그 보는 곳
- 웹: 저장소 → **Actions** → 실행 클릭 → job 클릭 → 단계 펼치기
- 터미널: `gh run list`, `gh run view --log-failed`

---

## 4. 시간 다루기

### 정밀도: 리눅스는 9자리, DB는 6자리
```
저장 직후 Java 값:  07:05:29.417899442   (리눅스 Java = 나노초 9자리)
DB에서 다시 읽은 값: 07:05:29.417899      (DATETIME(6) = 마이크로초 6자리)
```
맥은 6자리라서 테스트가 통과했고, 리눅스(CI, AWS)에서만 실패했다.
**해결:** 시간을 만들 때부터 6자리로 자른다.
```java
@Bean
public DateTimeProvider auditingDateTimeProvider() {
    return () -> Optional.of(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
}
```
테스트를 느슨하게 고치지 않고 **진짜 원인**을 고쳤다. 이 문제는 실제 서비스에서도 "작성 응답과 조회 응답의 시간이 다름"으로 나타났을 것이다.

### 시간대: 상자는 자기가 어느 나라에 있는지 모른다
- 컨테이너 기본값은 **UTC** → 한국보다 9시간 느림
- `Dockerfile`에 `ENV TZ=Asia/Seoul` → 어디서 띄우든 한국 시간
- 로그 끝이 `Z`면 UTC, `+09:00`이면 한국 시간
- **이미 저장된 데이터는 바뀌지 않는다.** 시간대는 서비스 시작 전에 정해야 한다. 나중에 바꾸면 데이터 마이그레이션이 필요하다.
- 서버 컴퓨터(Ubuntu)와 상자의 시계는 **따로**다. 우리는 상자(Java)만 바꿨다.

---

## 5. AWS 배포

### 비용 💰
- 계정 플랜이 **Free 플랜**이면 크레딧($100~$200, 6개월) 안에서만 쓰고, 유료 플랜으로 바꾸지 않는 한 결제되지 않는다.
- **"업그레이드" / "고급 기능 활성화" 버튼은 누르지 않는다.** (다른 리전, OIDC 등이 여기에 묶여 있었다)
- 서버보다 **예산 알림을 먼저** 만든다.
- 안 쓸 때는 EC2 **중지(Stop)**. 완전히 그만둘 때는 **종료(Terminate)** + 정리 체크리스트.

### 리전
AWS 서버가 실제로 있는 도시. 화면이 리전마다 따로라서, "만들었는데 안 보인다"면 대부분 리전 문제다.
Free 플랜 프로젝트가 시드니에 묶여 있어서 시드니를 썼다.

### EC2와 보안 그룹
| 개념 | 비유 | 설정 |
|---|---|---|
| EC2 | 빌린 리눅스 컴퓨터 | Ubuntu 26.04 LTS, Arm(`t4g.small`), 디스크 20GB |
| 보안 그룹 | 문지기 | SSH 22, API 8081 모두 **내 IP만**. MySQL 3307은 **열지 않음** |
| 키 페어 (`.pem`) | 열쇠 | `~/.ssh`에 두고 `chmod 400` |
| 스왑 | 비상용 메모리 | 메모리 1.8GB라 빌드 중 죽지 않게 2GB 추가 |

- `0.0.0.0/0` = 전 세계 누구나. `/32` = 딱 그 주소 하나.
- `Connection timed out`은 "거절"이 아니라 **"대답 없음"**. 보안 그룹은 모르는 손님에게 대답하지 않는다.
- EC2를 중지했다 켜면 **퍼블릭 IP가 바뀐다.** (`EC2_HOST` Secret, `~/.ssh/config` 수정 필요)

### SSH
**한 줄 요약:** 내 맥의 키보드와 화면으로, 원격 컴퓨터에서 명령을 실행하는 리모컨.
프롬프트로 어디에 있는지 구분한다: `➜ practice-backend` = 맥, `ubuntu@ip-...` = 서버.

### 버전 고르기
"무조건 최신"보다 **LTS(장기 지원) 중 최신**. 너무 새 버전은 호환성 문제, 너무 오래된 버전은 보안 패치가 끊긴다.
(brew가 몰래 올려 버린 MySQL 9.2는 지원이 끝난 실험판이었다.)

---

## 6. CD: 자동 배포

### CD란
**한 줄 요약:** 테스트가 통과하면 서버 배포까지 자동으로 하는 것.
```
전:  push → ✅ 확인 → ssh → git pull → docker compose up   (사람이 5번)
후:  push → 끝                                            (나머지는 GitHub)
```

### 우리 배포 단계
```yaml
deploy:
  needs: test                      # 테스트 통과해야만
  if: push && main                 # main에 push할 때만 (PR은 테스트만)
  steps:
    1) GitHub 컴퓨터 IP 확인 → $GITHUB_ENV에 저장 (다음 단계에 넘겨주는 메모장)
    2) 보안 그룹에 그 IP 추가
    3) 배포용 열쇠로 SSH → 서버가 deploy.sh 실행
    4) 보안 그룹에서 IP 삭제   ← if: always() : 3)이 실패해도 반드시
```

### GitHub Secrets
GitHub 저장소의 금고. 넣으면 **나도 다시 볼 수 없고**, 로그에서는 `***`로 가려진다.
- 비밀인 것 → Secrets (AWS 키, SSH 열쇠, 서버 주소)
- 비밀 아닌 것 → 파일의 `env:` (보안 그룹 ID, 리전). 누가 봐도 어떤 값인지 알 수 있어서 좋다.

### 최소 권한 원칙
**필요한 권한만, 필요한 대상에만** 준다. 새어 나가도 피해가 거기까지만 간다.
| 맡긴 것 | 할 수 있는 것 |
|---|---|
| IAM 사용자 `github-deploy` | board-sg 규칙 **추가·삭제만** |
| 배포용 SSH 열쇠 | 서버에서 **`deploy.sh`만** 실행 (`authorized_keys`의 `command=`) |
| 내 열쇠 (`board-key`) | 맡기지 않음. 내 맥에만 |

- **OIDC**(배포할 때마다 1시간짜리 출입증)가 가장 좋지만 Free 플랜에서 막혀서, **좁은 권한의 접근 키**를 썼다.
- 배포용 열쇠는 내 열쇠와 **따로** 만든다. 문제가 생기면 그것만 버리면 된다.

### 배포 중 다운타임
`docker compose up --build`는 옛날 상자를 끄고 새 상자를 켜기 때문에, 그 사이 10~15초 동안 서버가 응답하지 않는다. → 실서비스는 **무중단 배포**로 해결 (다음에 배울 것).

---

## 7. 보안 습관

| 습관 | 이유 / 겪은 일 |
|---|---|
| 비밀 파일은 **만들기 전에** `.gitignore`, `.dockerignore`에 등록 | `.env`가 커밋 대기 상태가 됐었고, `.dockerignore` 전에 빌드한 이미지에 비밀 파일이 들어갔었다 |
| 커밋 전 `git status`로 비밀 파일이 없는지 확인 | IntelliJ "Add to Git?"에 Yes를 누르면 바로 추가된다 |
| 공개 저장소에는 개인 이메일 대신 GitHub noreply 이메일 | 커밋 기록은 누구나 볼 수 있다 |
| 비밀번호는 사람이 몰라도 되면 무작위로 | 서버 `.env`는 `openssl rand -hex 16`으로 만들어서 아무도 모른다 |
| 비밀 값은 채팅·화면에 띄우지 않고 바로 금고로 | `gh secret set NAME < 파일` |
| 열쇠 파일은 `~/.ssh`에, 권한 `400` | 다른 사람이 읽을 수 있으면 ssh가 거부한다 |
| 줄 끝 공백 조심 | `\` 뒤에 공백이 있으면 명령이 끊긴다. IntelliJ "저장 시 줄 끝 공백 제거" |

---

## 8. 스스로 점검하기

답을 말로 설명할 수 있으면 개념이 잡힌 것이다.

<details>
<summary>1. 상자 안에서 <code>localhost:3306</code>으로 DB에 못 붙는 이유는?</summary>

상자 안의 `localhost`는 맥이 아니라 **그 상자 자신**이다. compose에서는 서비스 이름(`mysql`)으로 접속한다.
</details>

<details>
<summary>2. <code>.env</code>는 Git에 안 올리는데, 팀원은 어떤 값을 채워야 하는지 어떻게 아나?</summary>

빈칸 견본 `.env.example`을 Git에 올려 둔다. 팀원은 복사해서 `.env`를 만들고 값을 채운다.
</details>

<details>
<summary>3. CI에서 <code>@WebMvcTest</code>는 통과하고 <code>@SpringBootTest</code>만 실패한 이유는?</summary>

`@WebMvcTest`는 가짜 Service를 써서 DB가 필요 없고, `@SpringBootTest`는 진짜 DB가 필요한데 GitHub 컴퓨터에 MySQL이 없었다.
</details>

<details>
<summary>4. <code>needs: test</code>가 없으면?</summary>

테스트가 실패해도 배포가 같이 실행되어, 망가진 코드가 서버에 올라갈 수 있다.
</details>

<details>
<summary>5. <code>if: always()</code>가 없으면?</summary>

배포가 실패하면 그 뒤 단계가 건너뛰어져서, 보안 그룹에 GitHub IP가 **열린 채로** 남는다.
</details>

<details>
<summary>6. 리눅스에서만 테스트가 실패한 이유는?</summary>

리눅스 Java는 시간을 나노초(9자리)까지 재는데 DB는 마이크로초(6자리)까지만 저장해서, 저장 직후 값과 다시 읽은 값이 달랐다.
</details>

<details>
<summary>7. N+1 두 종류와 각각의 해결법은?</summary>

① 반복문 안에서 직접 쿼리 → id를 모아 `in + group by`로 한 번에. ② 지연 로딩으로 JPA가 몰래 쿼리 → `join fetch`.
</details>

<details>
<summary>8. 컨테이너를 지워도 DB 데이터가 남는 이유는?</summary>

데이터를 컨테이너 안이 아니라 **볼륨**에 저장하기 때문이다. `docker compose down -v`를 하면 볼륨까지 지워진다.
</details>
