# StackSSH Server

[English](./README.md)

`stackssh-server` 是 StackSSH 的后端服务，用于承载 AI 辅助 SSH 运维工作台的执行与控制能力。

它把 SSH 执行、终端会话管理、远程文件访问、AI Agent 编排、知识检索等能力统一放在服务端，避免把凭据、执行逻辑和风险控制分散到每台客户端机器上。

## 功能概览

- 管理 SSH 连接和终端会话
- 执行远程 Shell 命令并流式返回输出
- 提供远程文件树、文件内容、上传、下载接口
- 提供用于排障和运维操作的 AI Agent 对话能力
- 把 AI 上下文绑定到一个真实终端会话
- 持久化认证数据、对话历史、会话状态和知识库元数据
- 为桌面客户端提供 HTTP、SSE、WebSocket 接口

## 产品定位

StackSSH 并不只是一个远程终端后端。

它更像是一个 **AI + SSH 运维工作台** 的执行平面与控制平面：

- 客户端负责交互体验和可视化
- 服务端负责凭据、执行、编排与安全边界

这种架构更适合生产环境、团队协作、后续审计流程，以及更安全的 AI 辅助运维。

## 核心能力

### SSH 运维能力

- 创建和管理 SSH 连接配置
- 打开终端会话并执行命令
- 读写终端输入输出流
- 处理远程文件浏览与文件传输

### AI Agent 能力

- ReAct 多步推理与执行流程
- 感知工具调用的流式输出
- 结合真实终端上下文做故障排查与操作建议
- 基于运行时上下文和对话状态增强 Prompt

### 知识库与上下文能力

- 为服务器或 Agent 关联知识内容
- 在首轮 AI 对话前检索相关上下文
- 持久化摘要、轮次轨迹和会话状态

### 安全与集成能力

- 基于 JWT 的认证接口
- 面向认证、SSH、文件、Agent 配置的 REST API
- 面向 AI 对话的 SSE 流接口
- 面向终端交互的 WebSocket 接口

## 典型使用场景

- 在生产 Linux 服务器上配合 AI 做故障排查
- 让 AI Agent 通过 SSH 检查日志、端口、进程、服务与系统资源
- 通过桌面客户端运维远程主机，但不把 SSH 凭据散落在每个人本地
- 把架构说明、运行手册、已知问题沉淀到服务器知识库中并注入 AI 上下文
- 为 DevOps、SRE、后端团队搭建一个更安全的内部运维平台

## 相比传统 SSH 工具的优势

传统 SSH 工具通常只强调“手工连上去执行命令”，而 StackSSH Server 在服务端增加了一层控制与编排能力。

- 凭据和执行统一收口，而不是分散在每个本地终端里
- 支持 AI 辅助排障，而不是完全依赖人工手敲命令
- 提供结构化 HTTP / WebSocket 接口，而不只是一个终端窗口
- 支持会话记忆和知识检索，而不是只有无状态命令历史
- 更容易在后续扩展审计、审批、策略控制等企业级能力

## 技术栈

- Java 17
- Spring Boot 3
- MyBatis
- MySQL 8
- Flyway
- Spring Security
- Google ADK / Spring AI
- SSE 与 WebSocket

## 项目结构

```text
stackssh-server-api             API DTO 与服务接口定义
stackssh-server-app             启动模块、配置、资源、测试
stackssh-server-case            ReAct 编排用例层
stackssh-server-domain          领域服务与业务模型
stackssh-server-infrastructure  MyBatis、SSH 适配器、安全、持久化
stackssh-server-trigger         HTTP 与 WebSocket 控制器
stackssh-server-types           通用枚举与异常
```

## 主要 API 范围

- Auth：登录、注册
- Agent：会话创建、流式对话、模型与工具配置
- SSH：创建连接、连接、断开
- Terminal：打开、执行、写入、绑定 Agent
- File：树、内容、上传、下载
- Knowledge：上传、列表、搜索、删除

## 快速启动

### 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8

### 启动方式

```bash
mvn clean package -DskipTests
java -jar stackssh-server-app/target/stackssh-server-app.jar --spring.profiles.active=dev
```

默认端口：

- `8091`

启动前请确保：

- MySQL 已启动
- 已创建数据库 `stackssh`
- 已填写 `application-dev.yml` 与模型相关配置

## 推荐搭配

建议和下面的桌面客户端一起使用：

- [`stackssh-client`](https://github.com/BlueBloodFire/stackssh-client)

两者共同组成完整的 StackSSH 产品：

- `stackssh-server`：执行、编排、安全边界
- `stackssh-client`：终端交互、文件界面、AI 工作台、设置

## License

当前仓库还没有发布明确的许可证。如果你计划对外分发或复用，请先补充正式 License。
