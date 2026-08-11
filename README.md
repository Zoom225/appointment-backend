# Backend de prise de rendez-vous

Backend Spring Boot pour la gestion des rendez-vous, destiné a etre consomme par un frontend Angular.

## Presentation

L’application couvre :

- l’inscription et la connexion
- la gestion des rendez-vous
- la consultation des disponibilites
- les notifications persistées
- l’historique des actions
- les statistiques administrateur

L’authentification repose sur Spring Security avec JWT Bearer pour les appels API.

## Architecture

```text
com.kangoute.appointment
├── config
├── security
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── enums
├── exception
├── mapper
├── repository
├── service
└── service/impl
```

## Technologies

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- H2 pour les tests
- JWT
- Swagger / OpenAPI
- Lombok
- Maven
- Docker

## Installation

### Local

```bash
./mvnw clean test
./mvnw spring-boot:run
```

L’API demarre par defaut sur `http://localhost:8081`.

### Variables d’environnement

```bash
PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointment
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=http://localhost:4200
JWT_SECRET=VGhpcy1kZWZhdWx0LXNlY3JldC1tdXN0LWJlLXN1YnN0aXR1dGVkLWF0LXByb2R1Y3Rpb24=
JWT_EXPIRATION=PT2H
```

## Swagger

- UI : `/swagger-ui.html`
- JSON OpenAPI : `/v3/api-docs`

Une fois connecte, renseigner le jeton dans le schema `bearerAuth`.

## Docker

### Build

```bash
docker build -t appointment-backend .
```

### Execution

```bash
docker compose up --build
```

## Deploiement

Le backend est compatible avec Render :

- exposer le port via `PORT`
- connecter PostgreSQL Neon via `SPRING_DATASOURCE_URL`
- fournir `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`
- definir `JWT_SECRET`
- definir `CORS_ALLOWED_ORIGINS` sur l’URL du frontend Angular

## Structure fonctionnelle

## Compte de demonstration

Un compte de demonstration peut etre active pour presenter l'application a un recruteur ou un employeur.

- activation via `APP_DEMO_ENABLED=true`
- email : `demo@gestion-rendez-vous.com`
- mot de passe : `Demo2026!`
- role : `ROLE_USER`
- donnees fictives uniquement

Ce compte sert uniquement aux tests de l'application. Il ne donne aucun acces administrateur et n'utilise aucune donnee sensible.

- `controller` expose les endpoints HTTP
- `service` porte les contrats metier
- `service/impl` contient les implementations
- `repository` parle a la base de donnees
- `entity` mappe le domaine en JPA
- `dto` protege le contrat API
- `mapper` convertit les entites en DTO
- `security` gere l’authentification JWT
- `exception` centralise les erreurs metier

## Diagramme d’architecture

```text
[Angular]
    |
    v
[Controllers] -> [Services] -> [Repositories] -> [PostgreSQL / H2]
       |             |
       |             +--> [Security / JWT]
       +--> [DTO / Mapper]
```

## Commandes utiles

```bash
./mvnw -q test
./mvnw -q -DskipTests compile
docker compose up --build
```
