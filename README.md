# 📦 Stationery Management System

A full-stack **microservices-based** Stationery Management System built with **Spring Boot**, **Spring Cloud**, and **React**. The system enables organizations to manage stationery inventory, process purchase/issue requests with multi-level approvals, and maintain complete audit trails — all through a modern, responsive web interface.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (React)                               │
│                            http://localhost:3000                             │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Spring Cloud)                          │
│                            http://localhost:8080                             │
│               Route: /api/auth/** → AUTH-SERVICE                            │
│               Route: /api/inventory/** → INVENTORY-SERVICE                  │
│               Route: /api/requests/** → REQUEST-SERVICE                     │
│               Cross-Cutting: JWT Validation, CORS, Rate Limiting            │
└──────┬──────────────────────┬──────────────────────┬───────────────────────┘
       │                      │                      │
       ▼                      ▼                      ▼
┌──────────────┐   ┌───────────────────┐   ┌──────────────────┐
│ AUTH SERVICE │   │ INVENTORY SERVICE │   │ REQUEST SERVICE  │
│  :8081       │   │  :8082            │   │  :8083           │
│              │   │                   │   │                  │
│ • Register   │   │ • CRUD Items      │   │ • Create Request │
│ • Login/JWT  │   │ • Stock Mgmt      │   │ • Approve/Reject │
│ • Validate   │   │ • Categories      │   │ • Status Track   │
│ • Roles      │   │ • Low-Stock Alert │   │ • Feign → Inv.   │
└──────┬───────┘   └────────┬──────────┘   └────────┬─────────┘
       │                    │                       │
       ▼                    ▼                       ▼
┌──────────────┐   ┌───────────────────┐   ┌──────────────────┐
│   auth_db    │   │  inventory_db     │   │   request_db     │
│   (MySQL)    │   │   (MySQL)         │   │   (MySQL)        │
└──────────────┘   └───────────────────┘   └──────────────────┘

                    ┌───────────────────┐
                    │  CONFIG SERVER    │◄── Centralized Configuration
                    │  :8888            │    (Git/Native profiles)
                    └───────────────────┘

                    ┌───────────────────┐
                    │  EUREKA SERVER    │◄── Service Discovery
                    │  :8761            │    & Registration
                    └───────────────────┘
```

---

## 🛠️ Technology Stack

| Layer              | Technology                                      |
|--------------------|--------------------------------------------------|
| **Frontend**       | React 18, React Router, Axios, Bootstrap 5       |
| **API Gateway**    | Spring Cloud Gateway (Reactive)                  |
| **Backend**        | Java 17, Spring Boot 3.2.x, Spring Cloud 2023.x  |
| **Security**       | Spring Security, JWT (JSON Web Tokens)            |
| **Service Comm.**  | OpenFeign (Declarative REST Clients)              |
| **Discovery**      | Netflix Eureka (Service Registry)                 |
| **Configuration**  | Spring Cloud Config Server (Centralized)          |
| **Database**       | MySQL 8.0 (Separate DB per microservice)          |
| **ORM**            | Spring Data JPA / Hibernate                       |
| **Build**          | Maven 3.9+                                        |
| **Containerization** | Docker, Docker Compose                          |
| **CI/CD**          | Jenkins (Declarative Pipeline)                    |

---

## ✅ Prerequisites

Ensure the following tools are installed on your machine:

| Tool            | Version   | Download Link                                                 |
|-----------------|-----------|---------------------------------------------------------------|
| **Java JDK**    | 17+       | [Eclipse Temurin](https://adoptium.net/)                      |
| **Maven**       | 3.9+      | [Maven Downloads](https://maven.apache.org/download.cgi)      |
| **Node.js**     | 18+       | [Node.js Downloads](https://nodejs.org/)                      |
| **npm**         | 9+        | Bundled with Node.js                                          |
| **MySQL**       | 8.0       | [MySQL Downloads](https://dev.mysql.com/downloads/)           |
| **Docker**      | 24+       | [Docker Desktop](https://www.docker.com/products/docker-desktop) |
| **Docker Compose** | 2.20+  | Bundled with Docker Desktop                                   |
| **Git**         | 2.40+     | [Git Downloads](https://git-scm.com/downloads)                |

---

## 🚀 Running Locally (Without Docker)

### Step 1: Start MySQL & Create Databases

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create the three databases
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
CREATE DATABASE IF NOT EXISTS request_db;

-- Verify
SHOW DATABASES;
```

### Step 2: Start Config Server

```bash
cd stationery-management/config-server
mvn spring-boot:run
```

Wait until you see: `Started ConfigServerApplication on port 8888`

Verify: [http://localhost:8888/actuator/health](http://localhost:8888/actuator/health)

### Step 3: Start Eureka Server

```bash
cd stationery-management/eureka-server
mvn spring-boot:run
```

Wait until you see: `Started EurekaServerApplication on port 8761`

Verify: [http://localhost:8761](http://localhost:8761) (Eureka Dashboard)

### Step 4: Start API Gateway

```bash
cd stationery-management/api-gateway
mvn spring-boot:run
```

Wait until you see: `Started ApiGatewayApplication on port 8080`

### Step 5: Start Auth Service

```bash
cd stationery-management/auth-service
mvn spring-boot:run
```

Wait until you see: `Started AuthServiceApplication on port 8081`

### Step 6: Start Inventory Service

```bash
cd stationery-management/inventory-service
mvn spring-boot:run
```

Wait until you see: `Started InventoryServiceApplication on port 8082`

### Step 7: Start Request Service

```bash
cd stationery-management/request-service
mvn spring-boot:run
```

Wait until you see: `Started RequestServiceApplication on port 8083`

### Step 8: Start React Frontend

```bash
cd stationery-management/frontend
npm install
npm start
```

The frontend opens at: [http://localhost:3000](http://localhost:3000)

> **💡 Tip:** Start services in the exact order above. Each service depends on the ones started before it.

---

## 🐳 Running with Docker Compose

### Quick Start

```bash
cd stationery-management

# Build and start all services
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### Useful Commands

```bash
# View running containers
docker-compose ps

# Follow logs for a specific service
docker-compose logs -f auth-service

# Stop all services
docker-compose down

# Stop all services and remove data volumes
docker-compose down -v

# Rebuild a single service
docker-compose up --build auth-service
```

### Verify All Services Are Running

| Service          | Health Check URL                                    |
|------------------|-----------------------------------------------------|
| Config Server    | http://localhost:8888/actuator/health                |
| Eureka Dashboard | http://localhost:8761                                |
| API Gateway      | http://localhost:8080/actuator/health                |
| Auth Service     | http://localhost:8081/actuator/health                |
| Inventory Service| http://localhost:8082/actuator/health                |
| Request Service  | http://localhost:8083/actuator/health                |
| Frontend         | http://localhost:3000                                |

---

## 🌐 Service Ports

| Service            | Port  | Description                              |
|--------------------|-------|------------------------------------------|
| MySQL              | 3306  | Database server                          |
| Config Server      | 8888  | Centralized configuration                |
| Eureka Server      | 8761  | Service discovery & registry             |
| API Gateway        | 8080  | Single entry point for all API requests  |
| Auth Service       | 8081  | Authentication & authorization           |
| Inventory Service  | 8082  | Stationery inventory management          |
| Request Service    | 8083  | Purchase/issue request processing        |
| Frontend           | 3000  | React web application (Nginx in Docker)  |

---

## 📚 API Documentation

All API calls should go through the **API Gateway** at `http://localhost:8080`.

### 🔐 Authentication Service (`/api/auth`)

| Method | Endpoint               | Auth Required | Description                          |
|--------|------------------------|:-------------:|--------------------------------------|
| POST   | `/api/auth/register`   | ❌            | Register a new user                  |
| POST   | `/api/auth/login`      | ❌            | Login and receive JWT token          |
| GET    | `/api/auth/validate`   | ✅            | Validate JWT token                   |

#### Register a New User

```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "john_doe",
    "password": "SecurePass123!",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "EMPLOYEE"
}
```

**Response (201 Created):**
```json
{
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "EMPLOYEE",
    "message": "User registered successfully"
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "john_doe",
    "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "username": "john_doe",
    "role": "EMPLOYEE"
}
```

#### Validate Token

```http
GET /api/auth/validate
Authorization: Bearer <jwt-token>
```

**Response (200 OK):**
```json
{
    "valid": true,
    "username": "john_doe",
    "role": "EMPLOYEE"
}
```

---

### 📦 Inventory Service (`/api/inventory`)

| Method | Endpoint                            | Auth Required | Roles          | Description                    |
|--------|-------------------------------------|:-------------:|----------------|--------------------------------|
| GET    | `/api/inventory`                    | ✅            | ALL            | Get all inventory items        |
| GET    | `/api/inventory/{id}`               | ✅            | ALL            | Get item by ID                 |
| POST   | `/api/inventory`                    | ✅            | ADMIN          | Create new inventory item      |
| PUT    | `/api/inventory/{id}`               | ✅            | ADMIN          | Update inventory item          |
| DELETE | `/api/inventory/{id}`               | ✅            | ADMIN          | Delete inventory item          |
| GET    | `/api/inventory/category/{category}`| ✅            | ALL            | Get items by category          |
| GET    | `/api/inventory/low-stock`          | ✅            | ADMIN, MANAGER | Get low-stock items            |
| GET    | `/api/inventory/search?name={name}` | ✅            | ALL            | Search items by name           |

#### Create Inventory Item

```http
POST /api/inventory
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
    "name": "Blue Ballpoint Pen",
    "description": "Standard blue ballpoint pen, medium tip",
    "category": "PENS",
    "quantity": 500,
    "minimumStockLevel": 50,
    "unitPrice": 1.50,
    "supplier": "Office Supplies Co."
}
```

**Response (201 Created):**
```json
{
    "id": 1,
    "name": "Blue Ballpoint Pen",
    "description": "Standard blue ballpoint pen, medium tip",
    "category": "PENS",
    "quantity": 500,
    "minimumStockLevel": 50,
    "unitPrice": 1.50,
    "supplier": "Office Supplies Co.",
    "createdAt": "2026-06-17T12:00:00",
    "updatedAt": "2026-06-17T12:00:00"
}
```

#### Get Low-Stock Items

```http
GET /api/inventory/low-stock
Authorization: Bearer <admin-jwt-token>
```

**Response (200 OK):**
```json
[
    {
        "id": 3,
        "name": "A4 Paper Ream",
        "category": "PAPER",
        "quantity": 10,
        "minimumStockLevel": 25,
        "unitPrice": 5.99
    }
]
```

---

### 📝 Request Service (`/api/requests`)

| Method | Endpoint                          | Auth Required | Roles            | Description                    |
|--------|-----------------------------------|:-------------:|------------------|--------------------------------|
| POST   | `/api/requests`                   | ✅            | EMPLOYEE         | Create a new request           |
| GET    | `/api/requests/my`                | ✅            | ALL              | Get current user's requests    |
| GET    | `/api/requests/{id}`              | ✅            | ALL              | Get request by ID              |
| GET    | `/api/requests`                   | ✅            | ADMIN, MANAGER   | Get all requests (admin view)  |
| PUT    | `/api/requests/{id}/approve`      | ✅            | ADMIN, MANAGER   | Approve a pending request      |
| PUT    | `/api/requests/{id}/reject`       | ✅            | ADMIN, MANAGER   | Reject a pending request       |

#### Create a Stationery Request

```http
POST /api/requests
Authorization: Bearer <employee-jwt-token>
Content-Type: application/json

{
    "itemId": 1,
    "itemName": "Blue Ballpoint Pen",
    "quantity": 10,
    "reason": "Team supplies for Q3",
    "urgency": "MEDIUM"
}
```

**Response (201 Created):**
```json
{
    "id": 1,
    "itemId": 1,
    "itemName": "Blue Ballpoint Pen",
    "quantity": 10,
    "reason": "Team supplies for Q3",
    "urgency": "MEDIUM",
    "status": "PENDING",
    "requestedBy": "john_doe",
    "requestedAt": "2026-06-17T12:30:00",
    "approvedBy": null,
    "approvedAt": null,
    "rejectionReason": null
}
```

#### Approve a Request

```http
PUT /api/requests/1/approve
Authorization: Bearer <manager-jwt-token>
Content-Type: application/json

{
    "comments": "Approved for Q3 allocation"
}
```

**Response (200 OK):**
```json
{
    "id": 1,
    "status": "APPROVED",
    "approvedBy": "manager_jane",
    "approvedAt": "2026-06-17T14:00:00",
    "comments": "Approved for Q3 allocation"
}
```

#### Reject a Request

```http
PUT /api/requests/1/reject
Authorization: Bearer <manager-jwt-token>
Content-Type: application/json

{
    "rejectionReason": "Budget exceeded for this quarter"
}
```

**Response (200 OK):**
```json
{
    "id": 1,
    "status": "REJECTED",
    "approvedBy": "manager_jane",
    "approvedAt": "2026-06-17T14:00:00",
    "rejectionReason": "Budget exceeded for this quarter"
}
```

---

## 🗄️ Database Schema

### Auth DB (`auth_db`)

| Table     | Columns                                                                                    |
|-----------|--------------------------------------------------------------------------------------------|
| **users** | `id` (PK), `username` (UNIQUE), `password` (BCrypt), `email`, `full_name`, `role`, `enabled`, `created_at`, `updated_at` |

**Roles Enum:** `ADMIN`, `MANAGER`, `EMPLOYEE`

### Inventory DB (`inventory_db`)

| Table              | Columns                                                                                      |
|--------------------|----------------------------------------------------------------------------------------------|
| **inventory_items**| `id` (PK), `name`, `description`, `category`, `quantity`, `minimum_stock_level`, `unit_price`, `supplier`, `created_at`, `updated_at` |

**Categories Enum:** `PENS`, `PAPER`, `NOTEBOOKS`, `FOLDERS`, `STAPLERS`, `ADHESIVES`, `MARKERS`, `ERASERS`, `SCISSORS`, `OTHER`

### Request DB (`request_db`)

| Table              | Columns                                                                                      |
|--------------------|----------------------------------------------------------------------------------------------|
| **stationery_requests** | `id` (PK), `item_id` (FK ref), `item_name`, `quantity`, `reason`, `urgency`, `status`, `requested_by`, `requested_at`, `approved_by`, `approved_at`, `rejection_reason`, `comments` |

**Status Enum:** `PENDING`, `APPROVED`, `REJECTED`, `FULFILLED`, `CANCELLED`

**Urgency Enum:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

---

## 🔑 Default Credentials

| User         | Username       | Password        | Role       |
|--------------|----------------|-----------------|------------|
| Admin        | `admin`        | `admin123`      | ADMIN      |
| Manager      | `manager`      | `manager123`    | MANAGER    |
| Employee     | `employee`     | `employee123`   | EMPLOYEE   |

> **⚠️ Warning:** Change default passwords in production! These are seeded by the Auth Service on first startup for development/demo purposes.

**MySQL Credentials (Docker):**
- **Host:** `localhost:3306`
- **Username:** `root`
- **Password:** `root`

---

## 📁 Project Structure

```
stationery-management/
│
├── config-server/                  # Spring Cloud Config Server
│   ├── src/main/java/com/stationery/configserver/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── configurations/         # Service-specific configs
│   ├── Dockerfile
│   └── pom.xml
│
├── eureka-server/                  # Netflix Eureka Service Registry
│   ├── src/main/java/com/stationery/eurekaserver/
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── api-gateway/                    # Spring Cloud API Gateway
│   ├── src/main/java/com/stationery/gateway/
│   │   ├── config/                 # Route & CORS configuration
│   │   └── filter/                 # JWT authentication filter
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── auth-service/                   # Authentication & Authorization Service
│   ├── src/main/java/com/stationery/auth/
│   │   ├── config/                 # Security configuration
│   │   ├── controller/             # REST controllers
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── repository/             # Spring Data repositories
│   │   ├── security/               # JWT utility, filters
│   │   └── service/                # Business logic
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/                   # Unit & integration tests
│   ├── Dockerfile
│   └── pom.xml
│
├── inventory-service/              # Stationery Inventory Service
│   ├── src/main/java/com/stationery/inventory/
│   │   ├── controller/             # REST controllers
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── exception/              # Custom exceptions & handlers
│   │   ├── repository/             # Spring Data repositories
│   │   └── service/                # Business logic
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/                   # Unit & integration tests
│   ├── Dockerfile
│   └── pom.xml
│
├── request-service/                # Stationery Request Service
│   ├── src/main/java/com/stationery/request/
│   │   ├── client/                 # Feign client for Inventory Service
│   │   ├── controller/             # REST controllers
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── entity/                 # JPA entities
│   │   ├── exception/              # Custom exceptions & handlers
│   │   ├── repository/             # Spring Data repositories
│   │   └── service/                # Business logic
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── src/test/                   # Unit & integration tests
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/                       # React Frontend Application
│   ├── public/
│   ├── src/
│   │   ├── components/             # Reusable UI components
│   │   ├── pages/                  # Page-level components
│   │   ├── services/               # API service layer (Axios)
│   │   ├── context/                # Auth context provider
│   │   └── App.js                  # Root component & routing
│   ├── Dockerfile                  # Multi-stage: build + Nginx
│   ├── nginx.conf                  # Nginx reverse-proxy config
│   └── package.json
│
├── docker-compose.yml              # Full-stack orchestration
├── init.sql                        # Database initialization script
├── Jenkinsfile                     # CI/CD pipeline definition
└── README.md                       # This file
```

---

## 🔄 CI/CD Pipeline

The project includes a **Jenkinsfile** defining a complete CI/CD pipeline:

```
┌──────────┐   ┌─────────────────┐   ┌───────────┐   ┌────────────────┐
│ Checkout │──▶│ Build Services  │──▶│ Run Tests │──▶│ Build Frontend │
│          │   │  (Parallel x6)  │   │(Parallel) │   │                │
└──────────┘   └─────────────────┘   └───────────┘   └────────┬───────┘
                                                               │
┌──────────┐   ┌─────────────────┐   ┌──────────────┐         │
│  Deploy  │◀──│  Docker Push    │◀──│ Docker Build  │◀────────┘
│          │   │  (Registry)     │   │ (Parallel x7) │
└──────────┘   └─────────────────┘   └──────────────┘
```

### Pipeline Stages

| Stage                  | Description                                                |
|------------------------|------------------------------------------------------------|
| **Checkout**           | Pulls latest code from SCM                                 |
| **Build Backend**      | Parallel Maven builds for all 6 backend services           |
| **Run Tests**          | Parallel unit/integration tests for business services      |
| **Build Frontend**     | `npm ci` and `npm run build` for the React application     |
| **Frontend Tests**     | Runs React test suite                                      |
| **Docker Build**       | Parallel Docker image builds for all 7 components          |
| **Docker Push**        | Pushes tagged images to the configured Docker registry     |
| **Deploy**             | Runs `docker-compose up -d` to deploy the full stack       |

### Jenkins Prerequisites

- **Maven 3.9+** configured as `Maven-3.9` in Global Tool Configuration
- **JDK 17** configured as `JDK-17` in Global Tool Configuration
- **Node.js 18** configured as `NodeJS-18` via the NodeJS Jenkins plugin
- **Docker** and **Docker Compose** installed on the Jenkins agent
- **Credentials:**
  - `docker-registry-url` — Your Docker registry URL
  - `docker-registry-creds` — Username/password credentials for the registry

---

## 🧪 Running Tests

### Backend Tests

```bash
# Run all tests for a specific service
cd auth-service && mvn test
cd inventory-service && mvn test
cd request-service && mvn test

# Run tests with coverage report
cd auth-service && mvn test jacoco:report
```

### Frontend Tests

```bash
cd frontend

# Run tests in watch mode
npm test

# Run tests with coverage
npm test -- --coverage --watchAll=false
```

---

## 🔧 Configuration

All service configurations are centralized in the **Config Server**. Key properties can be overridden via environment variables:

| Environment Variable                        | Default Value                                      | Description              |
|---------------------------------------------|----------------------------------------------------|--------------------------|
| `SPRING_DATASOURCE_URL`                     | `jdbc:mysql://localhost:3306/<db_name>`             | JDBC connection URL      |
| `SPRING_DATASOURCE_USERNAME`                | `root`                                              | Database username        |
| `SPRING_DATASOURCE_PASSWORD`                | `root`                                              | Database password        |
| `SPRING_CONFIG_IMPORT`                      | `configserver:http://localhost:8888`                | Config server URL        |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`      | `http://localhost:8761/eureka/`                     | Eureka server URL        |
| `JWT_SECRET`                                | *(configured in auth-service)*                      | JWT signing secret       |
| `JWT_EXPIRATION`                            | `86400000` (24 hours)                               | JWT expiration in ms     |

---

## 🐛 Troubleshooting

### Common Issues

| Problem                                  | Solution                                                                 |
|------------------------------------------|--------------------------------------------------------------------------|
| Service can't connect to Config Server   | Ensure Config Server is running and healthy before starting other services |
| `Connection refused` to MySQL            | Wait for MySQL healthcheck to pass; verify port 3306 is available         |
| Eureka dashboard shows no services       | Services take 30s to register; wait and refresh                           |
| JWT token rejected at API Gateway        | Ensure the same JWT secret is configured across all services              |
| Frontend shows "Network Error"           | Verify API Gateway is running on port 8080                                |
| Docker Compose services keep restarting  | Check logs: `docker-compose logs <service-name>`                          |
| Port already in use                      | Stop conflicting process or change the port mapping in `docker-compose.yml` |

### Viewing Logs

```bash
# Docker Compose logs
docker-compose logs -f                    # All services
docker-compose logs -f auth-service       # Specific service

# Local development logs
# Check the console output of each running service
```

---

## 📄 License

This project is developed for educational and internal organizational use.

---

## 👤 Author

**Stationery Management System** — A Spring Boot Microservices Capstone Project
