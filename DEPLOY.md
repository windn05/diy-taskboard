# 배포 · 운영

오라클 클라우드 무료 VM(VM.Standard.E2.1.Micro — 1 OCPU / 1GB / Ubuntu 24.04)에 Docker Compose로 올린다.

구조와 그렇게 정한 이유는 [프로젝트_기획서.md](프로젝트_기획서.md) 4.3절에 있다. 이 문서는 **절차**만 다룬다.

---

## 구조

빌드는 **GitHub Actions에서만** 한다. 1GB VM에서 Gradle을 돌리면 OOM이 난다.

```
main에 push
  → Actions: 백엔드 테스트 + jar 빌드, 프론트 lint + dist 빌드
  → 멀티 아키텍처(amd64/arm64) 이미지 2개를 GHCR에 push
서버
  → docker compose pull && docker compose up -d
```

프론트 `dist`는 Caddy 이미지 안에 들어간다. 서버로 정적 파일을 따로 옮기지 않는다.

서버의 git 저장소에는 **설정 파일만** 있으면 된다(`docker-compose.yml`, `Caddyfile`). 애플리케이션 코드는 이미지 안에 있다.

---

## 서버 최초 준비 (Ubuntu 24.04)

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

### 방화벽 — 두 곳을 다 열어야 한다

OCI는 **보안 목록과 호스트 방화벽**이 따로 논다. 콘솔에서만 열고 끝내면 접속이 안 되는데 원인을 찾기 어렵다.

1. 콘솔: 네트워킹 → VCN → 서브넷 → 보안 목록 → 수신 규칙 추가
   - 소스 CIDR `0.0.0.0/0`, 프로토콜 TCP, **대상 포트** 80 (소스 포트는 비워둔다)
   - SSH(22)는 가능하면 본인 IP `x.x.x.x/32`로 좁힌다
2. 호스트: `REJECT` 규칙 **앞에** 삽입해야 한다

```bash
sudo iptables -L INPUT --line-numbers
```

출력에서 `REJECT` 줄의 번호를 확인해 아래 `6` 자리에 넣는다.

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT && sudo netfilter-persistent save
```

`netfilter-persistent save`를 빠뜨리면 재부팅 때 원복된다.

---

## 배포

```bash
git clone https://github.com/windn05/diy-taskboard.git && cd diy-taskboard
```

시크릿 값을 만든다(두 번 실행해 서로 다른 값을 쓴다).

```bash
openssl rand -base64 48
```

```bash
cp .env.example .env && nano .env
```

`JWT_SECRET`은 반드시 바꾼다. 기본값이 저장소에 공개돼 있어 그대로 두면 누구나 관리자 토큰을 위조할 수 있다.

```bash
docker compose pull && docker compose up -d
```

```bash
docker compose ps
```

`http://<공인IP>` 로 접속되면 성공이다.

> GHCR 패키지가 private이면 pull 전에 `read:packages` 권한의 PAT로 `docker login ghcr.io -u <계정>`을 해야 한다. GitHub 패키지 설정에서 public으로 바꾸면 로그인 없이 받는다.

### 최초 관리자

공개 회원가입이 없으므로 계정은 관리자만 만들 수 있는데, 새 DB에는 그 관리자가 없다. 그래서 **계정이 하나도 없을 때만** `.env`의 `BOOTSTRAP_ADMIN_USERNAME` / `BOOTSTRAP_ADMIN_PASSWORD`로 최초 관리자를 만든다.

로그인한 뒤 헤더에서 비밀번호를 바꾸고 `.env`에서 그 두 줄을 지운다. 이후 계정은 관리자 앱의 **계정 관리** 탭에서 만든다.

### 스키마 고정

테이블이 만들어졌으면 애플리케이션이 더 이상 스키마를 바꾸지 않게 낮춘다.

```bash
echo 'JPA_DDL_AUTO=validate' >> .env && docker compose up -d
```

엔티티가 추가되는 변경을 배포할 때는 한 번 `update`로 되돌려 기동한 뒤 다시 `validate`로 올린다.

---

## 배포 갱신

코드만 바뀐 push라면 두 줄이면 된다.

```bash
docker compose pull && docker compose up -d
```

`docker-compose.yml`이나 `Caddyfile`이 바뀐 push라면 저장소도 받는다.

```bash
git pull && docker compose pull && docker compose up -d
```

자동 배포(Watchtower, Actions에서 SSH 등)는 도입하지 않았다. Actions에서 SSH로 배포하려면 서버 22번을 GitHub의 넓은 IP 대역에 열어야 해서, 방화벽을 좁혀둔 것과 상충한다. 배포 빈도가 낮아 수동으로 충분하다.

---

## HTTPS 켜기

Let's Encrypt는 **IP 주소로는 인증서를 발급하지 않는다.** 호스트 이름이 하나 필요하다.

- 가입 없이 바로: `sslip.io` / `nip.io` — `129-225-171-106.sslip.io` 처럼 IP를 그대로 이름으로 쓴다
- 무료 서브도메인: DuckDNS — `이름.duckdns.org`
- 소유한 도메인: A 레코드를 서버 공인 IP로 지정

