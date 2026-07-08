# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Wandering Table** is a Spring Boot 4.0.3 + Kotlin REST API backend for a tabletop club, featuring JWT-based authentication with refresh token rotation and note management.

## Commands

```bash
# Build
./gradlew build
./gradlew clean build

# Run
./gradlew bootRun          # App runs on port 8050
docker-compose up          # Start PostgreSQL 15 container first

# Test
./gradlew test
```

## Architecture

**Stack:** Kotlin 2.2.21, Spring Boot 4.0.3, Spring Security, Spring Data JPA, PostgreSQL, JWT

**Package root:** `ru.gohasoft.wanderingtable`

**Layers:**
- `controllers/` — REST endpoints (AuthController, NoteController, StatusController)
- `security/` — JWT filter chain (JwtAuthFilter → JwtService → AuthService), SecurityConfig, HashEncoder
- `database/model/` — JPA entities: User, Note, RefreshToken, Role, ObjectId
- `database/repository/` — Spring Data repositories

## Key Design Decisions

**JWT flow:** Access token + refresh token returned on login. Refresh tokens are single-use (rotation): stored as SHA-256 hash in `refresh_tokens` table. On refresh, old token is deleted and a new pair is issued.

**Security config:** CSRF disabled (API only). Public routes: `GET /`, `/auth/**`. All others require JWT.

**Database:** Schema auto-updated via `spring.jpa.hibernate.ddl-auto=update`. Credentials and port in `src/main/resources/application.properties`. Docker Compose exposes PostgreSQL on `127.0.0.1:5432`.

**JWT secret:** Loaded from environment variable `JWT_SECRET_BASE64` (Base64-encoded).

**Password rules:** Min 8 chars, must contain uppercase, lowercase, and digit — validated in `AuthController`.

## Configuration

| Setting | Value |
|---|---|
| Server port | 8050 |
| DB URL | `jdbc:postgresql://localhost:5432/postgres` |
| DB user/pass | `postgres` / `pass` |
| JWT secret env var | `JWT_SECRET_BASE64` |
