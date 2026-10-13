# 验证记录

## 已完成

- Node.js 24.18.0 下 `npm ci` 成功；Vite 5.4.21 生产构建成功，转换 1613 个模块。
- `npm run test:integration` 通过，覆盖 mock 平台的跨角色请求与订单流程。
- `npm run test:http-adapter` 通过主要认证与订单流程。测试夹具没有配置部分新增接口，运行会输出 404 并使用适配器回退结果；不把这些接口视为已完整验证。
- 旧 `test:jkl-ui` 检查依赖已删除函数名，现改为验证四个路由的应用入口和四个 Vue 模块转换，检查通过。它没有启动浏览器，不属于完整端到端交互测试。
- `docker compose config --quiet` 通过；配置样例中的旧服务器地址改为 localhost。
- Maven Wrapper 脚本、`.mvn/` 和数据库迁移均保留；`backend/mvnw` 在 Git 中标记为可执行。
- 文档本地链接和 Git 提交清单检查通过。

## 验证边界

当前机器可用 Java 为 8，后端要求 Java 17，没有运行 Maven 后端测试。没有启动 MySQL/Redis 或构建 Compose 镜像，也没有验证真实外部服务。

测试安装的 node_modules、dist 已移到仓库外，上传副本保持干净；克隆后需重新执行 `npm ci`。
