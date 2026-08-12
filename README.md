# Gestion de rendez-vous — Backend Spring Boot

Backend REST de l'application Full Stack **Gestion de rendez-vous**, développé avec **Java 21 et Spring Boot**.

Cette API assure l'authentification, la sécurité, la gestion des utilisateurs, des rendez-vous, des disponibilités et des notifications. Elle est destinée à être consommée par le frontend Angular de l'application.

## Démo en production

| Service | Technologie / Hébergement |
|---|---|
| Backend | Spring Boot / Render |
| Frontend | Angular / Vercel |
| Base de données | PostgreSQL |
| Documentation API | Swagger / OpenAPI |
| Authentification | Spring Security / JWT |

### Liens

Backend :

`https://appointment-backend-vab1.onrender.com`

Swagger :

`https://appointment-backend-vab1.onrender.com/swagger-ui/index.html`

API :

`https://appointment-backend-vab1.onrender.com/api`

> Le backend étant hébergé sur Render, le premier appel peut prendre quelques secondes lorsque l'instance sort de veille.

---

## Fonctionnalités

### Authentification et utilisateurs

- inscription d'un utilisateur ;
- connexion sécurisée ;
- génération d'un JWT ;
- authentification par Bearer Token ;
- gestion des rôles ;
- protection des endpoints avec Spring Security ;
- compte de démonstration configurable.

### Rendez-vous

- création d'un rendez-vous ;
- consultation des rendez-vous ;
- modification d'un rendez-vous ;
- annulation d'un rendez-vous ;
- gestion des statuts ;
- contrôle des conflits de créneaux ;
- vérification des horaires autorisés ;
- historique des actions.

### Disponibilités

- consultation des disponibilités ;
- gestion des horaires de travail ;
- génération des créneaux disponibles ;
- contrôle des chevauchements avec les rendez-vous existants.

### Notifications

- création de notifications persistantes ;
- notifications liées aux rendez-vous ;
- rappels ;
- suivi des modifications et annulations.

### Administration

- gestion des utilisateurs ;
- gestion des rendez-vous ;
- statistiques globales.

---

## Stack technique

- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- JWT
- PostgreSQL
- H2
- Flyway
- OpenAPI / Swagger
- Lombok
- Maven
- Docker
- JUnit / Spring Boot Test

---

## Architecture

Le backend suit une architecture en couches avec séparation des responsabilités :

```text
src/main/java/com/kangoute/appointment
│
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
│   └── impl
│
└── PriseDeRendezVousApplication.java
```

### Responsabilité des couches

**Controller**

Expose les endpoints REST et reçoit les requêtes HTTP.

```text
HTTP Request
      ↓
Controller
```

**Service**

Contient les contrats de la logique métier.

**Service / Impl**

Contient l'implémentation de la logique métier de l'application.

**Repository**

Communique avec PostgreSQL grâce à Spring Data JPA.

**Entity**

Représente les données persistées dans la base de données.

**DTO**

Définit les données acceptées et retournées par l'API sans exposer directement les entités JPA.

**Mapper**

Assure les conversions entre entités et DTO.

**Security**

Gère l'authentification JWT et la protection des ressources.

**Exception**

Centralise la gestion des erreurs de l'API.

---

## Flux d'une requête

L'architecture générale suit le flux :

```text
Frontend Angular
       ↓
HTTP / REST
       ↓
JWT Bearer Token
       ↓
Spring Security
       ↓
Controller
       ↓
Service
       ↓
ServiceImpl
       ↓
Repository
       ↓
Spring Data JPA
       ↓
PostgreSQL
```

Cette séparation permet de conserver une architecture maintenable et de limiter les responsabilités de chaque couche.

---

## Authentification JWT

L'authentification repose sur **Spring Security et JWT**.

Flux de connexion :

```text
Utilisateur
     ↓
POST /api/auth/login
     ↓
Spring Security
     ↓
Vérification des identifiants
     ↓
Génération JWT
     ↓
Frontend Angular
     ↓
Authorization: Bearer <token>
     ↓
API protégée
```

Le frontend transmet ensuite automatiquement le JWT dans les requêtes nécessitant une authentification.

La sécurité réelle des ressources est contrôlée côté backend par Spring Security.

---

## API REST

### Authentification

```http
POST /api/auth/login
```

### Rendez-vous

Exemples :

```http
POST /api/appointments
PATCH /api/appointments/{id}
```

La liste complète et les contrats des endpoints sont disponibles dans Swagger :

`https://appointment-backend-vab1.onrender.com/swagger-ui/index.html`

---

## Règles métier principales

Le backend applique notamment les règles suivantes :

