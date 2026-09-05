# DUCART

Full-stack e-commerce application built with React, Spring Boot, MySQL, Docker, and Jenkins.

[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white)](https://react.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins-D24939?logo=jenkins&logoColor=white)](https://www.jenkins.io/)

## Overview

DUCART is a role-based online shopping platform with separate buyer and administrator experiences. The React client communicates with a Spring Boot REST API, which stores application data in MySQL and serves uploaded media. The repository also includes automated frontend and backend tests, multi-stage Docker builds, a Docker Compose environment, and a Jenkins pipeline for repeatable testing, building, deployment, catalog seeding, and smoke testing.

## Key Features

### Shopping experience

- Browse the product catalogue and view individual product details
- Create an account, sign in, reset a password, and update a profile
- Add products to a cart and manage item quantities
- Add and remove wishlist items
- Complete checkout and review order history
- Submit contact enquiries and newsletter subscriptions

### Administration

- Role-protected administration routes
- Manage products, brands, main categories, and subcategories
- Upload and manage catalogue images
- Review users, orders, contact enquiries, and newsletter subscriptions
- Manage testimonials and update order status

### Engineering and delivery

- JWT-based authentication and role-based authorisation
- Frontend tests with Jest and React Testing Library
- Backend tests with JUnit 5, Mockito, MockMvc, H2, and JaCoCo
- Multi-stage Dockerfiles for smaller runtime images
- Docker Compose orchestration for the frontend, backend, and MySQL
- Jenkins pipeline covering checkout, tests, image builds, deployment, seed data, and HTTP smoke tests
- Persistent Docker volumes for MySQL data and uploaded files

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | React 18, React Router, Redux Toolkit, Redux Saga, Bootstrap, DataTables |
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring Security, JWT, Spring Data JPA, Hibernate |
| Database | MySQL 8.4; H2 for automated tests |
| Testing | Jest, React Testing Library, JUnit 5, Mockito, MockMvc, JaCoCo |
| Build tools | npm, Maven Wrapper |
| DevOps | Docker, Docker Compose, Nginx, Jenkins |

## Architecture

```text
Browser
   |
   v
React application served by Nginx :3000
   |
   v
Spring Boot REST API :8080
   |
   v
MySQL :3306
```

## Requirements

### Recommended Docker setup

- Git
- Docker Desktop with Docker Compose v2
- At least 4 GB of memory available to Docker for comfortable builds

Docker provides the required Node.js, Java, Maven, Nginx, and MySQL environments inside the project containers.

### Manual development setup

- Java Development Kit 17
- Node.js 22 and npm
- MySQL 8
- Git

The backend uses the included Maven Wrapper, so a separate Maven installation is not required.

## Installation

Clone the repository and enter the project directory:

```bash
git clone https://github.com/RedBone12/DUCART.git
cd DUCART
```

Create a local environment file from the provided template.

Windows Command Prompt:

```cmd
copy .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

The values in `.env.example` are intended only for local development. Replace every password and `JWT_SECRET` before deploying the application outside your computer. The real `.env` file is ignored by Git.

## Quick Start with Docker Compose

Build and start the complete application:

```cmd
docker compose --env-file .env up -d --build --wait
```

Optionally load the sample catalogue item from **Windows Command Prompt**:

```cmd
docker compose --env-file .env exec -T mysql sh -c "mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE" < database\catalog-seed.sql
```

Check the container status:

```cmd
docker compose --env-file .env ps
```

Open the services:

- Frontend: <http://localhost:3000>
- Product API: <http://localhost:8080/product>

Stop the application while preserving its database and upload volumes:

```cmd
docker compose --env-file .env down
```

> [!CAUTION]
> `docker compose --env-file .env down -v` also deletes the local MySQL and upload volumes. Use it only when you intentionally want a clean database.

## Local Development

Start MySQL and configure the backend through environment variables or the local defaults in `application.properties`. Then run the backend from Windows Command Prompt:

```cmd
cd Ducart-Backend
mvnw.cmd spring-boot:run
```

In a second terminal, configure `Ducart-Frontend/.env`:

```env
REACT_APP_SERVER=http://localhost:8080
```

Then start the React development server:

```cmd
cd Ducart-Frontend
npm.cmd ci
npm.cmd start
```

## Usage Examples

Retrieve the product catalogue:

```cmd
curl.exe http://localhost:8080/product
```

Typical browser workflow:

1. Visit <http://localhost:3000/shop> to browse the catalogue.
2. Open a product to view its details.
3. Create an account or sign in.
4. Add an item to the cart or wishlist.
5. Continue to checkout and review the order from the buyer profile.

Run the backend test suite and generate the JaCoCo report:

```cmd
cd Ducart-Backend
mvnw.cmd clean verify
```

The coverage report is generated at `Ducart-Backend/target/site/jacoco/index.html`.

Run the frontend test suite once in CI mode:

```cmd
cd Ducart-Frontend
npm.cmd ci
set "CI=true" && npm.cmd test -- --watchAll=false
```

## Jenkins Pipeline

The root `Jenkinsfile` defines the following pipeline:

```text
Checkout
  -> Backend Test
  -> Frontend Test
  -> Docker Build
  -> Deploy
  -> Seed Catalog
  -> Smoke Test
```

The deployment stage currently targets Docker Desktop on the Jenkins host. The smoke test verifies that both the frontend and `/product` API return HTTP 200.

## Project Structure

```text
DUCART/
|-- Ducart-Frontend/       React application, tests, Nginx config, Dockerfile
|-- Ducart-Backend/        Spring Boot API, tests, uploads, Dockerfile
|-- database/              Idempotent local catalogue seed script
|-- .github/               Repository configuration
|-- .env.example           Local Docker Compose configuration template
|-- compose.yaml           Frontend, backend, MySQL, and persistent volumes
|-- Jenkinsfile            CI/CD pipeline definition
`-- README.md              Project documentation
```

## License

No open-source licence is currently granted for this repository. It is published for educational and portfolio review. Do not assume permission to copy, redistribute, or relicense the code unless a separate `LICENSE` file is added or the repository owner provides permission.

## Repository

Source code: <https://github.com/RedBone12/DUCART>
