# Collaborative Code Editor

A lightweight, real-time collaborative code editor built with **Java 17, Swing, TCP Sockets, and RSyntaxTextArea**. It enables multiple clients to edit code simultaneously with live document synchronization, color-coded remote carets, password-protected rooms, syntax highlighting for 30 programming and markup languages, and an integrated code execution console. Additional features include starter templates, persistent document storage with a 30-day retention policy, and on-demand local data deletion.

---

## 🌐 Live Demo & Repository

- 🔗 **Live Demo:** [Collaborative Code Editor](https://dhananjaychaudhary99.github.io/Portfolio/)
- 📂 **Source Code:** [GitHub Repository](https://github.com/dhananjaychaudhary99/Collaborative-Code-Editor)

> **Note:** The application is a Java Swing desktop application. The portfolio link serves as a project showcase rather than a browser-based interactive editor.

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Network Protocol and Synchronization](#network-protocol-and-synchronization)
- [Key Features](#key-features)
- [Supported Languages](#supported-languages)
- [Prerequisites](#prerequisites)
- [Build and Packaging](#build-and-packaging)
- [Automation Scripts](#automation-scripts)
- [Manual Execution](#manual-execution)
- [Running Tests](#running-tests)
- [Troubleshooting and Windows Tips](#troubleshooting-and-windows-tips)
- [Technical Highlights](#technical-highlights)
- [Future Improvements](#future-improvements)

---

## Overview

The Collaborative Code Editor is a Java-based desktop application designed to support real-time collaborative programming over TCP connections. Multiple clients connect to a central server and share document changes within the same collaboration room.

The application combines a Swing-based graphical interface with a socket-based networking layer, enabling peers to exchange document updates, synchronize cursor positions, and work independently in password-protected rooms.

### How It Works

1. The server accepts incoming client connections and manages collaboration rooms.
2. Clients connect to the server using a configurable host and port.
3. Local document changes are represented as synchronization messages and sent to the server.
4. The server broadcasts updates to other clients in the same room.
5. Remote changes are applied to each editor while suppressing redundant local synchronization events.
6. The default room's document can be persisted locally with timestamp-based retention.

The project demonstrates Java desktop development, network programming, event-driven user interfaces, document synchronization, process execution, and automated testing.

---

## System Architecture

```text
                  ┌──────────────────────────────┐
                  │       Central TCP Server     │
                  │                              │
                  │  Client Connection Manager   │
                  │  Room Management             │
                  │  Password Authentication     │
                  │  Message Broadcasting        │
                  │  Document Persistence        │
                  └──────────────┬───────────────┘
                                 │
                     TCP Socket Connections
                                 │
                  ┌──────────────┴───────────────┐
                  │                              │
          ┌───────▼────────┐             ┌───────▼────────┐
          │    Client 1    │             │    Client 2    │
          │                │             │                │
          │ Swing Editor   │             │ Swing Editor   │
          │ Syntax Support │             │ Syntax Support │
          │ Code Runner    │             │ Code Runner    │
          │ Remote Carets  │             │ Remote Carets  │
          └───────┬────────┘             └───────┬────────┘
                  │                              │
          Local Document Events          Local Document Events
                  │                              │
          Remote Update Guard            Remote Update Guard
```

### Core Components

**1. Central Server — `Server.java`**

- Accepts client connections over TCP.
- Maintains connected clients and organizes them into isolated rooms.
- Validates room passwords during room-joining requests.
- Broadcasts document updates to other clients in the same room.
- Integrates with document persistence for the default room.

**2. Network Client — `Client.java`**

- Manages socket connections and disconnections.
- Sends document synchronization messages to the server.
- Processes incoming messages on a dedicated listener thread.
- Dispatches UI-related updates to Swing's Event Dispatch Thread using `SwingUtilities.invokeLater()`.

**3. Document Synchronization — `DocumentSync.java`**

- Defines serializable messages for document edits, cursor movements, room changes, and authentication responses.
- Carries the information required to apply document changes at the appropriate offsets.
- Provides a common message format for communication between clients and the server.

**4. Desktop Interface — `EditorFrame.java`**

- Provides the main code editor and application controls.
- Integrates syntax highlighting, room management, remote cursor rendering, and code execution.
- Prevents remote document updates from being retransmitted as local edits.

**5. Persistence — `DocumentStorage.java`**

- Manages local document storage and timestamp metadata.
- Implements a 30-day retention policy.
- Supports explicit deletion of stored data.

**6. Code Execution — `CodeRunner.java`**

- Starts external processes for supported programming languages.
- Captures execution output for display in the application console.
- Applies an execution timeout to help prevent indefinitely running programs.

---

## Project Structure

```text
Collaborative-Code-Editor/
│
├── data/
│   ├── saved_code.txt
│   └── saved_meta.properties
│
├── src/
│   ├── main/
│   │   ├── java/com/editor/
│   │   │   ├── Main.java
│   │   │   │
│   │   │   ├── network/
│   │   │   │   ├── Client.java
│   │   │   │   └── Server.java
│   │   │   │
│   │   │   ├── sync/
│   │   │   │   ├── CodeRunner.java
│   │   │   │   ├── DocumentStorage.java
│   │   │   │   └── DocumentSync.java
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── EditorFrame.java
│   │   │       ├── RemoteCaretPainter.java
│   │   │       └── SyntaxHighlighter.java
│   │   │
│   │   └── resources/
│   │       └── icons and themes
│   │
│   └── test/
│       └── java/com/editor/
│           ├── network/
│           │   └── NetworkIntegrationTest.java
│           │
│           └── sync/
│               ├── DocumentStorageTest.java
│               └── DocumentSyncTest.java
│
├── start.bat
├── stop.bat
├── launch_peer2.bat
└── pom.xml
```

### Important Files

| File | Responsibility |
|---|---|
| `Main.java` | Application entry point and server/client startup |
| `Server.java` | TCP server, client management, and room-based broadcasting |
| `Client.java` | Socket communication and incoming message handling |
| `DocumentSync.java` | Serializable synchronization message model |
| `EditorFrame.java` | Swing editor interface and user interactions |
| `SyntaxHighlighter.java` | Language-specific syntax highlighting |
| `RemoteCaretPainter.java` | Remote caret visualization |
| `CodeRunner.java` | External process execution |
| `DocumentStorage.java` | Persistence, retention, and data deletion |
| `pom.xml` | Maven dependencies, build configuration, and packaging |

---

## Network Protocol and Synchronization

The application uses Java Serialization over TCP sockets to exchange structured synchronization messages through `DocumentSync`.

### Message Types

```java
public enum ActionType {
    INSERT,
    DELETE,
    REPLACE_ALL,
    CURSOR_MOVE,
    CLIENT_DISCONNECT,
    JOIN_ROOM,
    AUTH_SUCCESS,
    AUTH_FAILED
}
```

### Protocol Reference

| Action | Purpose | Important Payload Fields |
|---|---|---|
| `INSERT` | Insert text into a document | `offset`, `length`, `text`, `clientId` |
| `DELETE` | Delete document content | `offset`, `length`, `clientId` |
| `REPLACE_ALL` | Replace or initialize the document | `text`, document length |
| `CURSOR_MOVE` | Synchronize caret positions | `clientId`, `cursorPosition` |
| `CLIENT_DISCONNECT` | Notify the server about a disconnected client | Client identification |
| `JOIN_ROOM` | Request entry into a room | `roomId`, `roomPassword`, `clientId` |
| `AUTH_SUCCESS` | Confirm successful room authentication | `text` containing the confirmed room ID |
| `AUTH_FAILED` | Report failed room authentication | `text` containing the error message |

### Preventing Update Loops

When a client receives a remote document update, applying that change can trigger the editor's local `DocumentListener`. Without protection, the same change could be sent back to the server and retransmitted unnecessarily.

The application uses an `isRemoteUpdate` guard to distinguish remote changes from local edits.

```text
Local Edit
    │
    ▼
Document Listener
    │
    ▼
Send Synchronization Message
    │
    ▼
Server Broadcast
    │
    ▼
Remote Client Receives Update
    │
    ▼
Enable Remote Update Guard
    │
    ▼
Apply Document Change
    │
    ▼
Disable Remote Update Guard
```

This approach helps prevent synchronization loops and redundant broadcasts.

**Synchronization note:** This protocol provides change propagation, but concurrent edits at the same position may require additional conflict-resolution logic. Operational latency and synchronization guarantees should be measured under realistic network conditions before being described as sub-millisecond performance.

---

## Key Features

### 1. Real-Time Collaborative Editing

- Supports multiple connected editor clients.
- Propagates document changes through a central TCP server.
- Supports insertion, deletion, and complete document replacement messages.
- Displays color-coded remote carets to distinguish collaborators.

### 2. Syntax Highlighting for 30 Languages

Uses `RSyntaxTextArea` for syntax-aware editing and language-specific highlighting.

### 3. Integrated Code Runner

- Executes supported code through external processes.
- Displays output in an integrated console.
- Supports execution timeouts.
- Uses locally installed language runtimes rather than requiring a remote execution service.

### 4. Password-Protected Collaboration Rooms

- Supports separate collaboration rooms.
- Validates passwords when joining protected rooms.
- Prevents normal message broadcasting across different rooms.

### 5. Persistent Document Storage

- Stores the default room's document locally.
- Records timestamp metadata.
- Applies a 30-day retention policy.
- Provides an option to delete local stored data.

### 6. Native Desktop Experience

- Uses Java Swing for the graphical interface.
- Supports launching the application through Windows batch scripts.
- Can start Java GUI processes using `javaw` to avoid an additional console window when configured accordingly.

---

## Supported Languages

The editor is configured for syntax highlighting and starter templates for the following 30 languages and formats:

| No. | Language / Format | No. | Language / Format |
|---:|---|---:|---|
| 1 | Java | 16 | Perl |
| 2 | Python | 17 | Lua |
| 3 | C++ | 18 | Dart |
| 4 | C | 19 | SQL |
| 5 | C# | 20 | HTML |
| 6 | JavaScript | 21 | CSS |
| 7 | TypeScript | 22 | JSON |
| 8 | Rust | 23 | XML |
| 9 | Go | 24 | YAML |
| 10 | Kotlin | 25 | Markdown |
| 11 | Scala | 26 | Bash / Shell |
| 12 | Ruby | 27 | Batch |
| 13 | PHP | 28 | Properties |
| 14 | Groovy | 29 | Assembly (x86) |
| 15 | ActionScript | 30 | Dockerfile |

**Note:** Syntax highlighting, starter-template availability, and executable support are different capabilities. The integrated runner is described as supporting Java, Python, and JavaScript; actual execution depends on the installed runtimes and the project's implementation.

---

## Prerequisites

Install the following tools before building or running the application:

- **Java Development Kit:** JDK 17 or compatible supported version.
- **Apache Maven:** Version 3.8 or later.
- **Python:** Optional; required to run Python programs.
- **Node.js:** Optional; required to run JavaScript programs through Node.js.

Verify the installations in PowerShell:

```powershell
java -version
javac -version
mvn -version
```

Check optional runtimes:

```powershell
python --version
node --version
```

---

## Build and Packaging

Open PowerShell in the project root directory.

### 1. Compile and package the application

```powershell
mvn clean package
```

This builds the project and runs the configured tests before packaging, unless the Maven configuration changes that behavior.

To package without running tests:

```powershell
mvn clean package "-Dmaven.test.skip=true"
```

### 2. Locate the generated JAR

Expected output path:

```text
target/collaborative-code-editor-1.0.0.jar
```

The exact filename depends on the artifact name and version configured in `pom.xml`.

### 3. Run the application

Use the commands in the manual execution section below.

---

## Automation Scripts

The project includes Windows batch scripts for managing local development and multi-client testing.

### Start the Server and Primary Client

```powershell
.\start.bat
```

Starts the server and opens the primary editor window, according to the script configuration.

### Launch a Second Client

```powershell
.\launch_peer2.bat
```

Opens another editor instance for testing synchronization between clients.

### Stop the Application

```powershell
.\stop.bat
```

Stops the processes managed by the script.

**Important:** Process termination should target only the application's own processes. Avoid using broad commands that forcibly terminate every `java` or `javaw` process, as they may stop unrelated applications.

---

## Manual Execution

The server and clients can be launched independently from PowerShell.

### Start the Server

```powershell
java -jar target\collaborative-code-editor-1.0.0.jar server 8080
```

### Start the First Client

Open a second terminal:

```powershell
java -jar target\collaborative-code-editor-1.0.0.jar client localhost 8080
```

### Start Another Client

Open another terminal and run:

```powershell
java -jar target\collaborative-code-editor-1.0.0.jar client localhost 8080
```

For clients running on different computers, replace `localhost` with the server machine's reachable IP address or hostname. Ensure the server is listening on an appropriate network interface and the configured port is permitted by the firewall.

---

## Running Unit and Integration Tests

Run the full test suite:

```powershell
mvn clean test
```

Run the document-storage tests:

```powershell
mvn test "-Dtest=DocumentStorageTest"
```

Run the network integration tests:

```powershell
mvn test "-Dtest=NetworkIntegrationTest"
```

Run the serialization tests:

```powershell
mvn test "-Dtest=DocumentSyncTest"
```

For environments without a graphical display:

```powershell
mvn test "-Djava.awt.headless=true"
```

Headless mode is appropriate only if the tests do not require an actual Swing display. GUI-dependent tests may need to be skipped or adapted for headless execution.

---

## Troubleshooting and Windows Tips

| Issue | Possible Cause | Solution |
|---|---|---|
| `Unable to access jarfile` | The JAR does not exist or the path is incorrect | Run `mvn clean package` and verify the JAR under `target/`. |
| `Non-parseable POM` | Invalid XML or an encoding issue in `pom.xml` | Inspect the XML declaration and remove any unintended characters before it. |
| `illegal character: '\ufeff'` | An unexpected byte-order mark or encoding issue in a source file | Save the file using a consistent encoding, preferably UTF-8 without a BOM where required. |
| `Unknown lifecycle phase ".test.skip=true"` | A Maven argument was parsed incorrectly | Quote the argument: `"-Dmaven.test.skip=true"`. |
| `Port 8080 already in use` | Another process is using the configured port | Identify the process with `Get-NetTCPConnection` and stop only the relevant process. |
| Client cannot connect | Server is not running, the address is wrong, or a firewall blocks access | Check the server, hostname, port, and firewall configuration. |
| Remote changes are not synchronized | Connection failure, incorrect room membership, or synchronization logic errors | Verify both clients' connections and room settings; inspect server and client logs. |
| Code execution times out | A program runs indefinitely or blocks | Check the program's behavior and verify the configured timeout. |
| Python or JavaScript execution fails | The required runtime is missing or unavailable on `PATH` | Install the runtime and verify it with `python --version` or `node --version`. |
| GUI does not appear | Startup failure, Java configuration, or an exception during initialization | Launch the application from a terminal to inspect error messages. |

### Check Whether Port 8080 Is in Use

```powershell
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, State, OwningProcess
```

Inspect a process by its ID:

```powershell
Get-Process -Id <PROCESS_ID>
```

Replace `<PROCESS_ID>` with the actual numeric process ID.

---

## Technical Highlights

| Area | Technologies / Concepts |
|---|---|
| Programming Language | Java 17 |
| Desktop GUI | Java Swing |
| Code Editing | RSyntaxTextArea |
| Networking | TCP Sockets |
| Message Transport | Java Serialization |
| Concurrency | Background listener threads and Swing Event Dispatch Thread |
| Synchronization | Document events and remote-update guards |
| Room Management | Client grouping and password validation |
| Process Execution | Java `ProcessBuilder` or equivalent subprocess handling |
| Persistence | Local files and timestamp metadata |
| Build Tool | Apache Maven |
| Testing | Unit tests and network integration tests |

---

## Future Improvements

Potential enhancements to make the project more robust and production-ready:

- **Conflict Resolution:** Introduce operational transformation or a CRDT-based model to handle simultaneous edits more reliably.
- **Protocol Security:** Add transport encryption, stronger authentication, and safer message validation.
- **Execution Isolation:** Run untrusted code in restricted containers or operating-system sandboxes rather than relying on a timeout alone.
- **Scalability:** Improve room lifecycle management, connection cleanup, and concurrent message handling.
- **Persistence Reliability:** Add atomic file writes, explicit retention tests, and recovery from interrupted writes.
- **Observability:** Add structured logging for connection failures, synchronization errors, and process execution.
- **Automated Testing:** Expand tests to cover concurrent edits, room isolation, invalid messages, disconnections, and large documents.

---

## Conclusion

The Collaborative Code Editor demonstrates how Java desktop development, TCP networking, document synchronization, syntax highlighting, and subprocess execution can be combined into a collaborative programming environment.

It provides a foundation for experimenting with multi-client communication, event-driven application design, room-based collaboration, and local persistence while offering clear opportunities to improve synchronization correctness, security, and scalability.
