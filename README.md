# 拼图 PinTu 私家车拼车应用

软件工程课程团队项目，使用 Vue 3 与 Spring Boot 实现乘客、司机和管理端，覆盖拼车需求、行程发布、匹配请求、订单履约、评价与审核。支持前端 mock 演示和真实后端两种模式。

## 我的职责

负责需求分析、界面原型设计与前端页面实现。仓库同时保留团队后端、数据库迁移和接口文档，便于复现完整项目；后端与全部团队成果不作为个人独立实现声明。

## 技术栈

- 前端：Vue 3、Vue Router、Vite、Lucide Icons。
- 后端：Java 17、Spring Boot 3.2.5、Spring Security、JWT、MyBatis-Plus。
- 数据与部署：MySQL 8、Flyway、Maven Wrapper、Docker Compose。

当前地图、短信与身份校验支持 mock provider；支付页面与流程用于课程演示，不能据此宣称真实支付接入。规划文档中的待实现条目保留其原状态。

## 先运行前端演示

需要 Node.js 20+ 与 npm，在仓库根目录执行：

```bash
npm ci
npm run dev
```

在 URL 指定 mock 模式，无需启动数据库与后端：

| 角色 | 地址 |
| --- | --- |
| 乘客 | <http://127.0.0.1:5173/?api=mock> |
| 司机 | <http://127.0.0.1:5173/?role=driver&api=mock> |
| 管理员 | <http://127.0.0.1:5173/?role=admin&api=mock> |

未指定 `api=mock` 时，浏览器默认连接真实后端。测试用户和共享状态是虚构演示数据。

## 连接真实后端

配置 MySQL 与 Java 17 环境，按 [后端 README](backend/README.md) 准备数据库、设置环境变量，并执行：

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

后端监听 `8080`；前端 API 地址默认为 `http://127.0.0.1:8080`。Vite 可从根目录 `.env` 加载 `VITE_API_BASE_URL`、`VITE_API_MODE`；Java 直接启动需通过进程环境变量配置，不能仅复制 `.env`。

## Docker Compose

复制 `.env.example` 为 `.env`，填写数据库密码、JWT 密钥与数据加密密钥。可用 Python 生成后两项：

```bash
python -c "import secrets; print(secrets.token_urlsafe(48))"
python -c "import secrets,base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
docker compose up -d --build
```

浏览器访问 <http://localhost:5173>。Compose 注入后端配置；前端构建阶段的模式为 `http`，通过 Nginx 转发请求。

## 验证命令

```bash
npm run build
npm run test:integration
npm run test:http-adapter
npm run test:jkl-ui
# Java 17 环境，backend/ 目录中运行：
./mvnw test
```

前端现有测试主要覆盖 mock 闭环和适配器契约；测试通过不等于真实数据库部署验证。实际执行结果见 [VALIDATION.md](docs/VALIDATION.md)。

## 目录与资料

```text
src/          三角色界面、API 适配层与共享 mock 状态
backend/      Java 后端、Flyway 迁移与现有测试
scripts/      前端验证脚本
docker/       Nginx 配置
docs/         需求、设计、接口、分工及验证记录
```

重点资料：[页面组件拆分](docs/前端页面组件拆分说明.md)、[接口契约](docs/状态与接口契约.md)、[数据库设计](docs/A组数据库设计说明.md)。团队来源见 [SOURCES.md](docs/SOURCES.md)。