**1. 443 포트를 연다** — 80과 마찬가지로 콘솔과 호스트 양쪽이다.

콘솔에서 수신 규칙(0.0.0.0/0, TCP, 대상 포트 443)을 추가한 뒤:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT && sudo netfilter-persistent save
```

**80은 닫지 않는다.** 인증서 발급 검증이 80으로 오고, HTTP→HTTPS 전환도 80에서 받아 넘긴다.

**2. `.env`에 도메인을 넣는다**

```bash
echo 'SITE_ADDRESS=taskboard.example.com' >> .env
```

**3. 다시 올린다**

```bash
docker compose up -d && docker compose logs -f caddy
```

로그에 `certificate obtained successfully`가 보이면 끝이다. 발급은 보통 수십 초 걸린다.

```bash
curl -I https://taskboard.example.com
```

인증서는 `caddy_data` 볼륨에 남아 재기동해도 다시 받지 않는다. 갱신도 Caddy가 알아서 한다.

> 프론트는 `window.location.protocol`을 보고 `ws`/`wss`를 고르므로(`api/stomp.ts`), HTTPS로 바뀌면 WebSocket도 자동으로 `wss`가 된다. 고칠 것이 없다.
>
> 도메인을 넣은 뒤에는 `http://<공인IP>` 로는 사이트가 뜨지 않는다. Caddy가 그 이름으로만 사이트를 열기 때문이다.

---

## 환경변수

| 변수 | 필수 | 설명 |
|---|---|---|
| `DB_PASSWORD` | ✅ | Postgres 비밀번호 |
| `SITE_ADDRESS` | | 도메인. 넣으면 HTTPS 자동 전환, 비우면 `:80` 평문 |
| `JWT_SECRET` | ✅ | 32바이트 이상. **기본값을 그대로 쓰면 안 된다** |
| `BOOTSTRAP_ADMIN_USERNAME` | | 계정이 0건일 때만 쓰인다 |
| `BOOTSTRAP_ADMIN_PASSWORD` | | 위와 같음. 8자 이상 |
| `JPA_DDL_AUTO` | | 기본 `update`. 최초 배포 후 `validate` 권장 |
| `JAVA_OPTS` | | 기본 `-Xmx320m -Xss256k -XX:MaxMetaspaceSize=96m` (1GB 기준) |
| `SWAGGER_ENABLED` | | `docker-compose.yml`에서 `false`로 고정 |

---

## 인스턴스 중지 · 재시작

껐다 켜도 **따로 설정할 것은 없다.** 컨테이너는 `restart: unless-stopped`라 부팅 시 자동으로 뜨고, DB 볼륨·스왑(`/etc/fstab`)·방화벽 규칙(`netfilter-persistent`)은 모두 디스크에 남는다.

중지 전에 Docker가 부팅 시 자동 시작되는지만 확인해 둔다.

```bash
systemctl is-enabled docker
```

켠 뒤 확인(1/8 OCPU라 기동에 2~3분 걸린다):

```bash
docker compose ps && curl -I http://localhost
```

공인 IP는 임시(ephemeral) 주소다. 중지/시작에서는 유지되지만, 켠 뒤 콘솔에서 한 번 확인하는 편이 안전하다. 고정이 필요하면 예약된 공인 IP로 전환한다.

---

## 백업

```bash
docker compose exec -T db pg_dump -U postgres taskboard > ~/backup-$(date +%F).sql
```

`.env`도 따로 보관한다. `DB_PASSWORD`를 잃으면 기존 `pgdata` 볼륨을 열 수 없다.

---

## 더 큰 인스턴스로 옮기기

A1.Flex(ARM, 최대 4 OCPU / 24GB)를 확보하면 그쪽으로 옮기는 게 좋다. 이미지가 arm64로도 빌드돼 있어 재빌드가 필요 없다.

기존 서버에서 논리 덤프를 뜬다. **`pgdata` 볼륨을 통째로 복사하면 안 된다** — PostgreSQL 데이터 디렉터리는 아키텍처에 의존해서 x86 → ARM 복사는 깨진다.

```bash
docker compose exec -T db pg_dump -U postgres taskboard > backup.sql
```

새 서버에서 위 "서버 최초 준비"와 "배포"를 그대로 하고(스왑은 생략 가능), 컨테이너가 뜬 뒤 복원한다.

```bash
docker compose exec -T db psql -U postgres taskboard < backup.sql
```

메모리 여유가 생겼으니 `.env`에서 힙을 늘리고, `docker-compose.yml`의 `mem_limit`도 올리거나 지운다.

```
JAVA_OPTS=-Xmx2g
```

---

## 운영 메모

- 컨테이너 로그는 10MB × 3개로 회전한다. 기본값은 무한히 쌓여 부트 볼륨을 채운다
- `system_logs` 테이블에는 WARN/ERROR만 쌓인다. 오래된 행을 정리하는 배치는 아직 없다
- `SITE_ADDRESS`를 비워두면 `:80` 평문이라 JWT가 그대로 흐른다. 위 "HTTPS 켜기"를 적용하면 해소된다
