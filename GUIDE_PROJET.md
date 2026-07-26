# Guide du projet

Derniere mise a jour : 26/07/2026

## Objectif

Ce projet est un backend de prise de rendez-vous avec Spring Boot, JPA, Security, validation et PostgreSQL Neon.
Ce document sert de guide de progression. Il explique, dans l'ordre, ce qui a ete fait et pourquoi.

## Architecture cible

```text
com.kangoute.appointment
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
├── impl
├── util
├── config
└── security
```

## Journal chronologique

### 1. Correction de la structure de base

J'ai d'abord remis les classes Java dans des packages cohérents avec leur emplacement.

Pourquoi :
- IntelliJ et Maven doivent voir les packages et les chemins de fichiers au meme endroit.
- Sans ca, on obtient des erreurs du type `package ... does not correspond to file path`.

Actions :
- `PriseDeRendezVousApplication` a ete placee dans `src/main/java/com/kangoute/appointment`
- `SecurityConfig` a ete placee dans `src/main/java/com/kangoute/appointment/config`
- `Role`, `RoleName`, `RoleRepository`, `RoleService` et `RoleServiceImpl` ont ete remis dans les bons packages

### 2. Mise en place de Docker

J'ai ajoute :
- `Dockerfile`
- `docker-compose.yml`
- `.dockerignore`

Pourquoi :
- pour pouvoir lancer le backend facilement
- pour garder une base de demarrage propre

Important :
- la stack Docker a ensuite ete alignee sur Neon au lieu d'un PostgreSQL local
- le backend garde donc sa configuration de base dans `application.properties`

### 3. Dependances Maven

J'ai ajoute ou mis a jour :
- `spring-boot-devtools`
- `springdoc-openapi-starter-webmvc-ui` en `2.8.10`

Pourquoi :
- `devtools` facilite le developpement
- `springdoc` sert a exposer la documentation OpenAPI / Swagger

### 4. Authentification et securite

J'ai garde une configuration de base Spring Security.

Pourquoi :
- proteger les routes du backend
- laisser acces aux routes Swagger et aux endpoints necessaires au dev

J'ai ensuite ouvert explicitement :
- `/api/users/**`
- `/api/appointments/**`

J'ai aussi ajoute un `PasswordEncoder`.

Pourquoi :
- le mot de passe d'un utilisateur doit etre encode avant stockage
- c'est une base minimale propre pour une future authentification

### 5. Gestion des roles

J'ai garde l'entite `Role` et l'enum `RoleName`.

Pourquoi :
- un backend de prise de rendez-vous a besoin d'une base de roles pour gerer les profils et la securite

J'ai ajoute :
- `RoleRepository`
- `RoleService`
- `RoleServiceImpl`

Pourquoi :
- centraliser la creation et la lecture des roles
- eviter de dupliquer la logique partout

### 6. Couche User

J'ai ajoute :
- `User`
- `UserRepository`
- `UserService`
- `UserServiceImpl`
- `UserController`
- `UserCreateRequest`
- `UserResponse`
- `UserMapper`

Pourquoi :
- c'est la base metier du systeme de rendez-vous
- on doit pouvoir creer et retrouver un utilisateur

J'ai aussi ajoute :
- validation des champs d'entree
- `GlobalExceptionHandler`
- `ResourceNotFoundException`
- `DuplicateResourceException`

Pourquoi :
- renvoyer des erreurs claires
- eviter les exceptions techniques brutes
- proteger l'API contre les donnees invalides

Regles appliquees :
- `email` unique
- mot de passe encode
- attribution automatique du role `ROLE_USER`

### 7. Couche Appointment

J'ai ajoute :
- `Appointment`
- `AppointmentStatus`
- `AppointmentRepository`
- `AppointmentService`
- `AppointmentServiceImpl`
- `AppointmentCreateRequest`
- `AppointmentResponse`
- `AppointmentMapper`
- `AppointmentController`

Pourquoi :
- un rendez-vous est le coeur du projet
- il faut une couche complete pour creer, lire et lister les rendez-vous

