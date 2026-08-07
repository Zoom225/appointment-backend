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

### 13. Autorisation par roles

J'ai ajoute une premiere couche d'autorisation basee sur les roles.

Pourquoi :
- l'authentification seule ne suffit pas
- il faut separer ce qu'un utilisateur connecte peut faire de ce qu'un administrateur peut faire
- les routes ne doivent plus etre ouvertes sans controle apres la connexion

Ce qui a ete ajoute :
- `@EnableMethodSecurity`
- `@PreAuthorize` sur les methodes metier
- ouverture publique limitee a l'inscription et a la connexion

Effet concret :
- `POST /api/users/**` reste public pour l'inscription
- `POST /api/auth/**` reste public pour la connexion
- les autres operations `User` et `Appointment` demandent un role `USER` ou `ADMIN`

### 14. Gestion administrative des utilisateurs

J'ai ajoute une couche d'administration pour les utilisateurs.

Pourquoi :
- les roles servent maintenant a quelque chose de concret
- l'administration doit pouvoir superviser et corriger les comptes
- il faut pouvoir verifier, modifier ou supprimer un utilisateur sans passer par le parcours public

Ce qui a ete ajoute :
- `AdminUserController`
- `UserAdminUpdateRequest`
- `UserService.getAllUsers`
- `UserService.updateUser`
- `UserService.deleteUser`

Effet concret :
- `GET /api/admin/users` liste les utilisateurs
- `GET /api/admin/users/{id}` recupere un utilisateur
- `PUT /api/admin/users/{id}` modifie un utilisateur
- `DELETE /api/admin/users/{id}` supprime un utilisateur
- l'acces est reserve au role `ADMIN`

### 15. Gestion administrative des rendez-vous

J'ai ajoute une couche d'administration pour les rendez-vous.

Pourquoi :
- le role `ADMIN` doit pouvoir superviser les rendez-vous globalement
- il faut un canal d'administration qui ne depasse pas le parcours utilisateur
- les rendez-vous doivent pouvoir etre suivis et corriges sans ouvrir toute l'API

Ce qui a ete ajoute :
- `AdminAppointmentController`
- `AppointmentStatusUpdateRequest`
- `AppointmentService.updateStatus`

Effet concret :
- `GET /api/admin/appointments` liste tous les rendez-vous
- `GET /api/admin/appointments/{id}` recupere un rendez-vous
- `PATCH /api/admin/appointments/{id}/status` modifie le statut
- l'acces est reserve au role `ADMIN`

### 16. Disponibilite des rendez-vous

J'ai ajoute une regle de disponibilite de base et une consultation des creneaux libres.

Pourquoi :
- le projet avait deja la gestion des conflits, mais pas la notion de plage horaire exploitable
- il fallait bloquer les rendez-vous hors horaires de travail
- il fallait donner une vue simple des creneaux disponibles pour un utilisateur donne

Ce qui a ete ajoute :
- `AppointmentAvailabilityProperties`
- `AppointmentAvailabilityService`
- `AppointmentAvailabilityServiceImpl`
- `AppointmentAvailabilitySlotResponse`
- `AppointmentOutsideAvailabilityException`
- `GET /api/appointments/availability`

Regles appliquees :
- un rendez-vous doit commencer et finir le meme jour
- un rendez-vous doit rester dans les horaires de travail
- seuls les jours ouvrables definis sont autorises
- la consultation des creneaux libres se base sur des pas de 30 minutes

Tests ajoutes :
- creation refusee hors horaires
- creation valide dans les horaires
- creneau occupe absent de la liste des disponibilites

### 17. Variables d'environnement de lancement

J'ai ajoute un fichier d'exemple pour expliciter la configuration attendue au demarrage.

Pourquoi :
- la configuration de production ne doit pas rester en clair dans le repository
- le lancement local doit rester lisible sans deviner les variables attendues
- Docker et Spring Boot ont besoin des memes informations de connexion

Ce qui a ete ajoute :
- `.env.example`

Variables attendues :
- `PORT`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

### 18. Audit final de cohérence et de securite

J'ai resserre les controles d'acces sur les ressources metier.

Pourquoi :
- les routes de lecture et de modification ne devaient pas laisser un utilisateur agir sur les donnees d'un autre
- la securite ne doit pas reposer uniquement sur `@PreAuthorize`, mais aussi sur le proprietaire de la ressource
- le backend doit rester coherent entre authentification, autorisation et modele metier

