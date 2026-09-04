# DUCART Resume-Ready Full-Stack E-commerce Project

DUCART is a React + Redux Saga + Spring Boot + MySQL e-commerce project.

## Completed feature scope

### Buyer side
- User signup / login data flow
- Buyer profile display and update
- Profile picture upload
- Product listing and product details
- Add to cart
- Cart quantity update and delete
- Add/remove wishlist item
- Checkout / place order
- Order history in profile

### Admin side
- Main category CRUD with image upload
- Subcategory CRUD with image upload
- Brand CRUD with image upload
- Product CRUD with multiple image upload
- Product stock update after checkout
- User list
- Newsletter management
- Contact-us query management
- Testimonial management
- Order/checkout management and status update

## Tech stack

Frontend:
- React 18
- React Router
- Redux + Redux Saga
- Bootstrap / DataTables

Backend:
- Java 17
- Spring Boot 3.5
- Spring Web
- Spring Data JPA / Hibernate
- MySQL
- Multipart file upload

## How to run

### 1. Database

Import the provided SQL file:

```sql
source DUCART_SCHEMA_AND_SEED.sql;
```

Or run the backend once with `spring.jpa.hibernate.ddl-auto=update` and then insert seed data manually.

### 2. Backend

Open `Ducart-Backend/src/main/resources/application.properties` and update your MySQL username/password.

Then run:

```bash
cd Ducart-Backend
./mvnw spring-boot:run
```

Backend default URL:

```txt
http://localhost:8080
```

### 3. Frontend

```bash
cd Ducart-Frontend
npm install
npm start
```

Frontend default URL:

```txt
http://localhost:3000
```

Make sure `.env` contains:

```env
REACT_APP_SERVER="http://localhost:8080"
```

## Demo accounts

Admin:

```txt
username: admin
password: admin123
```

Buyer:

```txt
username: buyer
password: buyer123
```

## Important notes

- The uploaded SQL file only created the database. It did not include tables or seed data, so `DUCART_SCHEMA_AND_SEED.sql` was added.
- The original backend package did not include Java source files under `src/main/java`; source files were reconstructed so the project can be maintained and shown on GitHub.
- Do not push real database passwords to GitHub.
