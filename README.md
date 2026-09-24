# Gestion de rendez-vous — Backend Spring Boot

[![Backend CI](https://github.com/Zoom225/appointment-backend/actions/workflows/backend-ci.yml/badge.svg?branch=master)](https://github.com/Zoom225/appointment-backend/actions/workflows/backend-ci.yml)

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

- un utilisateur ne peut pas avoir plusieurs rendez-vous actifs futurs ;
- aucun créneau occupé ne peut être réservé par un autre utilisateur ;
- la date de début doit être strictement antérieure à la date de fin ;
- les créneaux doivent respecter les disponibilités autorisées ;
- un rendez-vous reçoit un statut lors de sa création ;
- les modifications passent par la couche métier ;
- les données sensibles ne sont pas exposées directement à travers les entités JPA ;
- les erreurs métier sont converties en réponses HTTP adaptées.

Le workflow conserve la validation ADMIN : `PENDING → CONFIRMED → COMPLETED`,
avec annulation possible tant que le rendez-vous est actif. L'identité de réservation
vient du JWT. Les détails des endpoints, transitions, notifications et verrous sont
documentés dans [Réservation et gestion des rendez-vous](docs/appointment-booking.md).

---

## Configuration des environnements

La configuration est séparée en trois fichiers :

- `application.properties` : propriétés communes (port, Swagger, expiration JWT, disponibilité et notifications).
- `application-dev.properties` : H2 en mémoire par défaut, `ddl-auto=update`, origines locales et clé JWT publique réservée au développement.
- `application-prod.properties` : PostgreSQL et secret JWT fournis exclusivement au runtime.

Aucun profil n'est activé par défaut dans l'application. Choisir explicitement `dev`
ou `prod` ; ne jamais les activer ensemble. Sans profil et sans configuration JWT,
le démarrage échoue.

### Développement local

Sous PowerShell :

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

Sous Linux/macOS :

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Aucune base externe ni aucun secret n'est nécessaire pour ce lancement local.
H2 utilise une base en mémoire, perdue à l'arrêt. Les seules origines CORS du profil
dev sont `http://localhost:4200` et `http://127.0.0.1:4200`.
Les variables datasource permettent d'utiliser une base locale différente si souhaité.

`.env.example` est un modèle documentaire : Spring Boot ne charge pas automatiquement
les fichiers `.env`. Renseigner les variables dans le terminal, la configuration de
l'IDE ou le service d'hébergement. Laisser les variables optionnelles inutilisées
**non définies**, plutôt que les exporter avec une valeur vide : une valeur vide
remplace la valeur par défaut.

### Production sur Render

Définir dans les variables d'environnement du service Render :

| Variable | Valeur attendue |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | URL JDBC PostgreSQL, par exemple `jdbc:postgresql://<host>:5432/<database>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur PostgreSQL, obligatoire |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL, obligatoire |
| `JWT_SECRET` | Secret Base64 aléatoire d'au moins **32 octets avant encodage**, obligatoire |
| `JWT_EXPIRATION` | `PT2H` par défaut ; durée ISO-8601 |
| `APP_FRONTEND_URL` ou `FRONTEND_URL` | Origine exacte du frontend, par exemple `https://mon-projet.vercel.app` |
| `CORS_ALLOWED_ORIGINS` | Facultatif : origines supplémentaires exactes, séparées par des virgules |
| `APP_DEMO_ENABLED` | `false` par défaut |
| `PORT` | Fourni par l'hébergeur, `8081` par défaut |

`APP_FRONTEND_URL` est prioritaire ; laisser la variable frontend inutilisée non
définie. Ne pas utiliser de chemin ni de slash final dans les origines. Aucun
localhost ni wildcard Vercel n'est ajouté en production. Sans origine configurée,
les requêtes cross-origin des navigateurs sont refusées.

Pour les deux frontends Vercel actuellement utilisés, définir sur Render :

```text
APP_FRONTEND_URL=https://appointment-front-gilt.vercel.app
CORS_ALLOWED_ORIGINS=https://appointment-front-gilt.vercel.app,https://gestion-de-rendez-vous-77exox4z0-kangoute.vercel.app
```

Les origines doivent être exactes, sans wildcard ni slash final. Séparer plusieurs
origines par des virgules. La duplication de l'origine principale est ignorée.

Le démarrage prod échoue avant l'initialisation de la base si une variable datasource
ou `JWT_SECRET` est absente ou vide, si l'URL n'est pas PostgreSQL, ou si le secret
n'est pas un Base64 d'au moins 32 octets. Les messages de validation nomment la
variable sans afficher sa valeur. Ne jamais réutiliser la clé publique du profil dev.

Générer le secret dans un environnement de confiance (par exemple avec
`openssl rand -base64 32`) et le renseigner directement dans les variables sécurisées
de Render. Ne jamais enregistrer de véritable secret, mot de passe ou token dans Git.
Si la démo est volontairement activée en production, définir aussi
`APP_DEMO_USER_EMAIL`, `APP_DEMO_USER_PASSWORD`, `APP_DEMO_ADMIN_EMAIL` et
`APP_DEMO_ADMIN_PASSWORD`. Les mots de passe locaux ne sont pas utilisés en prod.

### JPA, Flyway et tests

Les profils dev et prod conservent explicitement `ddl-auto=update`, Flyway activé et
`baseline-on-migrate=true`. Le passage à `ddl-auto=validate` et la migration complète
du schéma seront traités séparément. La migration V3 ajoute le verrou de réservation
et les dates d'audit du rendez-vous, sans supprimer de données existantes.

Les tests utilisent `src/test/resources/application.properties`, avec H2,
`ddl-auto=create-drop` et une clé JWT publique propre aux tests. Aucun profil dev/prod
ni aucune variable de production n'est nécessaire pour lancer `mvnw.cmd clean test`.

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

Le projet permet d'activer deux comptes publics réservés à la démonstration de l'application.

Activation :

```bash
APP_DEMO_ENABLED=true
```

Comptes de démonstration locaux (profil `dev`) :

```text
USER  : demo.user@appointment.local / DemoUser2026! / ROLE_USER
ADMIN : demo.admin@appointment.local / DemoAdmin2026! / ROLE_ADMIN
```

> Ces comptes sont publics et destinés uniquement à la démonstration. En production,
> `APP_DEMO_ENABLED=false` par défaut ; les quatre variables d'identifiants ci-dessus
> sont requises si la démo est activée. Les mots de passe sont encodés avant stockage.
> L'initialiseur ne modifie aucun compte existant et ne précharge aucun rendez-vous actif.

Le compte DEMO ADMIN conserve `ROLE_ADMIN`, mais ses accès backend sont limités aux
rendez-vous, statistiques et notifications du DEMO USER. La liste des utilisateurs
ne montre que les deux comptes démo ; leurs modifications par le DEMO ADMIN sont
refusées pour empêcher toute promotion de rôle. Les accès directs par identifiant
et les filtres restent soumis à la même limite. Un ADMIN ordinaire conserve ses
droits actuels. Quand `APP_DEMO_ENABLED=false`, les identifiants démo configurés
ne peuvent plus s'authentifier, même si leurs lignes existent encore en base.
La migration V4 ajoute `demo_account_type` (`NONE`, `USER`, `ADMIN`) aux comptes.
Les comptes historiques restent `NONE`. L'identité démo et le blocage de connexion
quand la démo est désactivée reposent sur ce marqueur persistant, pas sur l'email.
Les variables `APP_DEMO_USER_EMAIL` et `APP_DEMO_ADMIN_EMAIL` servent à la création
initiale : leur modification ultérieure ne renomme pas les comptes déjà marqués.
Si une adresse configurée appartient déjà à un compte non démo, le démarrage échoue
sans convertir ni modifier ce compte.
La création d'un rendez-vous démo génère la notification utilisateur habituelle
et une notification « Nouveau rendez-vous » destinée au DEMO ADMIN, liée au même
rendez-vous. Les autres réservations ne génèrent aucune notification démo.

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
mvnw.cmd "-Dspring-boot.run.profiles=dev" spring-boot:run
```

Sous Linux/macOS :

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
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

Le Dockerfile ne contient aucun secret ni profil actif. Fournir le profil et les
variables de production au runtime (Render ou `docker run --env-file <fichier-prive>`).
Ne jamais ajouter de secret via `ARG`, `ENV` ou `COPY` dans l'image.

Démarrer en développement avec Docker Compose (profil `dev` explicite) :

```bash
docker compose up --build
```

---

## Tests

### CI GitHub Actions

Le workflow **Backend CI** s'exécute à chaque push vers `master` et à chaque Pull
Request vers `master`. Sur Ubuntu avec Java 21 Temurin et le cache Maven, il lance
successivement `./mvnw clean test` puis `./mvnw clean package -DskipTests`.
Tout échec des tests ou du build fait échouer la CI.

Les tests utilisent la configuration H2 de `src/test/resources/application.properties`,
sans activation du profil prod ni secret de production. Un nouveau commit annule
le run précédent encore en cours pour la même branche ou PR.

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

Configurer les variables décrites dans [Configuration des environnements](#configuration-des-environnements), notamment `SPRING_PROFILES_ACTIVE=prod`.

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
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
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

Le contrat de réservation avec coordonnées, la migration V5, le QR de vérification et les variables SMTP sont décrits dans [Réservation, QR et email](docs/appointment-confirmation-qr-mail.md).
