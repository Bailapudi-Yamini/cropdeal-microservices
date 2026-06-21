# CropDeal — Agricultural Marketplace Platform

A full-stack microservices platform connecting farmers and buyers for crop trading, with order negotiation, online payments, and real-time notifications.

## Live Demo
🌐 Live Demo: https://cropdeal-microservices.vercel.app
> **Frontend:** [https://cropdeal-frontend.up.railway.app](https://cropdeal-frontend.up.railway.app) *(update after deployment)*
> **API Gateway:** [https://cropdeal-gateway.up.railway.app](https://cropdeal-gateway.up.railway.app) *(update after deployment)*

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.2, Spring Cloud 2023 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Config Management | Spring Cloud Config Server |
| Messaging | RabbitMQ (AMQP) |
| Databases | MySQL 8, MongoDB |
| Cache / Rate Limiting | Redis |
| Authentication | JWT + Spring Security |
| Payments | Razorpay |
| Frontend | Angular 17, Tailwind CSS |
| Container | Docker (multi-stage builds) |
| Deployment | Railway.app |

---

## Services

| Service | Port | Description |
|---|---|---|
| config-server | 8888 | Centralised config for all services |
| eureka-server | 8761 | Service registry and discovery |
| api-gateway | 8080 | Single entry-point, JWT auth, rate limiting |
| user-service | 8081 | Registration, login, profiles, bank accounts |
| crop-service | 8082 | Crop listings (MySQL + MongoDB) |
| order-service | 8083 | Orders, negotiation rounds |
| payment-service | 8084 | Razorpay payment processing |
| notification-service | 8085 | Email notifications via RabbitMQ |
| admin-service | 8086 | Admin dashboard, user/crop management |
| frontend | 4200 / 80 | Angular SPA |

---

## Run Locally (without Docker)

Start services in this exact order:

```bash
# 1. Config Server (port 8888)
cd config-server && ./mvnw spring-boot:run

# 2. Eureka Server (port 8761)
cd eureka-server && ./mvnw spring-boot:run

# 3. Business services (any order, ports 8081–8086)
cd user-service         && ./mvnw spring-boot:run
cd crop-service         && ./mvnw spring-boot:run
cd order-service        && ./mvnw spring-boot:run
cd payment-service      && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd admin-service        && ./mvnw spring-boot:run

# 4. API Gateway (port 8080) — after all services are registered in Eureka
cd api-gateway && ./mvnw spring-boot:run

# 5. Angular Frontend (port 4200)
cd frontend && npm install && npm start
```

### Verification Checkpoints

```
config-server : GET http://localhost:8888/actuator/health  → {"status":"UP"}
eureka-server : GET http://localhost:8761/actuator/health  → {"status":"UP"}
services      : visible at http://localhost:8761 (Eureka dashboard)
api-gateway   : GET http://localhost:8080/actuator/health  → {"status":"UP"}
frontend      : http://localhost:4200
```

### Required Local Services

- MySQL 8 running on port 3306 (default credentials: root / root22)
- RabbitMQ running on port 5672
- Redis running on port 6379
- MongoDB running on port 27017 (crop-service only)

Or start infrastructure with Docker:

```bash
docker-compose up -d mysql rabbitmq redis mongodb
```

---

## Run with Docker Compose

```bash
docker-compose up --build
```

---

## Deploy to Railway

Each service folder contains a `Dockerfile` and `railway.json`.

### Environment Variables to Set in Railway

Set these variables on **each** relevant Railway service:

| Variable | Description | Required By |
|---|---|---|
| `EUREKA_URI` | Full Eureka URL, e.g. `http://eureka-service.railway.internal:8761/eureka/` | All services |
| `CONFIG_SERVER_URI` | Full config-server URL | All services |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | All data services |
| `SPRING_DATASOURCE_USERNAME` | MySQL username | All data services |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | All data services |
| `RABBITMQ_HOST` | RabbitMQ host | All services |
| `RABBITMQ_PORT` | RabbitMQ port (default 5672) | All services |
| `RABBITMQ_USERNAME` | RabbitMQ username | All services |
| `RABBITMQ_PASSWORD` | RabbitMQ password | All services |
| `JWT_SECRET` | Base64 JWT signing secret (min 256-bit) | All services |
| `REDIS_HOST` | Redis host | api-gateway |
| `REDIS_PORT` | Redis port (default 6379) | api-gateway |
| `MONGO_URI` | MongoDB connection URI | crop-service |
| `RAZORPAY_KEY_ID` | Razorpay API key | payment-service |
| `RAZORPAY_KEY_SECRET` | Razorpay secret | payment-service |
| `MAIL_HOST` | SMTP host | notification-service |
| `MAIL_USERNAME` | SMTP username | notification-service |
| `MAIL_PASSWORD` | SMTP password | notification-service |
| `API_GATEWAY_URL` | Public gateway URL (e.g. `https://cropdeal-gateway.up.railway.app`) | frontend |

### Deploy Order on Railway

1. Deploy **config-server** first — wait for it to show healthy
2. Deploy **eureka-server** — wait for healthy
3. Deploy all 6 business services in any order
4. Deploy **api-gateway** last (after services register in Eureka)
5. Deploy **frontend** — set `API_GATEWAY_URL` to the gateway's Railway public URL

---

## API Documentation (Swagger)

Each service exposes Swagger UI at `/<service-base-path>/swagger-ui.html` when running locally.
Through the gateway:

- `http://localhost:8080/api/users/swagger-ui.html`
- `http://localhost:8080/api/crops/swagger-ui.html`
- `http://localhost:8080/api/orders/swagger-ui.html`
- `http://localhost:8080/api/payments/swagger-ui.html`
- `http://localhost:8080/api/admin/swagger-ui.html`

---

## Project Structure

```
cropdeal/
├── config-server/
├── eureka-server/
├── user-service/
├── crop-service/
├── order-service/
├── payment-service/
├── notification-service/
├── admin-service/
├── api-gateway/
├── frontend/
├── docker/
│   └── mysql/init.sql
└── docker-compose.yml
```
