# TaskBoard

Jira 스타일의 사내 협업 툴. 작업(이슈) 관리, 실시간 협업, 배포(릴리즈) 관리, 관리자/모니터링 앱으로 이루어져 있다.

기획·설계·구현 경위는 [프로젝트_기획서.md](프로젝트_기획서.md)에 정리돼 있다.

| 영역 | 스택 |
|---|---|
| 백엔드 | Spring Boot 3.3 / Java 17 / Spring Security(JWT) / JPA / STOMP WebSocket |
| 프론트 | React 19 / Vite / TanStack Query / React Router / Tailwind CSS 4 |
| DB | PostgreSQL |
| 배포 | Docker Compose + Caddy (오라클 클라우드 무료 VM) |

---

## 로컬 개발

PostgreSQL이 `localhost:5432`에 있고 `taskboard` DB가 만들어져 있어야 한다. 접속 정보는 `backend/src/main/resources/application.yml`의 기본값(`postgres` / `1234`)을 쓰거나 환경변수로 덮어쓴다.

**JDK 17이 설치돼 있어야 한다.** Gradle 툴체인이 정확히 17을 요구하고 자동 다운로드는 꺼져 있어, 21 등 다른 버전만 있으면 `Cannot find a Java installation` 에러로 빌드가 실패한다.

새 DB라 계정이 하나도 없다면, 최초 관리자를 만들기 위해 `BOOTSTRAP_ADMIN_USERNAME`/`BOOTSTRAP_ADMIN_PASSWORD` 환경변수를 넣고 기동한다(상세는 "배포 > 최초 관리자" 참고).

```bash
cd backend && BOOTSTRAP_ADMIN_USERNAME=admin BOOTSTRAP_ADMIN_PASSWORD=<원하는 비밀번호> ./gradlew bootRun
```

```bash
cd frontend && npm install && npm run dev
```

프론트 개발 서버가 `/api`와 `/ws`를 백엔드로 프록시한다(`frontend/vite.config.ts`). 운영에서는 Caddy가 같은 규칙을 맡으므로, 프론트 코드는 개발·운영에서 동일한 경로를 쓴다.

테스트는 H2 인메모리 DB로 돌아가 별도 준비가 필요 없다.

```bash
cd backend && ./gradlew test
```

---

## 배포

### 구조

빌드는 **GitHub Actions에서만** 한다. 배포 대상인 1GB VM에서 Gradle을 돌리면 OOM이 나기 때문이다.

```
main에 push
  → Actions: 백엔드 테스트 + jar 빌드, 프론트 lint + dist 빌드
  → 두 개의 멀티 아키텍처(amd64/arm64) 이미지를 GHCR에 push
서버
  → docker compose pull && docker compose up -d
```

프론트 `dist`는 Caddy 이미지 안에 들어간다. 서버로 정적 파일을 따로 옮기지 않는다.

### 서버 최초 준비 (Ubuntu 24.04)

```bash
sudo apt update && sudo apt upgrade -y && sudo apt purge snapd -y
```

1GB 인스턴스라면 스왑을 반드시 만든다. 없으면 OOM killer가 컨테이너를 골라 죽인다.

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile && echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

```bash
curl -fsSL https://get.docker.com | sudo sh && sudo usermod -aG docker $USER
```

OCI는 **보안 목록과 호스트 방화벽 양쪽**을 열어야 한다. 콘솔에서 수신 규칙(0.0.0.0/0, TCP, 대상 포트 80)을 추가한 뒤:

```bash
sudo iptables -L INPUT --line-numbers
```

출력에서 `REJECT` 줄의 번호를 확인해 그 앞에 삽입한다(아래 `6`을 그 번호로 바꾼다).

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT && sudo netfilter-persistent save
```

### 배포

서버에는 설정 파일만 있으면 된다(`docker-compose.yml`, `Caddyfile`).

```bash
git clone https://github.com/windn05/diy-taskboard.git && cd diy-taskboard
```

`.env`를 만들고 실제 값을 채운다.

```bash
cp .env.example .env && nano .env
```

`JWT_SECRET`은 반드시 바꾼다. 기본값이 저장소에 공개돼 있어 그대로 두면 누구나 관리자 토큰을 위조할 수 있다.

```bash
docker compose pull && docker compose up -d
```

`http://<공인IP>` 로 접속되면 성공이다.

### 최초 관리자

공개 회원가입이 없으므로 계정은 관리자만 만들 수 있는데, 새 DB에는 그 관리자가 없다. 그래서 **계정이 하나도 없을 때만** `.env`의 `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`로 최초 관리자를 만든다.

로그인한 뒤 헤더에서 비밀번호를 바꾸고, `.env`에서 그 두 줄을 지운다. 이후 계정은 관리자 앱의 **계정 관리** 탭에서 만든다.

> GHCR 패키지가 private이면 서버에서 pull하기 전에 `read:packages` 권한의 PAT로 `docker login ghcr.io`를 해야 한다. GitHub 패키지 설정에서 public으로 바꾸면 로그인 없이 받는다.

### 최초 확인 후

스키마가 만들어졌으면 애플리케이션이 더 이상 스키마를 바꾸지 않도록 낮춘다. `.env`에 아래를 추가하고 `docker compose up -d`를 다시 실행한다.

```
JPA_DDL_AUTO=validate
```

### 환경변수

| 변수 | 필수 | 설명 |
|---|---|---|
| `DB_PASSWORD` | ✅ | Postgres 비밀번호 |
| `JWT_SECRET` | ✅ | 32바이트 이상. **기본값을 그대로 쓰면 안 된다** |
| `JPA_DDL_AUTO` | | 기본 `update`. 최초 배포 후 `validate` 권장 |
| `JAVA_OPTS` | | 기본 `-Xmx320m -Xss256k -XX:MaxMetaspaceSize=96m` (1GB 기준) |

---

## 더 큰 인스턴스로 옮기기

오라클 A1.Flex(ARM, 최대 4 OCPU / 24GB)를 확보하면 그쪽으로 옮기는 게 좋다. 이미지가 arm64로도 빌드돼 있어 재빌드가 필요 없다.

기존 서버에서 논리 덤프를 뜬다. **`pgdata` 볼륨을 통째로 복사하면 안 된다** — PostgreSQL 데이터 디렉터리는 아키텍처에 의존해서 x86 → ARM 복사는 깨진다.

```bash
docker compose exec -T db pg_dump -U postgres taskboard > backup.sql
```

새 서버에서 위 "서버 최초 준비"와 "배포"를 그대로 하고(스왑은 생략 가능), 컨테이너가 뜬 뒤 복원한다.

```bash
docker compose exec -T db psql -U postgres taskboard < backup.sql
```

메모리 여유가 생겼으니 `.env`에서 힙을 늘린다.

```
JAVA_OPTS=-Xmx2g
```

`docker-compose.yml`의 `mem_limit`도 함께 올리거나 지운다.

---

## 운영 메모

- 컨테이너 로그는 10MB × 3개로 회전한다. 기본값은 무한히 쌓여 부트 볼륨을 채운다
- `system_logs` 테이블에는 WARN/ERROR만 쌓인다. 오래된 행을 정리하는 배치는 아직 없다
- 현재 `:80` 평문이라 JWT가 그대로 흐른다. 도메인이 생기면 `Caddyfile`의 `:80`을 도메인으로 바꾸고 compose의 443 포트 주석을 풀면 Caddy가 인증서를 자동 발급한다
