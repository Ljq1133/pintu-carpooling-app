# MNO 阶段交付说明（A 组）

> 对应任务清单：`PinTu-功能补齐任务清单.md`
> 范围：阶段 M（前端工程化）、N（模块化与可观测性）、O（部署与配置）的可在当前窗口落地的子集
> 约束：仅新增文件或在已有文件中追加，不重构 B/C 已交付的页面与服务

## 1. ABCDEFGHIJKL 完成情况审计

按代码现状与任务清单中的勾选状态对照：

| 阶段 | 清单标记 | 代码实测 | 备注 |
| --- | --- | --- | --- |
| A 基线整合与范围冻结 | 9/9 | ✅ 已完成 | OpenAPI 已挂入 springdoc，需求追踪矩阵和状态契约文档存在 |
| B 数据库与基础设施 | 16/16 | ✅ 已完成 | Flyway 迁移已到 `V13`，覆盖核心表、外键、索引、敏感字段加密 |
| C 认证授权 | 14/14 | ✅ 已完成 | Spring Security + JWT + BCrypt + 验证码 + 限流 + 审计事件均已落地 |
| D 用户中心 / 实名 / 车辆 | 12/12 | ✅ 已完成 | 实名申请、附件、审核、车辆 CRUD、掩码工具 `Masking.java` 已在用 |
| E 地图与价格预估 | 9/10 | ⚠️ 大部分完成 | E-05 标记进行中（地图可视化点选未做），其余已完成 |
| F 匹配引擎 | 13/14 | ⚠️ 大部分完成 | F-12 Redis GEO 仍标记进行中 |
| G 协商聊天 / 双向确认 | 0/12 | ⚠️ 部分落地 | `NegotiationController` / `NegotiationServiceImpl` 已支持会话、消息、提案、双方确认；但 G-03 WebSocket 文字聊天、G-11 协商超时定时任务仍缺 |
| H 订单生命周期 | 0/12 | ⚠️ 部分落地 | `OrderStatusHistory` 表与状态机已实现 H-01/H-04/H-05；H-08 取消信用扣分、H-11 乐观锁、H-12 幂等键未落地 |
| I 支付 / 评价 / 信用 | 0/13 | ⚠️ 部分落地 | mock 付款、评价、管理员信用调整已实现；I-01 支付网关适配器、I-09 7 天默认好评、I-10 信用规则引擎未落地 |
| J 安全 / 隐私 / 行程保障 | 0/12 | ⚠️ 部分落地 | `Masking` 与 `GeoPrivacy` 已在用，黑名单表存在；虚拟号、紧急联系人、报警、轨迹推送未做 |
| K 个人中心 / 客服 / 申诉 | 0/7 | ❌ 未落地 | 申诉表 `complaint` 在迁移中未见，客服会话未建 |
| L 管理后台完整化 | 0/15 | ⚠️ 部分落地 | 审核、订单看板、信用调整已有；规则配置/版本/灰度、申诉工单、运营统计未做 |

**结论**：A→F 基本对齐清单；G/H/I 仅落地了主线最小切片，二次确认事务、协商超时、支付网关接口、信用规则引擎、申诉、虚拟号、轨迹推送、运营统计等仍是缺口。这些不在本次 MNO 范围内，但建议在合并前由对应负责人补齐。

## 2. 本轮新增内容（MNO 子集）

### 2.1 阶段 N：可观测性与模块化

