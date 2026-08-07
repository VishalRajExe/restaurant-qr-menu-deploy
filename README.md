# Restaurant QR Menu Platform 🍽️📱

A complete, full-stack multi-tenant Restaurant QR Menu & Order Management System built with Spring Boot 3, Angular 17/21, and MySQL.

---

## 🏗 Architecture & Stack

- **Backend**: Java 17, Spring Boot 3.2.3, Spring Security (JWT & RBAC), Spring Data JPA, Flyway DB Migrations, MySQL 8.0
- **Frontend**: Angular, TypeScript, Tailwind CSS, Lucide Icons, Modern Reactive State & Signal Management
- **Deployment**: Docker, Docker Compose, Nginx Reverse Proxy

---

## 🚀 Quick Start with Docker

Run the entire full-stack platform (MySQL + Spring Boot Backend + Angular Frontend) with a single command:

```bash
docker-compose up --build -d
```

### Services & Endpoints

| Service | Container Name | URL / Port |
| :--- | :--- | :--- |
| **Frontend Web App** | `restaurant_qr_frontend` | [http://localhost:4200](http://localhost:4200) or [http://localhost](http://localhost) |
| **Backend REST API** | `restaurant_qr_backend` | [http://localhost:8080/api/v1](http://localhost:8080/api/v1) |
| **MySQL Database** | `restaurant_qr_mysql` | `localhost:3306` (`restaurant_qr_db`) |

To view live container logs:
```bash
docker-compose logs -f
```

To stop containers:
```bash
docker-compose down
```

---

## 🛠 Local Development Setup

### 1. Run Backend Locally
```bash
cd restaurant-qr-menu-backend
mvn spring-boot:run
```

### 2. Run Frontend Locally
```bash
cd restaurant-qr-menu-frontend
npm install
npm run start
```
Frontend dev server will start on `http://localhost:4200/`.

---

## 🔐 Credentials & Seeding

The database auto-seeds default roles and demo datasets on first boot (`DatabaseDataSeeder.java`).

---

## 📦 Project Structure

```
.
├── docker-compose.yml
├── .dockerignore
├── restaurant-qr-menu-backend/   # Spring Boot REST API
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
└── restaurant-qr-menu-frontend/  # Angular Frontend App
    ├── Dockerfile
    ├── nginx.conf
    └── src/
```
