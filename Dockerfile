## 前后端一体镜像（k3s 部署用）：前端 dist 打进 Spring Boot static，由后端 8087 端口统一提供
## 构建上下文为仓库根目录：docker build -f Dockerfile .
## 注：docker 部署方式（deploy.yml）仍使用 backend/Dockerfile，前端单独发布

## ==================== 第一阶段：前端构建 ====================
FROM node:22-alpine AS frontend

WORKDIR /frontend

# 1) 先装依赖（利用缓存）
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

# 2) 复制源码并构建
COPY frontend/ ./
RUN npm run build

## ==================== 第二阶段：Maven 构建 ====================
FROM crpi-g3stl1c9yrlh5x39.cn-beijing.personal.cr.aliyuncs.com/kk09/maven:latest AS builder

# Maven 配置（阿里云镜像）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings>' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>aliyun</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>central</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <url>https://maven.aliyun.com/repository/public</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

WORKDIR /build

ARG MAVEN_PROFILE=prod
ARG SKIP_TESTS=true
ARG BUILD_NUMBER

ENV MAVEN_OPTS="-Xmx2048m -Xms1024m \
    --add-opens=java.base/java.util=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.text=ALL-UNNAMED \
    --add-opens=java.desktop/java.awt.font=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    --add-opens=java.base/java.nio=ALL-UNNAMED"

# 1) 复制 POM（利用缓存）
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B || true

# 2) 复制后端源码，并把前端产物放入 classpath:/static
COPY backend/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static

# 3) 构建打包
RUN mvn -B -q -T 1C clean package \
    -DskipTests=${SKIP_TESTS} \
    -P${MAVEN_PROFILE} \
    -Dmaven.compiler.forceJavacCompilerUse=true \
    -Dmaven.compiler.source=21 -Dmaven.compiler.target=21 \
    -Dmaven.compiler.parameters=true

# 4) 处理产物（删除 original，复制主 jar 到根）
RUN find target -name "*-original.jar" -type f -delete && \
    cp $(ls -1 target/*.jar | head -n 1) /app.jar

## ==================== 第三阶段：运行时镜像 ====================
FROM crpi-g3stl1c9yrlh5x39.cn-beijing.personal.cr.aliyuncs.com/kk09/openjdk:21-jdk

LABEL authors="kk"
ARG BUILD_NUMBER
LABEL build.number=${BUILD_NUMBER}

WORKDIR /app

# 使用阿里云源并安装必要工具（tzdata + curl）
RUN sed -ri 's|http://deb.debian.org/debian|https://mirrors.aliyun.com/debian|g' \
           /etc/apt/sources.list.d/debian.sources && \
    sed -ri 's|http://security.debian.org/debian-security|https://mirrors.aliyun.com/debian-security|g' \
           /etc/apt/sources.list.d/debian.sources && \
    apt-get clean && \
    apt-get update && \
    apt-get install -y --no-install-recommends tzdata curl && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    dpkg-reconfigure -f noninteractive tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 非 root 用户运行
RUN groupadd -r spring && useradd -r -g spring spring && \
    mkdir -p /app/logs /app/config && chown -R spring:spring /app

COPY --from=builder --chown=spring:spring /app.jar /app/app.jar

USER spring:spring

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    PARAMS="" \
    SPRING_PROFILES_ACTIVE="prod" \
    TZ="Asia/Shanghai"

EXPOSE 8087

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom $JAVA_OPTS -jar /app/app.jar $PARAMS"]
