#!/usr/bin/env bash
set -euo pipefail

# SSM Run Command로 EC2에서 root 권한으로 실행됨.
# 인자: $1 = 배포할 앱 이미지 전체 URI

APP_IMAGE="${1:?APP_IMAGE(첫 번째 인자)가 필요합니다}"
REGION="ap-northeast-2"
DEPLOY_DIR="/opt/jpko-community"
HEALTH_URL="http://localhost:8080/api/categories"
MAX_RETRIES=20
SLEEP_SECONDS=5

cd "$DEPLOY_DIR"

# 0. 롤백 대상 확보: 지금 떠 있는 app 컨테이너가 물고 있는 이미지 태그를 미리 기록.
#    최초 배포라 컨테이너가 없으면 빈 문자열(정상 — 롤백 대상 없음으로 처리됨).
PREV_IMAGE="$(docker inspect --format='{{.Config.Image}}' jpko-app 2>/dev/null || true)"

# 1. ECR 로그인 (이미지 pull용). URI 맨 앞의 레지스트리 호스트만 추출.
REGISTRY="${APP_IMAGE%%/*}"
aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

# 2. SSM 파라미터 스토어에서 시크릿을 읽어 .env 생성
get_param() {
  aws ssm get-parameter --name "$1" --with-decryption \
    --query "Parameter.Value" --output text --region "$REGION"
}
  
JWT_SECRET="$(get_param /jpko/prod/JWT_SECRET)"
DB_USER="$(get_param /jpko/prod/DB_USER)"
DB_PASSWORD="$(get_param /jpko/prod/DB_PASSWORD)"
S3_BUCKET="$(get_param /jpko/prod/S3_BUCKET)"
TURNSTILE_SECRET_KEY="$(get_param /jpko/prod/TURNSTILE_SECRET_KEY)"

# .env는 소유자만 읽도록 권한 최소화
umask 077
write_env() {
  # $1 = .env에 쓸 APP_IMAGE (신규 이미지 또는 롤백용 이전 이미지)
  cat > .env <<EOF
APP_IMAGE=$1
JWT_SECRET=${JWT_SECRET}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
S3_BUCKET=${S3_BUCKET}
TURNSTILE_SECRET_KEY=${TURNSTILE_SECRET_KEY}
EOF
}

# 3. 새 이미지로 배포 시도
write_env "$APP_IMAGE"
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

# 4. 헬스체크: /api/categories가 응답할 때까지 폴링 (최대 100초)
healthy=false
for i in $(seq 1 "$MAX_RETRIES"); do
  if curl -sf -m 3 "$HEALTH_URL" > /dev/null; then
    healthy=true
    break
  fi
  echo "헬스체크 대기 중... ($i/$MAX_RETRIES)"
  sleep "$SLEEP_SECONDS"
done

if [ "$healthy" = true ]; then
  echo "헬스체크 통과 — 배포 성공"

  # 불필요한 옛 이미지 정리 (현재 이미지랑 롤백 대상을 제외)
  APP_REPO=${APP_IMAGE%%:*} # 태그 부분 제거
  docker images "$APP_REPO" --format '{{.Repository}}:{{.Tag}}' | while read -r img; do
    if [ "$img" != "$APP_IMAGE" ] && [ "$img" != "$PREV_IMAGE" ]; then
      echo "옛 이미지 삭제: $img"
      docker rmi "$img" || true # 다른 게 참조 중이면 실패할 수 있으나 무시
    fi
  done

  # 그 외 태그 없는 잔여 레이어 정리
  docker image prune -f
  exit 0
fi

# 5. 헬스체크 실패 → 새 이미지의 부팅 실패 로그를 남기고, 가능하면 롤백
echo "::error::헬스체크 실패 — 새 이미지가 정상 기동하지 않음"
docker compose -f docker-compose.prod.yml logs --tail=80 app

if [ -n "$PREV_IMAGE" ] && [ "$PREV_IMAGE" != "$APP_IMAGE" ]; then
  echo "이전 이미지로 롤백 시도: $PREV_IMAGE"
  write_env "$PREV_IMAGE"
  docker compose -f docker-compose.prod.yml up -d

  sleep "$SLEEP_SECONDS"
  if curl -sf -m 3 "$HEALTH_URL" > /dev/null; then
    echo "롤백 성공 — 서비스는 이전 버전(${PREV_IMAGE})으로 복구됨"
  else
    echo "::error::롤백본도 헬스체크 실패 — 수동 개입 필요 (postgres 문제 등 이미지 무관 원인 가능성)"
  fi
else
  echo "롤백 대상 없음(최초 배포) — 서비스가 다운된 상태로 남아있을 수 있음, 수동 확인 필요"
fi

# 롤백 성공 여부와 무관하게 "신규 배포 자체는 실패"이므로 CI에는 실패로 보고
exit 1