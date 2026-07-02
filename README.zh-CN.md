# StackSSH Server

[English](./README.md)

`stackssh-server` 是 StackSSH 的后端服务，也是一个面向 AI 辅助运维场景的 SSH 执行与控制中台。

它在服务端统一承接 SSH 执行、终端会话生命周期、远程文件访问、AI 智能体编排和知识检索能力，把安全边界、凭据管理和执行策略集中在服务端，而不是分散在每个客户端本地。

## 项目作用

- 管理 SSH 连接和终端会话
- 执行远程 Shell 命令并流式返回输出
- 提供远程文件树、文件内容、上传、下载接口
- 运行面向故障排查和运维任务的 AI Agent 对话
- 将 AI 上下文绑定到真实终端会话
- 持久化认证信息、聊天历史、会话状态和知识库元数据
- 为桌面客户端提供 HTTP、SSE 和 WebSocket 接口

## 产品定位

StackSSH 不只是一个远程终端后端。

它被设计成一个 **AI + SSH 运维工作台** 的执行与控制平面：

- 客户端负责交互与可视化
- 服务端负责凭据、执行、编排和安全边界

这种结构更适合真实生产环境、团队协作以及后续扩展审计、审批和策略控制能力。

## 核心能力

### SSH 运维能力

- 创建和管理 SSH 连接记录
- 打开终端会话并执行命令
- 读写终端输入输出流
- 浏览远程文件并进行文件传输

### AI Agent 工作流

- 基于 ReAct 的多步对话流程
- 支持工具调用感知的流式输出
- 结合终端上下文进行故障排查与引导执行
- 使用运行时上下文和会话状态增强 Prompt

### 知识与上下文

- 将知识资料绑定到服务器或智能体
- 在 AI 首轮回答前检索相关知识
- 持久化摘要、轮次轨迹和会话状态

### 安全与集成

- 基于 JWT 的认证接口
- 提供认证、SSH、文件、Agent 配置等 REST API
- 通过 SSE 提供 AI 流式输出
- 通过 WebSocket 提供终端交互能力

## 典型使用场景

- 在 Linux 服务器上结合 AI 辅助进行故障排查
- 让 AI Agent 通过 SSH 检查日志、端口、服务和系统状态
- 在不把 SSH 凭据分发到每台本地电脑的前提下远程操作服务器
- 为服务器附加架构说明、Runbook 或知识文档，并注入 AI 上下文
- 为 DevOps、SRE、后端或平台团队构建更安全的内部运维工具

## 相比传统 SSH 工具的优势

传统 SSH 工具通常只关注人工终端访问，StackSSH Server 额外提供了服务端控制层：

- 凭据与执行能力集中管理，而不是分散在本地
- 支持 AI 辅助排障，而不是纯手工敲命令
- 提供结构化 HTTP / WebSocket 接口，而不是只有终端
- 具备会话记忆和知识检索，而不是无状态命令历史
- 更容易扩展审计、审批和策略控制能力

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
stackssh-server-api             API DTO 与服务契约
stackssh-server-app             启动模块、配置、资源、测试
stackssh-server-case            ReAct 编排用例
stackssh-server-domain          领域服务与业务模型
stackssh-server-infrastructure  MyBatis、SSH 适配器、安全与持久化
stackssh-server-trigger         HTTP 与 WebSocket 控制器
stackssh-server-types           共享枚举与异常
```

## 主要接口域

- Auth：登录、注册
- Agent：会话创建、聊天流式输出、模型与工具配置
- SSH：创建连接、连接、断开
- Terminal：打开、执行、写入、绑定 Agent
- File：目录树、内容、上传、下载
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
- 已填写 `application-dev.yml` 和模型相关配置

## 推荐搭配

建议和下面的桌面客户端一起使用：

- [`stackssh-client`](https://github.com/BlueBloodFire/stackssh-client)

两者共同组成完整的 StackSSH 产品：

- `stackssh-server`：执行、编排、安全边界
- `stackssh-client`：终端交互、文件界面、AI 工作台、设置

## License

本项目使用 Apache License 2.0 授权，请查看 [LICENSE](./LICENSE) 文件。
