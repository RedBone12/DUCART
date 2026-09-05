# DUCART

Full-stack e-commerce application built with React, Spring Boot, MySQL, Docker, and Jenkins.

[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=white)](https://react.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins-D24939?logo=jenkins&logoColor=white)](https://www.jenkins.io/)

## Overview

DUCART is a role-based online shopping platform with a React frontend, Spring Boot REST API, and MySQL database. The project includes automated frontend and backend testing, containerised local deployment, persistent data storage, and a Jenkins CI/CD pipeline.

## Features

### Customer

- Browse the catalogue and view product details
- Sign up, sign in, reset a password, and update a profile
- Manage cart and wishlist items
- Complete checkout and review order history
- Submit contact enquiries and newsletter subscriptions

### Administrator

- Role-protected administration pages
- Manage products, brands, categories, and catalogue images
- Manage users, orders, testimonials, enquiries, and subscriptions

### Engineering

- JWT authentication and role-based authorisation
- Jest and React Testing Library frontend tests
- JUnit 5, Mockito, MockMvc, H2, and JaCoCo backend tests
- Multi-stage Docker builds and Docker Compose orchestration
- Jenkins pipeline for testing, building, deployment, and smoke testing

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | React 18, React Router, Redux Toolkit, Redux Saga, Bootstrap, DataTables |
| Backend | Java 17, Spring Boot 3.5, Spring Security, JWT, Spring Data JPA, Hibernate |
| Database | MySQL 8.4, H2 for tests |
| Testing | Jest, React Testing Library, JUnit 5, Mockito, MockMvc, JaCoCo |
| DevOps | Docker, Docker Compose, Nginx, Jenkins |

## Quick Start

### Requirements

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)

Clone and start the complete application:

```bash
git clone https://github.com/RedBone12/DUCART.git
cd DUCART
docker compose --env-file .env.example up -d --build --wait
```

Open:

- Frontend: <http://localhost:3000>
- Product API: <http://localhost:8080/product>

Stop the application:

```bash
docker compose --env-file .env.example down
```

> The values in `.env.example` are for local demonstration only. Create a private `.env` with new passwords and a new JWT secret before any public deployment.

## CI/CD Pipeline

The root `Jenkinsfile` runs the project through the following stages:

```text
Checkout
  -> Backend Test
  -> Frontend Test
  -> Docker Build
  -> Deploy
  -> Seed Catalog
  -> Smoke Test
```

The current deployment targets Docker Desktop on the Jenkins host. The smoke test verifies that the frontend and product API respond successfully.

## Project Structure

```text
DUCART/
|-- Ducart-Frontend/    React application, tests, Nginx config, Dockerfile
|-- Ducart-Backend/     Spring Boot API, tests, uploads, Dockerfile
|-- database/           Local catalogue seed script
|-- .env.example        Local configuration template
|-- compose.yaml        Docker Compose services and volumes
|-- Jenkinsfile         CI/CD pipeline
`-- README.md           Project documentation
```
