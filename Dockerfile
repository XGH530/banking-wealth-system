# =====================================================================
# 银行记账与理财系统 - Dockerfile
# 多阶段构建: Maven 编译阶段 + JRE 运行阶段
# =====================================================================

# ---- 阶段 1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 先复制 pom.xml 单独下载依赖, 利用 Docker 缓存层
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# 复制源码并编译打包
COPY src ./src
RUN mvn clean package -DskipTests -B && \
    cp target/bank-account-system.jar /build/app.jar

# ---- 阶段 2: JRE 运行 ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 时区设置
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 从构建阶段复制 jar
COPY --from=builder /build/app.jar /app/app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{}' || exit 1

# 启动
ENTRYPOINT ["java", \
    "-Xms256m", "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-jar", "/app/app.jar"]
