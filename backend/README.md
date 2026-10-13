# PinTu 后端（成员 A 交付物）

> 项目根：`backend/`，与前端 `src/` 同仓库

## 1. 技术栈

- Java 17
- Spring Boot 3.2.x
- Spring Security 6 + JWT
- MyBatis-Plus 3.5.7
- MySQL 8.x
- Flyway 数据库迁移
- Maven 3.8+
- Lombok（编译期注解，IDE 需安装 Lombok 插件）

## 2. 目录结构

```text
backend/
  pom.xml
  src/main/java/com/pintu/
    PintuApplication.java        // 启动类
    common/
      api/Result.java            // 统一返回结构
      exception/
        BusinessException.java   // 业务异常
        GlobalExceptionHandler.java
      util/
        IdGenerator.java         // 业务 ID 生成（沿用前端前缀）
        TimeFormatter.java
    config/CorsConfig.java       // CORS 跨域配置
    controller/                  // REST 控制器
    dto/                         // 请求体 / 视图对象
    entity/                      // MyBatis-Plus 实体
    enums/                       // 业务枚举
    mapper/                      // MyBatis Mapper 接口
    service/
      *Service.java              // 服务接口
      impl/*ServiceImpl.java     // 服务实现
  src/main/resources/
    application.yml              // Spring Boot 配置
    db/migration/
      V1__create_core_schema.sql // 核心表基线
      V2__seed_demo_data.sql     // 演示数据
      V3__normalize_time_and_location_types.sql // 时间与坐标类型规范化
      V4__complete_domain_foundation.sql // 完整领域表、外键、约束与索引
      V5__secure_demo_passwords.sql // BCrypt 演示密码迁移
      V6__normalize_phone_identity.sql // 手机号规范化与唯一索引
  src/test/java/                 // 单元测试
```

## 3. 启动步骤

### 3.1 准备数据库

```sql
CREATE DATABASE pintu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

`backend/src/main/resources/application.yml` 中的默认连接：

```yaml
url: jdbc:mysql://localhost:3306/pintu
username: root
password: test-password
```

建议通过环境变量覆盖连接信息：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

应用启动时由 Flyway 检查并执行尚未应用的版本迁移。已执行的迁移不会重复运行，
重启服务不会删除表或覆盖已有业务数据。

如果本地数据库来自旧版本且已经存在业务表，Flyway 会将其标记为版本 1 基线，
再执行后续迁移。提交后的迁移文件不可修改；结构变更必须新增更高版本脚本。

### 3.2 启动应用

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell 使用 `.\mvnw.cmd spring-boot:run`。项目已包含 Maven Wrapper，无需预先全局安装 Maven。

健康检查：

```bash
curl http://localhost:8080/api/health
# {"code":0,"msg":"ok","data":"alive"}
```

除健康检查、OpenAPI 和 `/api/auth/**` 外，接口必须携带：

```http
Authorization: Bearer <access-token>
```

`POST /api/auth/refresh` 轮换刷新令牌，`POST /api/auth/logout` 主动吊销刷新令牌。

### 3.3 演示账号

| 角色 | 用户名 | 密码 | ID |
| --- | --- | --- | --- |
| 乘客 | passenger_demo | 123456 | passenger-001 |
| 司机 | driver_demo | 123456 | driver-001 |
| 管理员 | admin_demo | 123456 | admin-001 |

## 4. 编码规范

- 包结构：`com.pintu.<层>`，每层职责单一（controller / service / mapper / entity / dto / enums）
- 命名：实体使用大驼峰单数（`OrderRecord`），表名小写下划线（`order_record`）
- 主键：所有业务表使用 `VARCHAR(32)`，与前端 ID 前缀对齐（`passenger-` / `po-` / `trip-` / `req-` / `order-` / `pay-` / `review-`）
- 状态值：枚举对外返回中文字符串，前端无需翻译
- 校验：请求体必须使用 `@Valid` + Jakarta Validation 注解
- 异常：业务异常通过 `BusinessException` 抛出，统一由 `GlobalExceptionHandler` 拦截
- 事务：涉及多表写入的操作使用 `@Transactional(rollbackFor = Exception.class)`
- 日志：使用 `@Slf4j`；敏感字段不进日志
- 时间：数据库使用 `DATETIME`，Java 使用 `LocalDateTime`，API 固定为 `yyyy-MM-dd HH:mm`，业务时区为 `Asia/Shanghai`
- 坐标：经纬度使用 `DECIMAL(10,7)` / `BigDecimal`，避免二进制浮点误差

## 5. 状态契约

订单状态对外返回七阶段，与 [`docs/C组接口需求清单.md`](../docs/C组接口需求清单.md) 一致：

```text
待确认 -> 已确认 -> 履约中 -> 待付款 -> 已完成
                                   \-> 已取消
```

B 组前端 [`src/api/passengerApi.mock.js`](../src/api/passengerApi.mock.js) 已实现到四阶段的映射，无需调整。

## 6. 测试

```bash
cd backend
./mvnw test
```

当前测试覆盖：

- `OrderStatusTest` 覆盖订单状态枚举的全部分支
- `FlywayMigrationTest` 覆盖首次建表、演示数据和重复迁移不清数据
- `FlywayMigrationTest` 同时验证 38 张业务表、核心外键/检查约束和双向评价唯一性
- `IdGeneratorTest` 覆盖业务 ID 生成器的前缀与唯一性
- `OrderServiceImplTest` 覆盖司机身份、付款时机和付款金额校验
- `MatchRequestServiceImplTest` 覆盖重复成单与座位不足校验
- `DriverServiceImplTest` 覆盖车辆归属校验

## 7. 常见问题

- **CORS 报错**：检查前端启动端口是否在 [`CorsConfig`](src/main/java/com/pintu/config/CorsConfig.java) 的白名单里
- **时间字段格式不对**：`JacksonConfig` 统一处理 `LocalDateTime`，请求和响应均使用 `yyyy-MM-dd HH:mm`，前端无需处理 `T` 分隔符
- **Flyway 校验失败**：不要修改已经执行过的迁移文件，应新增更高版本迁移；仅本地无用数据可删除数据库后重建
