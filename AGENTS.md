# AGENTS.md

> **注意：此文件仅供 Codex 本地使用，任何对 AGENTS.md 的修改都不应提交到 GitHub。**
> 该文件已加入 .gitignore，请勿手动 `git add AGENTS.md`。

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 项目概述

stackssh-server 是 AI SSH 智能终端的后端服务，基于 **Spring Boot 3 + Google ADK + DDD** 架构，提供 SSH 连接管理、文件操作、终端会话、AI 智能体对话等能力。

- JDK 17 · Maven 3.8.x · MySQL 8 · Spring Boot 3.4.3
- 默认端口：**8091**（`application-dev.yml`）

---

## 构建 & 运行

```bash
# 编译打包（跳过测试）
mvn clean package -DskipTests

# 启动（dev 配置）
java -jar stackssh-server-app/target/stackssh-server-app-1.0-SNAPSHOT.jar --spring.profiles.active=dev

# 单个测试
mvn test -pl stackssh-server-app -Dtest=ApiTest#testMethodName
```

**启动前提**：MySQL 已启动，数据库 `stackssh` 已创建，`application-dev.yml` 中数据库密码已配置。

---

## 模块结构

```
stackssh-server-api         ← 对外接口 DTO + 服务接口（无实现）
stackssh-server-app         ← 启动类 + 配置 + YAML + 测试
stackssh-server-case        ← 用例层：ReAct 流程编排
stackssh-server-domain      ← 领域层：服务实现 + 模型
stackssh-server-infrastructure ← 基础设施：MyBatis DAO + SSH 适配器
stackssh-server-trigger     ← 触发层：HTTP Controller
stackssh-server-types       ← 通用枚举 + 异常
```

依赖方向：`trigger → cases → domain → infrastructure`，domain 不依赖 infrastructure（通过端口接口反转）。

---

## 关键路径

### AI 对话流（SSE）

```
POST /api/v1/chat_stream
  → AgentServiceController.chatStream()
  → AIAgentReActServiceCase.chatStream()         ← 创建 emitter，异步执行
    → RootNode → AiCallNode → LoopDecisionNode → UserFeedbackNode
```

`AiCallNode` 是核心：调用 Google ADK `runner.runAsync()`，从 `event.actions().stateDelta()` 检测工具结果（ADK 自动执行工具，functionCalls() 为空），发送 SSE 事件给前端。

### Agent 装配（启动时 + 热更新）

```
AiAgentAutoConfig (启动)
  → IArmoryService.acceptArmoryAgents()
  → ArmoryService → RootNode → AiApiNode → ChatModelNode → RunnerNode
```

装配链读取 YAML 配置，构建 `InMemoryRunner + BaseAgent`，注册到 `DefaultArmoryFactory`。热更新通过 `POST /api/v1/agent-config/model/{agentId}` 触发重新装配。

### 意图识别增强（5 Phase，全部已实现）

| Phase | 实现位置 | 说明 |
|-------|---------|------|
| 1 动态 Prompt | `domain/agent/service/prompt/` | MilestoneTracker + DynamicPromptBuilder |
| 2 上下文管理 | `domain/agent/service/context/` | 4 个 Provider + HybridReducer |
| 3 意图识别 | `domain/agent/service/intent/` | RuleIntentClassifier + LLMIntentClassifier |
| 4 意图增强 | `domain/agent/service/enhance/` | SignalExtractor + ContextSearch |
| 5 会话持久化 | `infrastructure/adapter/repository/` | MySQL chat_session/message/milestone |

---

## 关键设计模式

### DDD 端口-适配器

Domain 层只依赖 `ISshTerminalService`、`ISshFilePort` 等**接口**（port），infrastructure 层提供实现（adapter）。新增功能时先在 domain 定义接口，再在 infrastructure 实现。

### 责任链节点（xfg-wrench 框架）

`AbstractArmorySupport` 和 `AbstractAIAgentReActSupport` 继承 `StrategyHandler`，实现 `doApply()` 处理逻辑，`get()` 返回下一个节点。新增 ReAct 节点需继承 `AbstractAIAgentReActSupport` 并注册为 Bean。

### Agent YAML 配置

Agent 通过 `classpath:agent/ssh-agent.yml` 定义，支持：
- `module.ai-api`：LLM API 连接
- `module.chat-model.model`：模型名
- `module.chat-model.tool-mcp-list`：MCP 工具（local/sse/stdio）
- `module.chat-model.tool-skills-list`：Skills
- `module.agents[].instruction`：系统 Prompt

