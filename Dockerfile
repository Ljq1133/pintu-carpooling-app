# PinTu 前端 Dockerfile（多阶段构建）
# 第一阶段：使用 Node 构建
# 第二阶段：使用 Nginx 提供静态资源

FROM node:20-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

COPY . .
ARG VITE_API_BASE_URL=
ARG VITE_API_MODE=http
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL VITE_API_MODE=$VITE_API_MODE
RUN npm run build

FROM nginx:1.27-alpine
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ || exit 1