Ce qui a ete ajoute :
- `CurrentUserService`
- verifications de proprietaire sur `UserController`
- verifications de proprietaire sur `AppointmentController`
- suppression de `formLogin` pour garder une API purement orientee session manuelle / endpoint de login
- handler 403 pour les violations d'acces

Tests ajoutes :
- creation de rendez-vous refusee pour un autre utilisateur
- consultation refusee d'un profil tiers
- consultation refusee d'un rendez-vous tiers
- mise a jour et annulation refusees sur un rendez-vous tiers

Effet concret :
- un utilisateur simple ne manipule plus que ses propres ressources
- l'admin garde un acces global
- les routes API gardent une regle lisible et testee

### 19. Pagination et filtres sur les listes

J'ai ajoute la pagination et des filtres de consultation sur les listes utilisateurs et rendez-vous.

Pourquoi :
- les endpoints de liste ne devaient pas renvoyer des volumes croissants sans limite
- l'administration a besoin d'interroger des sous-ensembles de donnees
- les filtres doivent rester derives de la base de donnees, pas faits en memoire

Ce qui a ete ajoute :
- `JpaSpecificationExecutor` sur `UserRepository` et `AppointmentRepository`
- `UserSpecifications`
- `AppointmentSpecifications`
- `Page<UserResponse>` pour `GET /api/admin/users`
- `Page<AppointmentResponse>` pour `GET /api/admin/appointments`
- `Page<AppointmentResponse>` pour `GET /api/appointments`

Filtres disponibles :
- utilisateurs: `query`, `role`
- rendez-vous: `userId`, `status`, `startFrom`, `startTo`

Tests ajoutes :
- pagination des utilisateurs
- filtre texte sur les utilisateurs
- filtre par role sur les utilisateurs
- pagination des rendez-vous
- filtre par statut sur les rendez-vous
- filtre par chevauchement de plage sur les rendez-vous

### 20. Historique et audit des rendez-vous

J'ai ajoute un historique metier des actions sur les rendez-vous.

Pourquoi :
- il fallait garder une trace lisible des changements importants
- l'administration doit pouvoir relire l'enchainement des actions
- les changements de rendez-vous doivent rester auditables sans parcourir les logs techniques

Ce qui a ete ajoute :
- `AppointmentAudit`
- `AppointmentAuditAction`
- `AppointmentAuditRepository`
- `AppointmentAuditService`
- `AppointmentAuditServiceImpl`
- `AppointmentAuditMapper`
- `AppointmentAuditResponse`
- `GET /api/admin/appointments/{id}/history`

Actions journalisees :
- creation
- mise a jour
- annulation
- changement de statut

Tests ajoutes :
- audit enregistre sur les actions de rendez-vous
- lecture de l'historique par l'admin
- fallback `SYSTEM` quand aucune authentification n'est presente

### 21. Notifications et rappels

J'ai ajoute une couche de notifications persistées pour les actions sur rendez-vous et un rappel planifie.

Pourquoi :
- les changements importants de rendez-vous doivent remonter au lieu d'etre seulement audites
- un utilisateur doit pouvoir consulter ses notifications depuis l'API
- le backend doit pouvoir generer un rappel sans intervention manuelle

Ce qui a ete ajoute :
- `AppointmentNotification`
- `AppointmentNotificationType`
- `AppointmentNotificationRepository`
- `AppointmentNotificationService`
- `AppointmentNotificationServiceImpl`
- `AppointmentNotificationMapper`
- `AppointmentNotificationResponse`
- `NotificationController`
- `AdminNotificationController`
- `AppointmentReminderScheduler`

Notifications generees :
- creation
- mise a jour
- annulation
- changement de statut
- rappel avant rendez-vous

Regles appliquees :
- chaque notification appartient a l'utilisateur concerne
- un rappel n'est genere qu'une fois par rendez-vous
- le scheduler de rappel est desactive en tests pour garder le build deterministe

Tests ajoutes :
- notifications de cycle de vie sur les rendez-vous
- marquage en lecture d'une notification
- generation d'un rappel unique pour un rendez-vous imminent

## Etat actuel

Le projet compile.

Les tests sont maintenant isoles de la base Neon via H2 en environnement de test, ce qui rend `mvn test` autonome dans le projet local.

La configuration de base de donnees de production n'est plus versionnee en clair dans le repository; elle doit venir des variables d'environnement.

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
- pousser un audit final de securite et de coherence des endpoints

## Regle de mise a jour

Ce fichier doit etre mis a jour a chaque bloc de travail important.
Ajouter :
- ce qui a ete fait
- pourquoi ca a ete fait
- ce qui reste a faire
