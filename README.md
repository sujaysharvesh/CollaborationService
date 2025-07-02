The **Collaboration Service** is a core component of a real-time document editing platform built with Java. Using WebSockets and CRDTs (Yjs), this system enables low-latency, multi-user document collaboration with seamless syncing, user presence tracking, and secure JWT-based authentication.


## Architecture Diagram
![Architecture Diagram](./architechtureDiagram.png)

## Overview of the Platform

This platform is architected as three distinct microservices:

1. **[User Authentication Service](https://github.com/sujaysharvesh/CollabEditor)**
   - Handles user registration, login, and JWT token issuance.
   - Ensures secure, authenticated access across the platform.

2. **[Document Service](https://github.com/sujaysharvesh/CollabDocService)**
   - Responsible for document storage, retrieval, and persistence.
   - Receives real-time updates from the Collaboration Service and persists Yjs document state.
   - Provides APIs for document management and integrates with RabbitMQ for event-driven updates.

3. **Collaboration Service** (this repository)
   - Manages real-time WebSocket connections.
   - Synchronizes document state among connected clients using Yjs (CRDT).
   - Tracks user presence and sends events to the Document Service via RabbitMQ.
   - Handles authorization and relays updates securely between users.
  
   ## Key Features

- **Real-time document collaboration** using CRDTs (Yjs)
- **WebSocket-based** low-latency sync between multiple clients
- **User presence tracking** (who is online and editing)
- **JWT-based authentication** for secure access
- **Event-driven architecture**: User join/leave and document updates sent via RabbitMQ
- **Document state persistence** via Document Service

## Integration Flow

1. **User authenticates** via Auth Service and receives a JWT.
2. **Client connects** to Collaboration Service WebSocket with JWT.
3. **Real-time edits** are synchronized using Yjs and broadcast to all connected clients.
4. **Presence updates** and **document changes** are published to Document Service via RabbitMQ.
5. **Document state** is periodically persisted by the Document Service.

## Acknowledgments

- [Yjs](https://github.com/yjs/yjs) for CRDT-based collaborative editing
