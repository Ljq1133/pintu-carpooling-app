# 后端编码规范

> 适用范围：`backend/` 目录下的全部 Java 代码
> 与前端规范的关系：本规范不约束前端代码，前端编码规范见 B / C 组各自文档

## 1. 工程结构

```text
com.pintu
├── PintuApplication        // 启动类，仅放 main 方法
├── common                  // 通用基础设施
│   ├── api                 // 统一返回结构 Result
│   ├── exception           // 业务异常 + 全局异常处理
│   └── util                // 通用工具类，禁止业务逻辑
├── config                  // Spring 配置类（CORS、序列化等）
├── controller              // REST 控制器，仅做参数收集与结果返回
├── dto                     // 请求体 / 视图对象，与 Entity 解耦
├── entity                  // 数据库实体
├── enums                   // 业务枚举
├── mapper                  // MyBatis Mapper 接口
└── service
    ├── *Service            // 服务接口
    └── impl                // 服务实现
```

> 跨层调用方向只允许 `controller -> service -> mapper`，反向调用一律禁止。

## 2. 命名约定

| 元素 | 规则 | 示例 |
| --- | --- | --- |
| 包 | 全小写，按层划分 | `com.pintu.controller` |
| 类 | 大驼峰，名词性 | `OrderRecord`、`PassengerOrderService` |
| 方法 | 小驼峰，动词开头 | `createTrip`、`listByDriver` |
| 常量 | 全大写下划线分隔 | `DRIVER_CREDIT_MIN` |
| 枚举值 | 全大写 | `OrderStatus.CONFIRMED` |
| DTO | `*Request` 表示输入，`*VO` 表示输出 | `LoginRequest`、`UserVO` |
| 表名 | 小写下划线，单数 | `order_record`、`match_request` |

## 3. 注释

- 类与公共方法使用 JavaDoc，说明职责与边界；私有辅助方法可省略
- 注释回答 **为什么**，不是 **是什么**
- 禁止保留过时注释（已删除字段、被替换逻辑）

```java
/**
 * 司机同意请求：事务性地创建正式订单、扣减座位、回写需求和请求状态。
 */
@Transactional(rollbackFor = Exception.class)
public MatchRequestVO accept(String requestId) { ... }
```

## 4. 异常处理

- 业务可预期错误：抛 `BusinessException(code, msg)`
- 校验错误：使用 Jakarta Validation 注解（`@NotBlank`、`@NotNull`、`@Positive`），由 `GlobalExceptionHandler` 自动捕获
- 永远不要 `catch (Exception e) { return null; }`，要么处理掉，要么继续向上抛

错误码区段：

| 区间 | 含义 |
| --- | --- |
| 0 | 成功 |
| 1001~1099 | 业务规则错误 |
| 1404 | 资源不存在 |
| 5000 | 服务器内部错误 |

## 5. 事务

- 涉及 **多张表写入** 的方法必须加 `@Transactional(rollbackFor = Exception.class)`
- 事务方法内部不要再调用其他事务方法（避免事务传播复杂）
- 校验异常应该在事务方法第一行抛出，避免做了一半再回滚

## 6. 校验

控制器入口必须使用 `@Valid`：

```java
@PostMapping("/orders")
public Result<PassengerOrderVO> create(@RequestBody @Valid PassengerOrderCreateRequest req) { ... }
```

DTO 字段使用注解：

```java
@NotBlank(message = "出发地不能为空")
private String start;

@NotNull
@Positive(message = "乘车人数必须大于 0")
private Integer passengerCount;
```

## 7. 数据库

- 主键统一 `VARCHAR(32)`，业务侧通过 `IdGenerator.next(prefix)` 生成
- 时间字段：业务侧字符串使用 `departureTime`（`yyyy-MM-dd HH:mm`）；审计字段 `created_at` / `updated_at` 用 `LocalDateTime`，由 MySQL 默认值维护
- 状态字段：用中文字符串，与前端枚举对齐
- 逻辑删除：所有业务表加 `deleted TINYINT`，由 MyBatis-Plus 的 `@TableLogic` 处理
- 索引：高频查询字段（`status`、`driver_id`、`passenger_id` 等）建立二级索引

## 8. 日志

- 类上 `@Slf4j`，禁止 `System.out.println`
- 业务异常 `log.warn`，未预期异常 `log.error("..", ex)`（带堆栈）
- 不在日志输出敏感字段（密码、token、手机号原文）

## 9. 提交与分支

- 每个特性单独分支：`feature/a-<short-desc>`
- 提交信息格式：`<type>(<scope>): <subject>`，常见 type：`feat` / `fix` / `docs` / `refactor` / `test`
- 单次提交聚焦单一目的，不要把无关变更混进同一 commit
- PR 描述列出：变更点 / 测试方式 / 联调影响

## 10. 测试

- 工具类、枚举、纯函数 -> JUnit5 单元测试
- 服务层 -> 配合 H2 内存数据库或 Mockito 做路径测试
- 控制器 -> `@SpringBootTest` + `MockMvc`，覆盖主线流程
- 测试类与被测类同包，命名 `<被测>Test`

## 11. 安全（MVP 阶段限制）

- 当前未接 JWT，请求头 `X-User-Id` 仅用于演示，不做信任校验
- 数据库密码、生产 URL 不进仓库
- 输入数据由 Validation + Service 双重校验，避免依赖前端按钮禁用
