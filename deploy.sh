#!/usr/bin/env bash
set -euo pipefail

# SSM Run Command로 EC2에서 root 권한으로 실행됨.
# 인자: $1 = 배포할 앱 이미지 전체 URI

APP_IMAGE="${1:?APP_IMAGE(첫 번째 인자)가 필요합니다}"
REGION="ap-northeast-2"
DEPLOY_DIR="/opt/jpko-community"

cd "$DEPLOY_DIR"

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

# .env는 소유자만 읽도록 권한 최소화
umask 077
cat > .env <<EOF
APP_IMAGE=${APP_IMAGE}
JWT_SECRET=${JWT_SECRET}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
S3_BUCKET=${S3_BUCKET}
EOF

# 3. 새 이미지 pull 후 컨테이너 교체 (down이 아니라 up -d → postgres는 유지, app만 재생성)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d

# 4. 미사용 이미지 정리
docker image prune -f