---

## HTTP API 速查

| 分类 | 端点 |
|------|------|
| 智能体 | `GET /api/v1/query_ai_agent_config_list` · `POST /api/v1/create_session` · `POST /api/v1/chat_stream` |
| 模型配置 | `GET/POST /api/v1/agent-config/model/{agentId}` |
| 工具配置 | `GET/POST /api/v1/agent-config/tools/{agentId}` |
| SSH 连接 | `POST /api/v1/ssh/create_connection` · `POST /api/v1/ssh/connect` · `POST /api/v1/ssh/disconnect` |
| 终端 | `POST /api/v1/ssh/terminal/open` · `POST /api/v1/ssh/terminal/exec` · `POST /api/v1/ssh/terminal/write` |
| 文件 | `GET /api/v1/ssh/file/tree` · `GET /api/v1/ssh/file/content` · `POST /api/v1/ssh/file/upload` |
| 智能体绑定 | `POST /api/v1/ssh/agent/bind_terminal` · `POST /api/v1/ssh/agent/unbind_terminal` |

---

## 待实现功能路线图

> 按优先级排列，调用时直接引用对应条目编号。

### A. AI 能力扩展

| # | 功能 | 说明 | 难度 |
|---|------|------|------|
| A1 | **AI Playbook** | 将 AI 对话保存为可复用运维脚本/剧本，支持参数化和一键重放 | 中 |
| A2 | **定时巡检任务** | 配置周期性 AI Agent 任务（如每小时检查磁盘/服务状态），持久化到 MySQL，Spring Scheduler 调度 | 高 |
| A3 | **多智能体并行** | 同时对多台服务器执行 Agent 任务，结果聚合展示，基于现有 ParallelAgentNode 扩展 | 高 |
| A4 | **服务器知识库** | 给每台服务器保存 AI 笔记（架构说明、已知问题），关联 `connection_id`，向量化检索注入 Prompt | 中 |
| A5 | **对话历史搜索** | 跨会话全文搜索历史对话内容，基于 `chat_message` 表 LIKE 或 ES 索引 | 中 |
| A6 | **命令解释模式** | 执行前 AI 解释命令风险和效果，新增 `explain` 意图类型，在 AiCallNode 拦截处理 | 低 |
| A7 | **AI 命令补全** | 用户输入时实时 AI 建议（SSE 接口），基于终端上下文和历史命令 | 中 |

### B. 终端与会话

| # | 功能 | 说明 | 难度 |
|---|------|------|------|
| B1 | **终端录制/回放** | 录制终端操作序列（asciinema 格式），存 MySQL/OSS，支持审计回放 | 中 |
| B2 | **WebSocket 终端** | 替换当前 polling 模式为 WebSocket 双向通信，降低延迟 | 高 |
| B3 | **危险命令拦截** | 可配置黑名单（`rm -rf /` 等），执行前返回警告要求二次确认 | 中 |

### C. 安全与多用户

| # | 功能 | 说明 | 难度 |
|---|------|------|------|
| C1 | **用户认证** | 登录/登出，JWT token，Spring Security，`user` 表，连接数据按 userId 隔离 | 高 |
| C2 | **操作审计日志** | `ssh_session_log` 表已存在，补全写入逻辑：记录所有命令执行、操作者、时间 | 中 |
| C3 | **权限控制** | 连接级别的读/写/执行权限，RBAC 模型，基于 C1 完成后实现 | 高 |
| C4 | **SSH 私钥加密存储** | 私钥内容 AES 加密后存 DB，启动时加载到内存，不落明文磁盘 | 中 |
| C5 | **跳板机/ProxyJump** | `ssh_connection_config` 表已有扩展字段，补全 ProxyJump 连接建立逻辑 | 中 |

### D. 监控与可观测

| # | 功能 | 说明 | 难度 |
|---|------|------|------|
| D1 | **实时监控采集** | 新增 `/api/v1/ssh/monitor/metrics` 接口，执行 `top`/`df`/`free` 并返回结构化数据 | 低 |
| D2 | **日志流式接口** | 新增 SSE 接口 `/api/v1/ssh/log/stream`，持续 `tail -f` 并推送行数据 | 中 |
| D3 | **Webhook 通知** | AI 检测到异常（OOM/磁盘满/服务宕机）时推送 Webhook（钉钉/飞书/Slack）| 中 |