| 任务 | 文件 | 说明 |
| --- | --- | --- |
| N-08 | [`backend/.../common/trace/TraceIdFilter.java`](../backend/src/main/java/com/pintu/common/trace/TraceIdFilter.java) | 最高优先级过滤器，分配 / 延续 `X-Trace-Id`，写入 SLF4J MDC，写回响应头 |
| N-08 | [`common/trace/TraceContext.java`](../backend/src/main/java/com/pintu/common/trace/TraceContext.java) | 在异常响应、领域事件中读取当前 TraceId 的入口 |
| N-08 | [`common/api/Result.java`](../backend/src/main/java/com/pintu/common/api/Result.java) | 错误响应体追加 `traceId`，成功响应体省略（`@JsonInclude(NON_EMPTY)`）|
| N-09 | `pom.xml` | 新增 `spring-boot-starter-actuator` 依赖 |
| N-09 | [`application.yml`](../backend/src/main/resources/application.yml) | 暴露 `health/info/metrics/loggers`，加固 endpoint 访问 |
| N-09 | [`SecurityConfig.java`](../backend/src/main/java/com/pintu/config/SecurityConfig.java) | `/actuator/health/**` 公开，其余 actuator 端点要求 ADMIN |
| N-09 | [`logback-spring.xml`](../backend/src/main/resources/logback-spring.xml) | 结构化日志，`%X{traceId}` 占位符，`prod` profile 下滚动写文件 |
| N-11 | [`common/logging/SensitiveDataMaskingConverter.java`](../backend/src/main/java/com/pintu/common/logging/SensitiveDataMaskingConverter.java) | 兜底脱敏：自动屏蔽日志中的 password/token/phone/idCard/secret |
| N-03 | [`common/event/OrderStatusChangedEvent.java`](../backend/src/main/java/com/pintu/common/event/OrderStatusChangedEvent.java) | 订单状态变化领域事件 |
| N-03 | [`common/event/OrderStatusAuditListener.java`](../backend/src/main/java/com/pintu/common/event/OrderStatusAuditListener.java) | 默认审计监听器：事务提交后异步记录结构化日志 |
| N-03 | [`config/AsyncConfig.java`](../backend/src/main/java/com/pintu/config/AsyncConfig.java) | 启用 `@Async`，为事件监听与未来异步任务提供线程池 |

> **接入说明**：业务侧只需要 `@Autowired ApplicationEventPublisher` 然后 `publisher.publishEvent(new OrderStatusChangedEvent(this, orderId, from, to, operatorId, reason))`，事务提交后会自动触发监听器。本轮没有改动 `OrderServiceImpl`，待对应负责人评估后接入。

### 2.2 阶段 O：部署、配置与数据保护

| 任务 | 文件 | 说明 |
| --- | --- | --- |
| O-01 | [`backend/Dockerfile`](../backend/Dockerfile) | 多阶段构建：maven 编译 → JRE 运行；非 root 用户；HEALTHCHECK；`MaxRAMPercentage` |
| O-01 | [`Dockerfile`](../Dockerfile) | 前端多阶段构建：Node 编译 → Nginx 静态服务 |
| O-01 | [`docker/nginx.conf`](../docker/nginx.conf) | SPA fallback、静态资源缓存、`/api` 与 `/ws` 反代到后端容器 |
| O-01 | [`backend/.dockerignore`](../backend/.dockerignore) / [`.dockerignore`](../.dockerignore) | 缩小构建上下文 |
| O-02 | [`docker-compose.yml`](../docker-compose.yml) | MySQL + Redis + 后端 + 前端，一条命令起；prod profile；env 必填校验 |
| O-04 | [`application-dev.yml`](../backend/src/main/resources/application-dev.yml) | 开发：默认密码、debug 日志、放宽 CORS、暴露调试验证码 |
| O-04 | [`application-test.yml`](../backend/src/main/resources/application-test.yml) | 自动化测试：H2 内存、关闭 Flyway |
| O-04 | [`application-prod.yml`](../backend/src/main/resources/application-prod.yml) | 生产：DB / JWT / 数据加密密钥必须显式给，CORS 必填 |
| O-04 | [`application.yml`](../backend/src/main/resources/application.yml) | 默认 profile 改为 `dev`，删除 `test-password` 兜底值 |
| O-05 | [`config/CorsConfig.java`](../backend/src/main/java/com/pintu/config/CorsConfig.java) | 白名单从 `pintu.cors.allowed-origins` 读取，逗号分隔，默认仅含本地 |
| O-08 | [`config/ConfigurationGuard.java`](../backend/src/main/java/com/pintu/config/ConfigurationGuard.java) | `ApplicationReadyEvent` 时校验 JWT/数据加密密钥/DB 密码/CORS；prod 缺失直接抛异常终止 |
| O-09 | [`scripts/reset-demo-data.sql`](../scripts/reset-demo-data.sql) | 演示前手工执行的数据重置脚本，不再依赖应用重启清库 |

