#!/usr/bin/env bash
set -euo pipefail

# ==================== 配置变量 ====================
APP_NAME=${APP_NAME:-k-blog-backend}
IMAGE_NAME=${IMAGE_NAME:-k-blog-backend}
CONTAINER_NAME=${CONTAINER_NAME:-$APP_NAME}
CONTAINER_PORT=${CONTAINER_PORT:-8087}
HOST_PORT=${HOST_PORT:-8087}
DOCKERFILE_PATH=${DOCKERFILE_PATH:-Dockerfile}
BUILD_CONTEXT=${BUILD_CONTEXT:-.}
MAVEN_PROFILE=${MAVEN_PROFILE:-prod}
SKIP_TESTS=${SKIP_TESTS:-true}
DOCKER_NETWORK=${DOCKER_NETWORK:-common-net}

# 数据库配置（设置默认值或从 Jenkins 环境变量读取）
SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
SPRING_DATASOURCE_USERNAME=${SPRING_DATASOURCE_USERNAME}
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}

# Jenkins BUILD_NUMBER fallback
BUILD_NUMBER=${BUILD_NUMBER:-local-$(date +%Y%m%d%H%M%S)}

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
log_info(){ echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn(){ echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error(){ echo -e "${RED}[ERROR]${NC} $1"; }

# ==================== 分支验证 ====================
log_info "=== 验证分支 ==="
CURRENT_REF=${ref:-}
[[ -z "${CURRENT_REF}" ]] && CURRENT_REF=${BRANCH_NAME:-}
[[ -z "${CURRENT_REF}" ]] && CURRENT_REF=${GIT_BRANCH:-}

# ==================== 环境信息 ====================
log_info "=== 环境信息 ==="
docker -v || true
log_info "工作目录: $(pwd)"
log_info "当前用户: $(whoami)"
log_info "构建号: ${BUILD_NUMBER}"
log_info "Maven Profile: ${MAVEN_PROFILE}"
log_info "数据库地址: ${SPRING_DATASOURCE_URL}"

# ==================== 停止旧容器 ====================
log_info "=== 停止旧容器 ==="
if docker ps -a --filter "name=^${CONTAINER_NAME}$" --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
  log_info "停止并删除旧容器: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" || true
  sleep 2
else
  log_info "没有运行中的旧容器"
fi

# ==================== Docker 网络 ====================
if [[ -n "${DOCKER_NETWORK}" ]]; then
  log_info "=== 检查 Docker 网络: ${DOCKER_NETWORK} ==="
  if ! docker network inspect "${DOCKER_NETWORK}" >/dev/null 2>&1; then
    log_info "创建 Docker 网络: ${DOCKER_NETWORK}"
    docker network create "${DOCKER_NETWORK}"
  else
    log_info "网络已存在: ${DOCKER_NETWORK}"
  fi
fi

# ==================== 构建镜像 ====================
log_info "=== 构建 Docker 镜像（多阶段） ==="
docker build \
  -f "${DOCKERFILE_PATH}" \
  -t "${IMAGE_NAME}:${BUILD_NUMBER}" \
  -t "${IMAGE_NAME}:latest" \
  --build-arg MAVEN_PROFILE="${MAVEN_PROFILE}" \
  --build-arg SKIP_TESTS="${SKIP_TESTS}" \
  --build-arg BUILD_NUMBER="${BUILD_NUMBER}" \
  "${BUILD_CONTEXT}"

# ==================== 验证镜像 ====================
log_info "=== 验证镜像 ==="
if ! docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "^${IMAGE_NAME}:${BUILD_NUMBER}$"; then
  log_error "镜像构建失败"
  exit 1
fi
log_info "镜像大小: $(docker images --format '{{.Size}}' "${IMAGE_NAME}:${BUILD_NUMBER}")"

# ==================== 运行新容器 ====================
log_info "=== 运行新容器: ${CONTAINER_NAME} ==="
run_args=(
  -d --name "${CONTAINER_NAME}"
  -p "${HOST_PORT}:${CONTAINER_PORT}"
  --restart always
  -e SPRING_PROFILES_ACTIVE="${MAVEN_PROFILE}"
  -e BUILD_NUMBER="${BUILD_NUMBER}"
  -e TZ=Asia/Shanghai
  -e LOG_PATH=/app/logs
  -e SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}"
  -e SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME}"
  -e SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD}"
  -e DB_USERNAME="${SPRING_DATASOURCE_USERNAME}"
  -e DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD}"
)
[[ -n "${DOCKER_NETWORK}" ]] && run_args+=(--network "${DOCKER_NETWORK}")

docker run "${run_args[@]}" "${IMAGE_NAME}:${BUILD_NUMBER}"

# 验证容器启动
sleep 3
if ! docker ps --filter "name=^${CONTAINER_NAME}$" --filter "status=running" | grep -q "${CONTAINER_NAME}"; then
  log_error "容器启动失败！"
  docker logs "${CONTAINER_NAME}"
  exit 1
fi

log_info "容器已启动，ID: $(docker ps -q --filter "name=^${CONTAINER_NAME}$")"

# ==================== 输出状态与日志 ====================
log_info "=== 容器状态 ==="
docker ps --filter "name=${CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

log_info "=== 应用日志（最近 30 行） ==="
docker logs --tail 30 "${CONTAINER_NAME}" || true

# ==================== 清理旧镜像 ====================
log_info "=== 清理旧镜像（保留最近 5 个版本） ==="
old_images=$(docker images --format '{{.Repository}}:{{.Tag}} {{.CreatedAt}}' \
  | grep "^${IMAGE_NAME}:" | grep -v ':latest ' \
  | sort -k2,3r | awk '{print $1}' | tail -n +6 || true)

if [[ -n "${old_images}" ]]; then
  while read -r img; do
    [[ -z "${img}" ]] && continue
    log_info "删除旧镜像: ${img}"
    docker rmi "${img}" || true
  done <<< "${old_images}"
else
  log_info "没有需要清理的旧镜像"
fi

# ==================== 最终报告 ====================
log_info "=========================================="
log_info "部署成功！"
log_info "应用名称: ${APP_NAME}"
log_info "容器名称: ${CONTAINER_NAME}"
log_info "镜像版本: ${IMAGE_NAME}:${BUILD_NUMBER}"
log_info "访问地址: http://服务器IP:${HOST_PORT}"
log_info "健康检查: http://服务器IP:${HOST_PORT}/actuator/health"
log_info "查看日志: docker logs -f ${CONTAINER_NAME}"
log_info "=========================================="
