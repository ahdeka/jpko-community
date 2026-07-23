# 실행 환경
FROM amazoncorretto:21-alpine

# gif2webp 도구 설치
RUN apk add --no-cache libwebp-tools

# 작업 디렉토리 설정
WORKDIR /app

# jar 파일 복사
COPY jpko-community-0.0.1-SNAPSHOT.jar app.jar

# 포트 오픈
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
