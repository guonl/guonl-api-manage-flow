# APIFlow — API 全生命周期管理与流程编排平台

一个开箱即用的 API 管理系统：自动收集接入应用的全部接口、类 Postman 在线调试、接口流程编排（A 接口出参 → B 接口入参自动衔接）、AI 辅助（生成测试数据 / 接口文档 / 编排流程）。

## 功能特性

| 模块 | 能力 |
| --- | --- |
| 工作台 | 项目/接口/流程统计、今日调试概况（成功/失败）、最近调用与最近运行、快捷入口 |
| 项目管理 | 项目与环境（多环境 baseUrl、默认环境）管理，创建项目时自动生成默认环境 |
| 接口收集 | 引入 `api-manage-tools` 的应用通过 actuator `GET /actuator/api-manage` 暴露全部接口元数据，管理端一键同步（新增/更新/下线 + 同步历史） |
| 接口管理 | 分组树、接口详情（入参/出参/Content-Type/标签）、手工新增、关键字检索、接口文档 |
| 接口调试 | 类 Postman：Method/Headers/Query/Body（json、form、x-www-form-urlencoded），代理超时控制，调用历史落库并可回填模板 |
| 流程编排 | 可视化步骤编辑（HTTP / DELAY），`{{var}}` 变量渲染、JSONPath 出参提取、断言（EQUALS/CONTAINS/STATUS/GT…）、失败策略（终止/继续）、初始变量、运行快照与单步重跑、流程导入导出 |
| AI 助手 | OpenAI 兼容协议（Spring AI 2.0.1）：AI 对话、调试页一键生成测试数据、响应结果 AI 分析、接口详情 AI 生成文档、流程编辑器 AI 生成流程（替换/追加） |
| 系统设置 | AI 模型配置（Base URL / API Key 脱敏 / 模型 / 温度）与连通测试、登录密码修改 |

## 技术栈

- 后端：Spring Boot 4.1.1、JDK 17、MyBatis（注解 + XML）、MySQL 8、Spring AI 2.0.1（OpenAI 兼容）
- 前端：Vue 3 + Vite + Element Plus + Pinia + Vue Router（构建产物输出至管理端 static 目录，单 jar 部署）
- 认证：Session（HttpSession，12h），密码 BCrypt

## 模块结构

```
api-manage
├── api-manage-admin    # 管理端（含前端静态资源，可独立运行）
├── api-manage-tools    # 接入工具 jar（宿主应用引入后暴露 /actuator/api-manage 端点）
└── doc                 # 需求/设计文档与建库 SQL
    ├── 01-产品需求文档.md ~ 06-开发计划.md
    └── sql/api-manage-v2.sql
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x

### 2. 建库

执行 [doc/sql/api-manage-v2.sql](doc/sql/api-manage-v2.sql) 创建库表：

```bash
mysql -h<host> -u<user> -p < doc/sql/api-manage-v2.sql
```

### 3. 配置数据源

修改 [api-manage-admin/src/main/resources/application.yml](api-manage-admin/src/main/resources/application.yml) 中 `spring.datasource` 的 url/username/password。也可用启动参数覆盖：

```bash
java -jar api-manage-admin-*.jar --spring.datasource.url='jdbc:mysql://127.0.0.1:3306/db_api_manage?...' --spring.datasource.username=xxx --spring.datasource.password=xxx
```

### 4. 构建与启动

```bash
mvn clean package -DskipTests
java -jar api-manage-admin/target/api-manage-admin-0.0.1-SNAPSHOT.jar --server.port=8081
```

浏览器访问 `http://localhost:8081`，默认账号 `admin / admin123`（首次启动自动初始化，请尽快在「系统设置」中修改密码）。

### 5. 前端开发模式（可选）

```bash
cd api-manage-admin/frontend
npm install
npm run dev     # dev 代理 /api → http://localhost:8081
npm run build   # 产物输出到 api-manage-admin/src/main/resources/static
```

## 接入 api-manage-tools（收集被管理应用的接口）

在需要被收集接口的 Spring Boot 4.x 宿主应用中：

**1）引入依赖**

```xml
<dependency>
    <groupId>com.guonl</groupId>
    <artifactId>api-manage-tools</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

> jar 未发布到远程仓库时，先在工程根目录执行 `mvn install` 安装到本地仓库。

**2）暴露 actuator 端点**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,api-manage
```

**3）编译保留参数名（入参字段解析依赖）**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

**4）验证并同步**

访问宿主应用 `http://<host>:<port>/actuator/api-manage` 应返回接口元数据 JSON 列表。然后在管理端「接口管理 → 同步」即可拉取该应用的全部接口（自动 diff：新增/更新/下线）。

工具包通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动装配，无需额外注解扫描。

## 使用指南

1. **创建项目**：项目管理 → 新建项目（自动生成默认环境），在环境列表中维护各环境 baseUrl
2. **同步接口**：进入项目 → 接口管理 → 同步，拉取接入应用的接口；也可手工新增
3. **调试接口**：接口管理 → 点击接口 → 调试页签，填写参数发送；支持「AI 生成数据」「AI 分析」
4. **编排流程**：流程编排 → 新建流程 → 添加步骤。步骤 URL/参数中使用 `{{var}}` 引用初始变量或上一步提取的变量；在「出参提取」中配置 JSONPath 保存到变量；可加断言与失败策略
5. **AI 生成流程**：流程编辑器 → 「AI 生成流程」输入自然语言需求，生成的步骤可替换或追加
6. **AI 配置**：系统设置 → AI 模型配置，填写 OpenAI 兼容网关的 Base URL（需含 `/v1`）、API Key、模型名，点击「测试连通」

### 流程变量衔接示例

```
步骤1 GET {{baseUrl}}/api/project/create
      出参提取: varName=projectId, jsonPath=$.data.projectId
步骤2 GET {{baseUrl}}/api/api/tree?projectId={{projectId}}
```

## 关键配置项

| 配置 | 说明 | 默认 |
| --- | --- | --- |
| `server.port` | 管理端端口 | 8081 |
| `api-manage.debug.connect-timeout-ms` | 调试代理连接超时 | 5000 |
| `api-manage.debug.read-timeout-ms` | 调试代理读超时 | 30000 |
| `api-manage.debug.max-body-log-size` | 历史记录响应体截断阈值（字节） | 65536 |
| `sys_config: ai.base-url / ai.api-key / ai.model / ai.temperature` | AI 模型配置（页面维护，运行期动态生效） | — |

## 开发文档

- [产品需求](doc/01-产品需求文档.md) / [技术架构](doc/02-技术架构设计.md) / [数据库设计](doc/03-数据库设计.md) / [接口与前端设计](doc/04-接口与前端设计.md) / [开发计划](doc/06-开发计划.md)
- [使用说明](doc/08-使用说明.md)（面向使用者的操作手册与 FAQ） / [测试报告](doc/07-测试报告.md)（54 用例功能覆盖结论）
