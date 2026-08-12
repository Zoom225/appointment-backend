# Backend de prise de rendez-vous

Backend Spring Boot pour gerer une application de prise de rendez-vous. Ce projet expose une API REST securisee, documentee avec Swagger, et pensee pour etre consommee par un frontend Angular.

## Objectif du backend

Le backend a ete mis en place pour couvrir les besoins suivants :

- creation et connexion des utilisateurs
- gestion des rendez-vous
- consultation des disponibilites
- notifications persistantes
- historique des actions sur les rendez-vous
- statistiques pour l administration

L authentification repose sur Spring Security avec un jeton JWT transmis en `Bearer`.

## Mise en place technique

Le projet a ete organise autour d une architecture classique en couches :

```text
com.kangoute.appointment
|-- config
|-- security
|-- controller
|-- dto
|   |-- request
|   `-- response
|-- entity
|-- enums
|-- exception
|-- mapper
|-- repository
|-- service
`-- service/impl
```

### Role des couches

- `controller` expose les endpoints HTTP
- `service` porte la logique metier
- `service/impl` contient les implementations
- `repository` accede a la base de donnees
- `entity` represente les donnees cote JPA
- `dto` definit les contrats d entree et de sortie
- `mapper` transforme les entites en DTO
- `security` gere l auth JWT et le filtrage des requetes
- `exception` centralise la gestion des erreurs metier

## Choix techniques

Le backend utilise :

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL en production
- H2 pour les tests et le demarrage local sans base externe
- JWT pour l authentication
- Swagger / OpenAPI pour la documentation
- Flyway pour les migrations
- Lombok pour reduire le boilerplate
- Docker pour le lancement conteneurise

## Fonctionnalites implementees

### Utilisateurs et authentification

- inscription utilisateur
- connexion
- generation du JWT
- gestion des roles
- compte de demonstration configurable

### Rendez-vous

- creation d un rendez-vous
- mise a jour d un rendez-vous
- annulation d un rendez-vous
- controle des conflits de creneaux
- verification des horaires autorises

### Disponibilites

- creneaux de travail configures par jour et par horaire
- generation de slots de rendez-vous
- controle du chevauchement

### Notifications et suivi

- notifications persistantes
- rappel automatique des rendez-vous
- journalisation des actions sur les rendez-vous

### Administration

- statistiques globales
- gestion admin des utilisateurs
- gestion admin des rendez-vous

## Configuration

Le projet lit sa configuration principale dans `src/main/resources/application.properties`.

Exemple des variables attendues :

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

### Points importants

- si aucune base externe n est fournie, l application peut demarrer avec H2
- le port est configurable via `PORT`
- Swagger est disponible sur `/swagger-ui.html`
- l API OpenAPI est disponible sur `/v3/api-docs`

## Demarrage en local

### Avec Maven

```bash
./mvnw clean test
./mvnw spring-boot:run
```

L application demarre par defaut sur `http://localhost:8081`.

### Avec Docker

Construction de l image :

```bash
docker build -t appointment-backend .
```

Demarrage avec Docker Compose :

```bash
docker compose up --build
```

## Structure du projet

Les principaux fichiers de code se trouvent dans `src/main/java/com/kangoute/appointment` :

- `PriseDeRendezVousApplication` : point d entree Spring Boot
- `config` : configuration globale, JWT, CORS, OpenAPI, donnees de demo
- `controller` : exposition des routes REST
- `dto` : objets echanges avec le frontend
- `entity` : modeles persistants
- `repository` : acces aux donnees
- `service` : contrats metier
- `service/impl` : logique applicative
- `security` : authentification et contexte utilisateur

## Regles metier principales

- un utilisateur ne peut pas avoir deux rendez-vous qui se chevauchent
- `startDateTime` doit etre strictement anterieur a `endDateTime`
- un rendez-vous a un statut par defaut a la creation
- les donnees sensibles ne sont pas exposees directement par les entites
- les erreurs metier sont converties en reponses HTTP lisibles

## Comptes de demonstration

Un compte de demonstration peut etre active pour presenter le projet sans creer de vraies donnees.

Activation :

```bash
APP_DEMO_ENABLED=true
```

Parametres par defaut :

- email : `demo@gestion-rendez-vous.com`
- mot de passe : `Demo2026!`
- role : `ROLE_USER`

## Deploiement

Le backend est compatible avec une execution type Render ou autre hebergeur Spring Boot.

Points a fournir au deploiement :

- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `APP_FRONTEND_URL`

## Documentation

- `GUIDE_PROJET.md` : suivi chronologique de la mise en place du backend
- `GUIDE_FRONTEND.md` : consignes pour l integration frontend

## Commandes utiles

```bash
./mvnw -q test
./mvnw -q -DskipTests compile
docker compose up --build
```
