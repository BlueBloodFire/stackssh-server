# StackSSH Server

`stackssh-server` is the backend of StackSSH, an AI-assisted SSH workspace for server operations.

It combines SSH session management, terminal execution, remote file access, AI agent orchestration, and knowledge retrieval in a single service, so risk control and operational auditing stay on the server side instead of being scattered across clients.

## What It Does

- Manage SSH connections, sessions, and terminal lifecycle
- Execute remote shell commands and stream terminal output
- Browse, read, upload, and download remote files
- Run AI agent conversations for operations and troubleshooting
- Bind AI context to a live terminal session
- Store chat history and conversation state in MySQL
- Support RAG-style knowledge injection for server-specific context
- Expose HTTP and WebSocket APIs for desktop clients

## Product Positioning

StackSSH is not just a terminal emulator.

It is designed as an **AI + SSH operations workbench**:

- The **client** focuses on interaction, editing, and visualization
- The **server** owns SSH credentials, command execution, AI orchestration, and policy control

This architecture is better suited for team usage, controlled environments, and future audit / approval workflows.

## Core Capabilities

### 1. SSH Operations

- Create and manage SSH connections
- Open terminal sessions
- Execute commands through terminal or AI agent flow
- Read and write terminal input/output
- Handle remote file tree, content, upload, and download

### 2. AI Agent Workflow

- ReAct-style multi-step execution
- Tool-call aware streaming responses
- Terminal-aware troubleshooting and guided operations
- Prompt enrichment from runtime context
- Intent recognition and context compression

### 3. Knowledge & Context

- Attach operational knowledge to a server or agent
- Retrieve relevant context before the first AI round
- Persist session state, summaries, and round traces

### 4. Client Integration

- REST APIs for auth, SSH, files, and agent config
- SSE / streaming chat interface
- WebSocket terminal channel

## Tech Stack

- Java 17
- Spring Boot 3
- MyBatis
- MySQL 8
- Flyway
- Spring Security
- Google ADK / Spring AI
- WebSocket + SSE

## Project Structure

```text
stackssh-server-api             API DTOs and service contracts
stackssh-server-app             Boot application, config, resources, tests
stackssh-server-case            ReAct orchestration use cases
stackssh-server-domain          Domain services and core business models
stackssh-server-infrastructure  MyBatis, SSH adapters, security, persistence
stackssh-server-trigger         HTTP / WebSocket controllers
stackssh-server-types           Shared enums and exceptions
```

## Key API Areas

- Auth: login, register
- Agent: session creation, chat streaming, model/tool config
- SSH: connection create, connect, disconnect
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

Before startup, make sure:

- MySQL is running
- Database `stackssh` exists
- `application-dev.yml` and agent-related model config are filled in

## Recommended Pairing

This repository is the backend service. For the desktop workbench, use:

- `stackssh-client`

Together they form the full StackSSH product:

- `stackssh-server`: execution, orchestration, security boundary
- `stackssh-client`: terminal UX, file UI, AI workspace, settings

## Current Direction

The project is evolving toward:

- safer AI-assisted operations
- multi-session remote collaboration
- auditable command execution
- richer server knowledge context
- an IDE-like SSH experience for infrastructure work

## License

No license file is currently published in this repository. If you plan to reuse or distribute it, add an explicit license first.
