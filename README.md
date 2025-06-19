📄 Collaborative Document Editing Platform
A real-time collaborative document editing platform with user authentication, music metadata service, and WebSocket-powered document synchronization. Built using microservices, deployed with Docker on EC2, and designed for scalability and performance.

🧩 Architecture Overview

![Architecture Diagram](./architechtureDiagram.png)

🔁 High-Level Flow:
Users edit documents in real time using Tiptap + Yjs (CRDT).

Requests go through Nginx reverse proxy to an API Gateway hosted on EC2.

The gateway routes requests to respective microservices:

User Service for auth & user data.

Document Service for document & music metadata.

Collaboration Service for WebSocket handling and CRDT syncing.

🧱 Microservices

🔐 User Service

https://github.com/sujaysharvesh/CollabEditor

Manages user registration, login, and authentication.

Stores user data in PostgreSQL.

JWT authentication with Redis for token storage.

📁 Document Service

https://github.com/sujaysharvesh/CollabDocService

Handles documents and music metadata.

Stores data in a dedicated PostgreSQL database.

Communicates with the frontend web client for CRUD operations.

💬 Collaboration Service

Powered by Y-WebSocket and Redis Pub/Sub.

Enables real-time collaborative editing with Tiptap + Yjs.

Communicates with RabbitMQ for distributed messaging.

🚪 API Gateway

Centralized entry point for all requests.

Routes traffic to internal services based on endpoint paths.

Handles request validation and security.


📦 Infrastructure & DevOps
☁️ Deployed on AWS EC2

🐳 Dockerized each microservice

🔗 Service Registry for dynamic discovery

🌐 Nginx as reverse proxy & load balancer

📨 RabbitMQ for inter-service communication

🔧 Technologies Used
Layer	Tech Stack
Frontend Editor	Tiptap, Yjs
Backend Services	Spring Boot, Redis, PostgreSQL
Real-time Sync	Y-WebSocket, Redis Pub/Sub
Messaging Queue	RabbitMQ
Auth & Security	JWT, CSRF
Gateway & Routing	Spring Cloud Gateway, Nginx
Deployment	Docker, AWS EC2