- un utilisateur ne peut pas avoir deux rendez-vous qui se chevauchent ;
- la date de début doit être strictement antérieure à la date de fin ;
- les créneaux doivent respecter les disponibilités autorisées ;
- un rendez-vous reçoit un statut lors de sa création ;
- les modifications passent par la couche métier ;
- les données sensibles ne sont pas exposées directement à travers les entités JPA ;
- les erreurs métier sont converties en réponses HTTP adaptées.

---

## Configuration

La configuration principale se trouve dans :

```text
src/main/resources/application.properties
```

Les informations sensibles sont fournies au moyen de variables d'environnement.

Exemple :

```bash
PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/appointment
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
CORS_ALLOWED_ORIGINS=http://localhost:4200
JWT_SECRET=change-this-secret
JWT_EXPIRATION=PT2H
APP_DEMO_ENABLED=false
APP_FRONTEND_URL=http://localhost:4200
```

> Les véritables secrets de production ne doivent jamais être enregistrés dans Git.

---

## Base de données

En production, l'application utilise :

```text
PostgreSQL
```

La persistance est gérée avec :

```text
Spring Data JPA
```

Les migrations de base de données sont gérées avec :

```text
Flyway
```

H2 peut être utilisé pour les tests ou certains environnements locaux.

---

## Compte de démonstration

Le projet permet d'activer un compte de démonstration destiné à la présentation de l'application.

Activation :

```bash
APP_DEMO_ENABLED=true
```

Compte de démonstration :

```text
Email : demo@gestion-rendez-vous.com
Mot de passe : Demo2026!
Rôle : ROLE_USER
```

> Ce compte est destiné uniquement à la démonstration de l'application.

---

## Installation locale

### Prérequis

- Java 21
- Maven ou Maven Wrapper
- PostgreSQL, selon la configuration choisie

Vérifier Java :

```bash
java -version
```

---

## Lancer les tests

Sous Windows :

```bash
mvnw.cmd test
```

Sous Linux/macOS :

```bash
./mvnw test
```

---

## Démarrer l'application

Sous Windows :

```bash
mvnw.cmd spring-boot:run
```

Sous Linux/macOS :

```bash
./mvnw spring-boot:run
```

Par défaut :

```text
http://localhost:8081
```

Swagger local :

```text
http://localhost:8081/swagger-ui/index.html
```

---

## Docker

Construire l'image :

```bash
docker build -t appointment-backend .
```

Démarrer avec Docker Compose :

```bash
docker compose up --build
```

---

## Tests

Le backend dispose de tests permettant de vérifier différentes couches de l'application.

Les tests permettent notamment de contrôler :

- la logique métier ;
- les services ;
- les contrôleurs ;
- les repositories ;
- les validations ;
- certains comportements de sécurité.

Commande :

```bash
./mvnw test
```

Sous Windows :

```bash
mvnw.cmd test
```

---

## Déploiement

Le backend est actuellement déployé sur **Render**.

Les principales variables nécessaires en production sont :

```text
PORT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
APP_FRONTEND_URL
```

Le frontend Angular déployé sur Vercel communique avec cette API en HTTPS.

---

## Sécurité

Le projet applique plusieurs mécanismes de sécurité :

- Spring Security ;
- authentification JWT ;
- Bearer Token ;
- endpoints protégés ;
- contrôle des rôles ;
- validation des entrées ;
- DTO pour limiter l'exposition des entités ;
- gestion centralisée des exceptions ;
- configuration CORS ;
- secrets fournis par variables d'environnement.

---

## Documentation complémentaire

Le projet contient également :

```text
GUIDE_PROJET.md
GUIDE_FRONTEND.md
```

`GUIDE_PROJET.md` documente la mise en place du backend.

`GUIDE_FRONTEND.md` contient les informations utiles pour l'intégration avec le frontend Angular.

---

## Commandes utiles

```bash
./mvnw test
./mvnw -DskipTests compile
./mvnw spring-boot:run
docker compose up --build
```

---

## État du projet

- Backend Spring Boot développé
- API REST opérationnelle
- Authentification JWT opérationnelle
- Spring Security configuré
- PostgreSQL connecté
- Gestion des rendez-vous opérationnelle
- Disponibilités opérationnelles
- Notifications opérationnelles
- Swagger disponible
- Tests backend présents
- Backend déployé sur Render
- Frontend Angular connecté
- Application Full Stack disponible en production

---

## Objectif du projet

Ce projet démontre la conception et le développement d'un backend professionnel basé sur :

- Java ;
- Spring Boot ;
- architecture en couches ;
- API REST ;
- Spring Security ;
- JWT ;
- JPA ;
- PostgreSQL ;
- tests automatisés ;
- Docker ;
- déploiement cloud.

Il constitue un projet de démonstration destiné à présenter mes compétences en développement **Java / Spring Boot / Angular**.