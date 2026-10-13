# PinTu 后端 Dockerfile（多阶段构建）
# 第一阶段：使用 Maven + JDK 17 编译
# 第二阶段：使用 JRE 运行时镜像，体积更小

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# 缓存依赖：先复制 pom.xml 单独跑一次依赖解析
COPY pom.xml .
RUN mvn -B -e -q dependency:go-offline

COPY src ./src
RUN mvn -B -e -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 创建非 root 运行用户
RUN groupadd -r pintu && useradd -r -g pintu pintu \
    && mkdir -p /app/logs /app/data \
    && chown -R pintu:pintu /app

COPY --from=build /workspace/target/pintu-backend.jar /app/app.jar

USER pintu
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

# Use Bash's TCP support; the JRE base image does not guarantee wget is installed.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080; printf "GET /api/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3; head -n 1 <&3 | grep -q " 200 "' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
