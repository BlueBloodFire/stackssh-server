# StackSSH Server

[中文文档](./README.zh-CN.md)

`stackssh-server` is the backend service of StackSSH, an AI-assisted SSH operations workspace.

It centralizes SSH execution, terminal session lifecycle, remote file access, AI agent orchestration, and knowledge retrieval on the server side, so security control and operational policy stay in one place instead of being spread across clients.

## What It Does

- Manage SSH connections and terminal sessions
- Execute remote shell commands and stream output
- Provide remote file tree, content, upload, and download APIs
- Run AI-agent conversations for troubleshooting and operations
- Bind AI context to a live terminal session
- Persist auth data, chat history, conversation state, and knowledge metadata
- Serve HTTP, SSE, and WebSocket interfaces for desktop clients

## Product Positioning

StackSSH is not only a remote terminal backend.

It is designed as the execution and control plane of an **AI + SSH operations workbench**:

- The client focuses on interaction and visualization
- The server owns credentials, execution, orchestration, and policy boundaries

This architecture is better suited for production use, team collaboration, future audit workflows, and safer AI-assisted operations.

## Core Capabilities

### SSH Operations

- Create and manage SSH connection records
- Open terminal sessions and execute commands
- Read and write terminal streams
- Handle remote file browsing and file transfer

### AI Agent Workflow

- ReAct-style multi-step conversation flow
- Tool-call aware streaming output
- Terminal-aware troubleshooting and guided execution
- Prompt enrichment from runtime context and conversation state

### Knowledge and Context

- Attach knowledge to servers or agents
- Retrieve relevant knowledge before the first AI round
- Persist summaries, round traces, and session state

### Security and Integration

- JWT-based auth endpoints
- REST APIs for auth, SSH, files, and agent config
- SSE for AI streaming
- WebSocket for terminal interaction

## Typical Use Cases

- Investigate a production issue on a Linux server with AI assistance
- Let an AI agent inspect logs, ports, services, and system health through SSH
- Operate a remote host from a desktop client without exposing SSH credentials to every local machine
- Attach architecture notes or runbooks to a server and inject them into AI context
- Build a safer internal tool for DevOps, SRE, and backend teams

## Why It Is Better Than a Traditional SSH Tool

Traditional SSH tools usually focus on manual terminal access. StackSSH Server adds a server-side control layer.

- Centralized credential and execution control instead of scattered local configuration
- AI-assisted troubleshooting instead of pure manual command entry
- Structured HTTP / WebSocket APIs instead of terminal-only interaction
- Session memory and knowledge retrieval instead of stateless command history
- Easier future extension for audit, approval, and policy enforcement

## Tech Stack

- Java 17
- Spring Boot 3
- MyBatis
- MySQL 8
- Flyway
- Spring Security
- Google ADK / Spring AI
- SSE and WebSocket

## Project Structure

```text
stackssh-server-api             API DTOs and service contracts
stackssh-server-app             Boot application, config, resources, tests
stackssh-server-case            ReAct orchestration use cases
stackssh-server-domain          Domain services and business models
stackssh-server-infrastructure  MyBatis, SSH adapters, security, persistence
stackssh-server-trigger         HTTP and WebSocket controllers
stackssh-server-types           Shared enums and exceptions
```

## Main API Areas

- Auth: login, register
- Agent: session creation, chat streaming, model and tool config
- SSH: create connection, connect, disconnect
- Terminal: open, exec, write, bind agent
- File: tree, content, upload, download
- Knowledge: upload, list, search, delete

## Quick Start

### Requirements

- JDK 17
- Maven 3.8+
- MySQL 8

### Run

```bash
mvn clean package -DskipTests
java -jar stackssh-server-app/target/stackssh-server-app.jar --spring.profiles.active=dev
```

Default port:

- `8091`

Before startup:

- Ensure MySQL is running
- Create database `stackssh`
- Fill in `application-dev.yml` and model-related config

## Recommended Pairing

Use this service together with:

- [`stackssh-client`](https://github.com/BlueBloodFire/stackssh-client)

Together they form the full StackSSH product:

- `stackssh-server`: execution, orchestration, security boundary
- `stackssh-client`: terminal UX, file UI, AI workspace, settings

## License

No license file is currently published in this repository. Add an explicit license before redistribution or external reuse.
