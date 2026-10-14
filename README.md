```markdown
# Collaborative Code Editor

A lightweight, multi-client real-time collaborative text and code editor built with Java 17, Swing, raw TCP Sockets, and `RSyntaxTextArea`.

---

## Table of Contents
- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Network Protocol & Synchronization](#network-protocol--synchronization)
- [Key Features](#key-features)
- [Prerequisites](#prerequisites)
- [Build & Run](#build--run)
  - [1. Compile & Package](#1-compile--package)
  - [2. Run Dedicated Server](#2-run-dedicated-server)
  - [3. Run Client Instances](#3-run-client-instances)
  - [4. Run via Maven Plugin](#4-run-via-maven-plugin)
- [Running Unit & Integration Tests](#running-unit--integration-tests)
- [CI/CD Pipeline](#cicd-pipeline)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Collaborative Code Editor enables multiple users to write and edit source code concurrently across network nodes. Changes made on one client are serialized into lightweight diff packets and broadcasted across connected peers with sub-millisecond latency. 

The editor interface leverages `RSyntaxTextArea` to deliver IDE-grade token parsing, syntax highlighting, and line numbering for languages including Java, Python, C++, JavaScript, HTML, and JSON.

---

## System Architecture

The application adopts a **Client-Server Star Topology**:

```text
                  ┌─────────────────────────────────┐
                  │       Central TCP Server        │
                  │       (Port: 8080 default)      │
                  │  - Thread-per-Client Handler    │
                  │  - Non-blocking Broadcast       │
                  └───────────────▲─────────────────┘
                                  │
                   TCP Socket     │   TCP Socket
                   Serialized Obj │   Serialized Obj
                                  │
            ┌─────────────────────┴─────────────────────┐
            │                                           │
 ┌──────────▼──────────┐                     ┌──────────▼──────────┐
 │      Client 1       │                     │      Client 2       │
 │ ┌─────────────────┐ │                     │ ┌─────────────────┐ │
 │ │ EditorFrame     │ │                     │ │ EditorFrame     │ │
 │ │ (RSyntaxTextArea│ │                     │ │ (RSyntaxTextArea│ │
 │ └────────┬────────┘ │                     │ └────────┬────────┘ │
 │          │          │                     │          │          │
 │   Reentrancy Guard  │                     │   Reentrancy Guard  │
 │   (isRemoteUpdate)  │                     │   (isRemoteUpdate)  │
 └─────────────────────┘                     └─────────────────────┘

```

1. **Central Server (`Server.java`)**:
* Listens on a dedicated port via `ServerSocket`.
* Spawns a dedicated `ClientHandler` thread for each connected socket.
* Implements **Echo Suppression**: Broadcasts incoming document mutation packets to all clients *except* the sender who generated the edit.


2. **Client Layer (`Client.java`)**:
* Manages socket lifecycle (`connect`, `send`, `disconnect`).
* Runs a dedicated background listener thread consuming `ObjectInputStream`.
* Dispatches incoming payloads to the UI thread via `SwingUtilities.invokeLater()`.


3. **Reentrancy Protection (`EditorFrame.java`)**:
* Employs a boolean flag (`isRemoteUpdate`) to prevent packet reflection loops. When remote changes are applied to the local Swing document, the local `DocumentListener` is silenced to avoid resending the same edit back to the server.



---

## Project Structure

```text
Collaborative-Code-Editor/
│
├── pom.xml                                 # Maven dependencies, plugins, and build profiles
├── README.md                               # Project documentation & reference
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── editor/
│   │   │           ├── Main.java               # Unified entry point (GUI / CLI)
│   │   │           ├── network/
│   │   │           │   ├── Client.java         # Socket client connection logic
│   │   │           │   └── Server.java         # Multi-threaded broadcast server
│   │   │           ├── sync/
│   │   │           │   └── DocumentSync.java   # Serialized document diff payload
│   │   │           └── ui/
│   │   │               ├── EditorFrame.java    # Swing GUI, status bar, and listeners
│   │   │               └── SyntaxHighlighter.java # Token styles and language switching
│   │   │
│   │   └── resources/
│   │       └── icons/                          # Application icons and static assets
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── editor/
│                   ├── network/
│                   │   └── NetworkIntegrationTest.java # Loopback broadcast test suite
│                   └── sync/
│                       └── DocumentSyncTest.java       # Serialization & state test suite
│
└── .github/
    └── workflows/
        └── build.yml                           # GitHub Actions CI workflow

```

---

## Network Protocol & Synchronization

Every document change is wrapped into a Java Serializable object (`DocumentSync`):

```java
public class DocumentSync implements Serializable {
    public enum ActionType { INSERT, DELETE, REPLACE_ALL }
    private final ActionType actionType;
    private final int offset;
    private final int length;
    private final String text;
}

```

| Action Type | Description | Offset | Length | Text Payload |
| --- | --- | --- | --- | --- |
| `INSERT` | Character(s) typed or pasted | Starting insertion index | Character count | Inserted string |
| `DELETE` | Character(s) removed (backspace/delete) | Starting index of deletion | Removed count | `""` (Empty string) |
| `REPLACE_ALL` | Complete document sync | `0` | Total document length | Full buffer text |

---

## Key Features

* **Real-Time Collaboration**: Changes synchronize across nodes over persistent raw TCP streams.
* **Echo Suppression & Loop Prevention**: Prevents infinite update cascades using client-side flags and sender-excluded server broadcasting.
* **Syntax Highlighting**: On-the-fly syntax formatting for Java, Python, C++, JavaScript, HTML, and JSON.
* **Flexible Launch Modes**: Launch interactive GUI modal, standalone server, or dedicated client via single command-line execution.
* **Automated Testing Suite**: Full headless verification of network broadcasts and serialization routines.

---

## Prerequisites

Ensure your environment meets the minimum version requirements:

* **Java Development Kit (JDK)**: Version 17 or higher
* **Apache Maven**: Version 3.8+

Verify installation:

```powershell
java -version
mvn -version

```

---

## Build & Run

### 1. Compile & Package

Build the complete project and bundle all dependencies into an executable fat JAR:

```powershell
mvn clean package

```

Output JAR: `target/collaborative-code-editor-1.0.0.jar`

### 2. Run Dedicated Server

Start the broadcast server on port `8080`:

```powershell
java -jar target/collaborative-code-editor-1.0.0.jar server 8080

```

### 3. Run Client Instances

Open two or more terminal windows to simulate multiple users:

**Client 1:**

```powershell
java -jar target/collaborative-code-editor-1.0.0.jar client localhost 8080

```

**Client 2:**

```powershell
java -jar target/collaborative-code-editor-1.0.0.jar client localhost 8080

```

### 4. Run via Maven Plugin

Alternatively, execute directly without manual packaging:

```powershell
# Interactive GUI mode
mvn exec:java

# CLI Server
mvn exec:java -Dexec.args="server 8080"

# CLI Client
mvn exec:java -Dexec.args="client localhost 8080"

```

---

## Running Unit & Integration Tests

Run the test suite using Maven:

```powershell
mvn clean test

```

For running headless testing environments explicitly:

```powershell
mvn test -Djava.awt.headless=true

```

To run a specific test:

```powershell
mvn test -Dtest=DocumentSyncTest
mvn test -Dtest=NetworkIntegrationTest

```

---

## CI/CD Pipeline

The project includes an automated GitHub Actions workflow (`.github/workflows/build.yml`):

* Triggers on every `push` and `pull_request` targeting `main` or `master`.
* Sets up Eclipse Temurin JDK 17.
* Runs integration and unit tests in headless mode (`-Djava.awt.headless=true`).
* Packages the fat JAR and uploads it as a workflow artifact.

---

## Troubleshooting

| Issue | Cause | Solution |
| --- | --- | --- |
| `Connection refused: connect` | Server is not running or incorrect port supplied | Start the server first (`server 8080`) before connecting clients. |
| `HeadlessException` in tests | Swing component trying to render without a display | Add `-Djava.awt.headless=true` flag to test commands. |
| Characters duplicating while typing | Reentrancy guard bypassed | Verify that `isRemoteUpdate` is checked inside `DocumentListener`. |
| Port already in use | Another process is bound to port `8080` | Kill the process or specify a different port (e.g., `8090`). |

```