### E. 文件系统

| # | 功能 | 说明 | 难度 |
|---|------|------|------|
| E1 | **远程文件搜索** | 新增 `POST /api/v1/ssh/file/search` 执行 `grep -r`/`find`，流式返回结果 | 低 |
| E2 | **文件 Diff 接口** | 服务端执行 `diff` 或返回两个文件内容供前端 Monaco Diff 展示 | 低 |
| E3 | **压缩包操作** | 新增解压/打包接口，执行 `tar`/`unzip` 命令 | 低 |

---

## 工作规范

- **每次完成一条指令后，立即更新本文件** —— 在"进度记录"节追加
- 大任务拆分为小任务，逐步完成
- Context 接近上限时先 `/compact` 再继续

---

## 进度记录

### C1 用户认证 + B2 WebSocket 终端 ✅（2026-06-11）

#### C1 用户认证（JWT + Spring Security）

**需手动执行 SQL 建表：**
```sql
CREATE TABLE user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL UNIQUE COMMENT '用户唯一ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
  password VARCHAR(128) NOT NULL COMMENT 'BCrypt 加密密码',
  status TINYINT DEFAULT 1 COMMENT '0=禁用 1=启用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**pom 新增依赖：**
- `stackssh-server-app/pom.xml`：`spring-boot-starter-security`、`spring-boot-starter-websocket`
- `stackssh-server-infrastructure/pom.xml`：`spring-boot-starter-security`、`com.auth0:java-jwt`

**新增文件：**
1. `domain/auth/model/entity/UserEntity.java`
2. `domain/auth/adapter/repository/IUserRepository.java`
3. `domain/auth/adapter/port/IPasswordHashPort.java`
4. `domain/auth/adapter/port/ITokenGeneratorPort.java`
5. `domain/auth/adapter/port/ITokenVerifierPort.java`
6. `domain/auth/service/IAuthDomainService.java`
7. `domain/auth/service/AuthDomainService.java`
8. `infrastructure/dao/po/UserPO.java`
9. `infrastructure/dao/IUserDao.java`
10. `infrastructure/adapter/repository/UserRepository.java`
11. `infrastructure/adapter/port/PasswordHashAdapter.java`（BCrypt 实现）
12. `infrastructure/security/JwtUtil.java`（实现 ITokenGeneratorPort + ITokenVerifierPort，auth0 JWT）
13. `infrastructure/security/JwtAuthFilter.java`（OncePerRequestFilter，读 Bearer/token 参数）
14. `infrastructure/config/SecurityConfig.java`（Spring Security，无状态，放行 /auth/** 和 /ws/**）
15. `resources/mybatis/mapper/user_mapper.xml`
16. `api/dto/LoginRequestDTO.java`、`RegisterRequestDTO.java`、`LoginResponseDTO.java`
17. `api/IAuthService.java`
18. `trigger/http/AuthController.java`（POST /api/v1/auth/login、/api/v1/auth/register）

**修改文件：**
- `application-dev.yml`：新增 `jwt.secret` 配置

#### B2 WebSocket 终端

**新增文件：**
1. `app/config/WebSocketConfig.java`（@EnableWebSocket，注册 /ws/terminal）
2. `trigger/websocket/TerminalWebSocketHandler.java`（每 50ms 读 SSH 输出推送，收消息写 SSH 输入，JWT 鉴权）

**架构说明：**
- 终端模式从 HTTP 轮询（GET /terminal/read 每 50ms）升级为 WebSocket（/ws/terminal?sessionId=xxx&token=xxx）
- HTTP /terminal/read 接口仍保留（AI Agent 调用终端时使用）
- WebSocket 握手通过 query 参数传 JWT token（解决 WebSocket 不支持自定义 Header 的问题）

### B 终端与会话功能实现（2026-06-04）

#### B1 终端录制/回放 ✅

**需手动执行 SQL 建表：**
```sql
CREATE TABLE terminal_recording (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id VARCHAR(64) NOT NULL UNIQUE,
  connection_id VARCHAR(64) NOT NULL,
  session_id VARCHAR(64) NOT NULL,
  cols INT DEFAULT 120,
  rows INT DEFAULT 24,
  status TINYINT DEFAULT 0 COMMENT '0=录制中 1=已完成 2=已中断',
  started_at DATETIME,
  ended_at DATETIME,
  duration_ms BIGINT,
  INDEX idx_connection_id (connection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE terminal_recording_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_db_id BIGINT NOT NULL,
  offset_ms BIGINT NOT NULL COMMENT '距录制开始的毫秒偏移',
  data TEXT NOT NULL COMMENT 'Base64 编码的终端输出',
  INDEX idx_recording_db_id (recording_db_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**新增文件：**
1. `infrastructure/dao/po/TerminalRecordingPO.java`
2. `infrastructure/dao/po/TerminalRecordingEventPO.java`
3. `infrastructure/dao/ITerminalRecordingDao.java`
4. `infrastructure/dao/ITerminalRecordingEventDao.java`
5. `infrastructure/adapter/port/TerminalRecordingPort.java`
6. `domain/ssh/adapter/port/ITerminalRecordingPort.java`
7. `domain/ssh/model/entity/TerminalRecordingEntity.java`
8. `resources/mybatis/mapper/terminal_recording_mapper.xml`
9. `resources/mybatis/mapper/terminal_recording_event_mapper.xml`
10. `trigger/http/SshTerminalRecordingController.java`
11. `api/dto/RecordingStartRequestDTO.java`
12. `api/dto/RecordingStopRequestDTO.java`

**修改文件：**
- `ITerminalSessionPort` / `TerminalSessionPort`：新增 `startRecording`/`stopRecording`，输出读取线程同时写入录制 Buffer（Base64 编码）
- `ISshTerminalService` / `SshTerminalService`：新增录制生命周期管理

**API 端点：** `POST /recording/start`, `POST /recording/stop`, `GET /recording/list`, `GET /recording/playback/{recordingId}`

#### B3 危险命令拦截 ✅

**新增文件：**
1. `domain/ssh/model/valobj/DangerousCommandProperties.java`（`@ConfigurationProperties(prefix="terminal.safety")`）
2. `api/dto/CommandCheckRequestDTO.java`
3. `api/dto/CommandCheckResponseDTO.java`

**修改文件：**
- `application-dev.yml`：新增 `terminal.safety.dangerous-commands` 黑名单配置
- `SshTerminalController`：新增 `POST /check-command` 端点，遍历黑名单匹配 contains

### 多标签新建断开已有连接 修复 ✅（2026-06-04）

- **根因**：服务端 `TerminalSessionPort` 用 `activeConnectionSession: Map<connectionId, sessionId>` 强制"一连接一会话"，新 tab 调 `openTerminal` 时直接 `cleanup` 旧 sessionId（关闭 Shell Channel），旧 tab 轮询 3 次失败后调 `disconnect` 断掉整个 SSH 连接
- **修复**：将 `activeConnectionSession` 改为 `connectionSessions: Map<connectionId, Set<sessionId>>`，新开会话不再关闭旧会话，同一 SSH Session 可并发开多个 Shell Channel（JSch 原生支持）；`cleanup` 时只从集合中移除对应 sessionId
- **同步修复** `SshTerminalService.openTerminal()`：去掉遍历 sessionCache 清理同 connectionId 旧会话的逻辑

### 刷新后连接状态异常 修复 ✅（2026-06-04）

- **根因 1（服务端）**：JSch Session 在 JVM 内存中，进程重启后全部丢失，但 DB 里 SSH 连接状态仍为 CONNECTED，前端 `fetchConnections` 拉回来显示绿色已连，实际开终端立刻失败
- **修复 1**：新增 `AppStartupListener.java`，监听 `ApplicationReadyEvent`，启动后执行 `UPDATE ssh_connection SET status=0 WHERE status=1`，将遗留 CONNECTED 状态全部重置
- **根因 2（客户端）**：`connectionStore.disconnect()` API 失败（ECONNREFUSED）时不更新本地状态，导致连接看起来仍"已连接"但终端已断开
- **修复 2**：`disconnect()` 无论 API 成功与否，都立即在本地将连接标记为 `DISCONNECTED`

### SFTP channel is not opened 修复 ✅（2026-06-04）

- **根因**：SSH Session 的底层 TCP 连接因空闲超时或防火墙中断后，`session.isConnected()` 仍返回 `true`（JSch 未感知），导致 `openChannel("sftp")` 拿到 Channel 对象但 `sftp.connect()` 失败（`channel is not opened`）
- **修复 1 — requireSession() 主动探活**：调用 `session.sendKeepAliveMsg()` 发送 SSH keep-alive 包，写入失败则立即 `disconnect()` 并抛 `IllegalStateException("SSH连接已失效，请重新连接")`，在尝试打开 SFTP channel 之前就快速失败
- **修复 2 — getOrOpenSftp() 兜底清理**：捕获 `channel is not opened` / `session is down` 错误时，主动调 `sshSessionPort.disconnect(connectionId)` 清除失效 Session 缓存，再抛 `IllegalStateException`
- 控制器已有 `catch (IllegalStateException e)` → `ILLEGAL_PARAMETER` 响应，前端会显示"连接已失效"提示

### ADK Session not found 修复 ✅（2026-06-04）

- **根因**：`InMemoryRunner.runAsync()` 要求 sessionId 必须先在 ADK 内部 Session Store 注册，但项目只在业务层创建 chat session，从未调用 `runner.sessionService().createSession()`，首次对话就抛 `Session not found`
- **修复**：`AiCallNode.doApply()` 中在 `runner.runAsync()` 之前新增 `ensureAdkSession()` 方法，先调 `sessionService().getSession()` 检查是否存在，不存在则调 `createSession(appName, userId, emptyMap, sessionId)`，并发/异常场景下忽略重复创建错误

### RAG 知识库 ✅（2026-06-10）— 编译通过

**技术栈**：Spring AI `OpenAiEmbeddingModel`（指向 DeepSeek embeddings） + `SimpleVectorStore`（JSON 文件持久化）+ MySQL 元数据

**新建文件：**
1. `docs/dev-ops/mysql/sql/knowledge.sql` — 建表语句
2. `domain/knowledge/adapter/repository/IKnowledgeRepository.java` — 端口接口
3. `domain/knowledge/model/entity/KnowledgeDocumentEntity.java`
4. `domain/knowledge/service/IKnowledgeService.java`
5. `domain/knowledge/service/KnowledgeService.java` — 核心：切块/嵌入/检索
6. `infrastructure/dao/IKnowledgeDocumentDao.java`
7. `infrastructure/dao/po/KnowledgeDocumentPO.java`
8. `infrastructure/adapter/repository/KnowledgeRepository.java`
9. `infrastructure/config/VectorStoreConfig.java` — 注册 EmbeddingModel + SimpleVectorStore Bean
10. `mybatis/mapper/knowledge_document_mapper.xml`
11. `api/dto/KnowledgeDocumentDTO.java`
12. `api/dto/KnowledgeSearchRequestDTO.java`
13. `trigger/http/KnowledgeController.java`

**修改文件：**
- `infrastructure/pom.xml`：新增 `spring-ai-openai`
- `application-dev.yml`：新增 `knowledge.*` 配置块
- `api/dto/ChatRequestDTO.java`：新增 `connectionId` 字段
- `cases/react/node/AiCallNode.java`：注入 `IKnowledgeService`，首轮检索 top-3 chunks 注入 prompt

**API 端点：**
- `POST /api/v1/knowledge/upload` — multipart 上传文档（txt/md）
- `GET  /api/v1/knowledge/list`   — 文档列表（?connectionId=&agentId=）
- `DELETE /api/v1/knowledge/{docId}` — 删除文档
- `POST /api/v1/knowledge/search` — 调试用语义搜索

**RAG 注入点：** `AiCallNode.doApply()` step 5，首轮（step==0）检索相关 chunks，拼入 enrichedMessage 前缀

### Phase 4 意图增强 ✅（2026-06-03）

1. `domain/agent/model/valobj/enhance/ExtractedSignals.java`
2. `domain/agent/model/valobj/enhance/SearchContext.java`
3. `domain/agent/service/IIntentEnhancerService.java`
4. `domain/agent/service/enhance/SignalExtractor.java`
5. `domain/agent/service/enhance/ContextSearch.java`
6. `domain/agent/service/enhance/IntentEnhancerService.java`
7. `PromptContextVO` 新增 `SearchContext` 字段
8. `DynamicPromptBuilder` 新增服务状态/配置/日志渲染
9. `IPromptService` / `PromptService` 新增 `SearchContext` 参数
10. `AiCallNode` 注入 `IIntentEnhancerService`，首轮执行增强