> 阶段 N 中 N-04（事务 Outbox）、N-05（RabbitMQ）、N-06（Redis 限流缓存）、N-07（独立 WebSocket 模块）属于结构性引入，建议作为独立 PR 完成；当前 docker-compose 已经预留 Redis 服务以备 N-06 接入。

### 2.3 阶段 M：前端工程化与交互质量

| 任务 | 文件 | 说明 |
| --- | --- | --- |
| M-14 | [`.env.example`](../.env.example) | 重新整理为「后端必填」「前端构建参数」「外部服务」三段；显式区分 `VITE_` 前缀 |
| M-14 | [`src/config.js`](../src/config.js) | 集中暴露 `apiBaseUrl / apiMode / mapProvider / features`，支持运行时通过 `window.__PINTU_RUNTIME_CONFIG__` 覆盖 |
| M-06 | [`src/composables/useAsyncState.js`](../src/composables/useAsyncState.js) | 通用异步状态封装：loading / error / data / lastUpdated / 防重入 run |
| M-07 | [`src/components/ui/ConfirmDialog.vue`](../src/components/ui/ConfirmDialog.vue) | 通用二次确认弹窗，支持 danger 样式与 loading 状态，给取消/付款/封禁/仲裁等不可逆操作复用 |

> M 阶段的大头是 M-03（拆分 2539 行乘客组件）和 M-04（DriverWorkspace/AdminWorkspace 路由化）。本轮没有动这两项，原因是改动面太大，会与 B/C 当前页面冲突；建议作为单独 PR、与 B 协同进行。
> 当前已有 `src/router.js`、`src/auth/session.js`，意味着 M-01（Vue Router）已落地；M-02（Pinia 状态层）暂未引入，可参照 `useAsyncState` 的模式逐步迁移。

## 3. 验收建议

### 3.1 后端

```bash
cd backend
./mvnw -DskipTests package           # 验证 pom.xml 与依赖
./mvnw spring-boot:run               # dev profile 起服务
curl -i http://localhost:8080/api/health
# 关注响应头中是否带 X-Trace-Id

# 故意触发错误，验证 traceId 出现在响应体
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{}'
```

### 3.2 一键 Docker 启动

```bash
cp .env.example .env
# 在 .env 中填 DB_PASSWORD / JWT_SECRET / DATA_ENCRYPTION_KEY（≥32 字符）
docker compose up -d --build
docker compose exec backend curl -s http://127.0.0.1:8080/api/health
```

### 3.3 演示前数据重置

```bash
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" pintu' < scripts/reset-demo-data.sql
```

### 3.4 配置守卫验证

```bash
# 应当报错并退出
SPRING_PROFILES_ACTIVE=prod \
  JWT_SECRET=dev-only-pintu-jwt-secret-change-me-2026 \
  ./mvnw spring-boot:run
```

## 4. 风险与未尽事项

1. **OrderServiceImpl 尚未发布领域事件**：本轮只完成了事件 + 监听器骨架，业务侧接入由订单负责人决定时机。
2. **Actuator 在生产可被滥用**：当前限定 ADMIN 角色；若部署到公网，建议通过运维网关再加一层 IP 白名单。
3. **logback `%mask` 的正则是兜底措施**，并非灵丹妙药；业务代码仍需遵守"不打敏感字段"的基线。
4. **Docker 镜像未做依赖锁层缓存极致优化**：当前的多阶段构建在频繁 push 时仍可能重复下载依赖；可后续接入仓库 cache。
5. **前端 ConfirmDialog 未接入 ESC / 焦点陷阱**：演示足够，A11Y 验收建议作为 M-13 的一部分补齐。
6. **G/H/I/K/L 缺口未在本次解决**：审计章节中已点名，建议作为下一里程碑追踪项。