Regles appliquees :
- `PENDING` est le statut par defaut
- un rendez-vous appartient a un utilisateur

### 8. Regles metier des rendez-vous

J'ai ajoute des regles metier dans `AppointmentServiceImpl`.

Pourquoi :
- un rendez-vous doit etre coherent dans le temps
- un meme utilisateur ne doit pas avoir deux rendez-vous qui se chevauchent

Regles appliquees :
- `startDateTime` doit etre strictement avant `endDateTime`
- un chevauchement de creneau est refuse
- erreurs dediees pour les cas metier

J'ai aussi ajoute :
- `InvalidAppointmentTimeException`
- `AppointmentConflictException`

Pourquoi :
- un rendez-vous ne doit jamais avoir une plage incoherente
- un seul patient ne doit pas pouvoir reserver deux creneaux qui se chevauchent
- l'API doit renvoyer une erreur claire au lieu d'une exception technique

Effet concret :
- `startDateTime` doit etre strictement avant `endDateTime`
- un rendez-vous conflictuel est refuse
- le statut `PENDING` reste la valeur par defaut a la creation

### 9. Guide de travail

J'ai ajoute ce fichier `GUIDE_PROJET.md`.

Pourquoi :
- garder un suivi chronologique du travail
- expliquer ce qui a ete fait et pourquoi
- servir de reference pour continuer le backend sans repartir de zero

### 10. Stabilisation des configurations

J'ai corrige plusieurs problemes de config et de merge.

Pourquoi :
- le projet a plusieurs fois change de package principal
- IntelliJ gardait parfois une ancienne classe principale
- certains fichiers avaient ete deplaces au mauvais endroit pendant les merges

Ce qui a ete corrige :
- classe principale Spring Boot
- `SecurityConfig`
- `RoleServiceImpl`
- position des fichiers sous `src/main/java`
- conflits Git de merge

### 11. Mise a jour et annulation des rendez-vous

J'ai ajoute la suite logique de `Appointment` :
- mise a jour d'un rendez-vous
- annulation d'un rendez-vous
- detection des conflits en ignorant le rendez-vous en cours de modification

Pourquoi :
- une prise de rendez-vous utile ne se limite pas a la creation
- l'utilisateur doit pouvoir corriger ou annuler un creneau
- les regles de disponibilite doivent rester valides apres modification

Effet concret :
- `PUT /api/appointments/{id}` modifie un rendez-vous existant
- `PATCH /api/appointments/{id}/cancel` annule un rendez-vous
- la validation de plage horaire reste appliquee
- les conflits de creneaux restent bloques

### 12. Authentification utilisateur

J'ai ajoute une premiere vraie couche d'authentification.

Pourquoi :
- le projet avait deja la gestion des utilisateurs et des roles
- il fallait un flux de connexion cohérent avant d'aller plus loin sur les droits et la protection des routes
- une authentification basique par session est suffisante a ce stade, sans ajouter de complexite JWT prematuree

Ce qui a ete ajoute :
- `CustomUserDetails`
- `CustomUserDetailsService`
- `AuthController`
- `AuthRequest`
- `AuthResponse`
- `AuthMapper`
- bean `AuthenticationManager`

Effet concret :
- `POST /api/auth/login` verifie l'email et le mot de passe
- la session Spring Security est enregistree
- l'utilisateur connecte est renvoye dans la reponse

## Etat actuel

Le projet compile et les tests passent.

Commandes deja valides :
```bash
mvn -q -DskipTests compile
mvn -q test
```

## Prochaine logique de travail

La suite naturelle est :
- enrichir `Appointment` avec update / cancel
- ajouter une couche de disponibilite ou de planning si le besoin metier le demande
- preparer l'authentification utilisateur
- nettoyer les warnings non bloquants de dev

## Regle de mise a jour

Ce fichier doit etre mis a jour a chaque bloc de travail important.
Ajouter :
- ce qui a ete fait
- pourquoi ca a ete fait
- ce qui reste a faire
