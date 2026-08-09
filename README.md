# Appointment Backend

Backend Spring Boot de gestion de rendez-vous pour consommation par un frontend Angular.

## Présentation

L’application couvre:

- inscription et connexion
- gestion des rendez-vous
- disponibilité par créneau
- notifications persistées
- historique des actions
- statistiques administrateur

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
- Spring Boot 4.1
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

L’API démarre par défaut sur `http://localhost:8081`.

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

- UI: `/swagger-ui.html`
- OpenAPI: `/v3/api-docs`

Une fois connecté, renseigner le jeton dans le schéma `bearerAuth`.

## Docker

### Build

```bash
docker build -t appointment-backend .
```

### Run

```bash
docker compose up --build
```

## Déploiement

Le backend est compatible avec Render:

- exposer le port via `PORT`
- connecter PostgreSQL Neon via `SPRING_DATASOURCE_URL`
- fournir `SPRING_DATASOURCE_USERNAME` et `SPRING_DATASOURCE_PASSWORD`
- définir `JWT_SECRET`
- définir `CORS_ALLOWED_ORIGINS` sur l’URL du frontend Angular

## Structure fonctionnelle

- `controller` expose les endpoints HTTP
- `service` porte les contrats métier
- `service/impl` contient les implémentations
- `repository` parle à la base de données
- `entity` mappe le domaine en JPA
- `dto` protège le contrat API
- `mapper` convertit les entités en DTO
- `security` gère l’authentification JWT
- `exception` centralise les erreurs métiers

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
