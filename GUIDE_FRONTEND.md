# Guide frontend

Derniere mise a jour : 07/08/2026

## Objectif

Ce document sert de base pour construire le frontend du projet de prise de rendez-vous.
Il resume les ecrans a faire, les donnees a consommer, les regles metier a respecter et les priorites.

## Principes

- utiliser les endpoints exposes par le backend existant
- respecter les roles `USER` et `ADMIN`
- ne pas dupliquer la logique metier du backend dans l'UI
- afficher les erreurs de facon claire
- garder une navigation simple et directe

## Parcours utilisateur

### Utilisateur standard

- creer un compte
- se connecter
- voir ses rendez-vous
- creer un rendez-vous
- modifier un rendez-vous
- annuler un rendez-vous
- consulter ses notifications
- marquer une notification comme lue
- voir les disponibilites

### Administrateur

- se connecter
- voir tous les utilisateurs
- voir tous les rendez-vous
- filtrer et paginer les listes
- changer le statut d'un rendez-vous
- consulter l'historique d'un rendez-vous
- consulter les notifications globales
- consulter les statistiques admin

## Endpoints principaux

### Authentification

- `POST /api/auth/login`

### Utilisateurs

- `POST /api/users`
- `GET /api/users/{id}`
- `GET /api/users`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`

### Rendez-vous

- `POST /api/appointments`
- `GET /api/appointments`
- `GET /api/appointments/{id}`
- `PUT /api/appointments/{id}`
- `PATCH /api/appointments/{id}/cancel`
- `GET /api/appointments/availability`
- `GET /api/admin/appointments`
- `GET /api/admin/appointments/{id}`
- `PATCH /api/admin/appointments/{id}/status`
- `GET /api/admin/appointments/{id}/history`

### Notifications

- `GET /api/notifications`
- `PATCH /api/notifications/{id}/read`
- `GET /api/admin/notifications`

### Statistiques

- `GET /api/admin/statistics`

## Donnees a afficher

### User

- id
- firstName
- lastName
- email
- roles

### Appointment

- id
- user
- startDateTime
- endDateTime
- reason
- status
- reminderSentAt

### Notification

- id
- recipientId
- appointmentId
- type
- message
- readAt
- createdAt

### Audit

- id
- appointmentId
- action
- details
- createdAt
- actorEmail

### Statistics

- totalUsers
- activeUsersLast30Days
- totalAppointments
- appointmentsInPeriod
- pendingAppointments
- confirmedAppointments
- cancelledAppointments
- periodFrom
- periodTo

## Regles importantes

- un utilisateur ne voit que ses propres donnees
- un admin voit les donnees globales
- les rendez-vous ne doivent pas se chevaucher pour un meme utilisateur
- un rendez-vous doit respecter les horaires ouvrables
- les notifications sont paginees
- les listes admin sont paginees et filtrables

## Ecrans a creer

### Auth

- connexion
- inscription

### Utilisateur

- tableau de bord
- liste de mes rendez-vous
- formulaire de creation de rendez-vous
- formulaire d'edition de rendez-vous
- detail rendez-vous
- calendrier ou vue disponibilites
- notifications

### Admin

- tableau de bord statistiques
- liste utilisateurs
- detail utilisateur
- liste rendez-vous
- detail rendez-vous
- historique rendez-vous
- notifications admin

## Priorite d'implementation

1. auth et session
2. layout principal
3. liste rendez-vous utilisateur
4. creation et modification rendez-vous
5. notifications
6. vues admin
7. statistiques admin

## Source de verite

- Swagger UI pour verifier les routes et les schemas
- `GUIDE_PROJET.md` pour comprendre les choix backend
- les tests d'integration backend pour les regles metier

## Conseils d'implementation

- centraliser les appels API dans un client unique
- gerer les codes 401, 403, 404, 409 et 400
- utiliser la pagination serveur pour les listes
- ne pas recalculer les regles de disponibilite cote frontend
- afficher les dates en format local lisible

