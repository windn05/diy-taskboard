# 프론트 빌드 결과(dist)를 Caddy 이미지에 담는다.
# 정적 파일을 이미지에 넣어두면 서버로 따로 옮길 필요가 없다.
# Caddyfile은 도메인·HTTPS 설정을 서버에서 바로 고칠 수 있게 마운트로 주입한다.
FROM caddy:2-alpine

COPY dist /srv/frontend
