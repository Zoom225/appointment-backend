# Réservation et gestion des rendez-vous

Le backend conserve Controller → Service → Repository, ses DTO, ses mappers,
JWT, les notifications persistantes et les endpoints existants.

## Réserver

`POST /api/appointments`, authentifié, retourne `201` et `AppointmentResponse` :

```json
{
  "startDateTime": "2030-01-08T10:00:00",
  "endDateTime": "2030-01-08T10:30:00",
  "reason": "Consultation"
}
```

Choisir une date future réelle à partir des disponibilités. Le backend utilise
l'identité JWT. L'ancien champ `userId` reste facultatif pour compatibilité :
un USER peut uniquement fournir son propre identifiant ; un ADMIN peut réserver
pour un autre utilisateur. Le statut initial est toujours `PENDING`.

La date doit être strictement future. Les jours et horaires configurés sont
respectés. Le début s'aligne sur la grille depuis l'ouverture ; la durée doit être
un multiple positif de la durée de créneau configurée (30 minutes par défaut).
Les dates locales utilisent le fuseau horaire du serveur, comme le modèle existant.

Un seul rendez-vous futur `PENDING`, `CONFIRMED` ou ancien `SCHEDULED` est autorisé
par utilisateur, même pour deux dates différentes. `CANCELLED` et `COMPLETED`
libèrent la réservation. Les chevauchements entre **tous** les utilisateurs sont
refusés. Deux rendez-vous adjacents ne se chevauchent pas.

Les conflits renvoient `409`, les horaires invalides `400`, les accès non
authentifiés `401` et les droits insuffisants `403`, au format `ApiErrorResponse`.

## Endpoints utilisateur

| Méthode et route | Usage |
|---|---|
| `POST /api/appointments` | Réserver pour l'utilisateur JWT |
| `GET /api/appointments` | Ses rendez-vous ; pagination, statut, `startFrom`, `startTo` |
| `GET /api/appointments/{id}` | Consulter son rendez-vous |
| `GET /api/appointments/me/upcoming` | Actifs futurs, tri croissant ; `size=1` pour le prochain |
| `GET /api/appointments/me/history` | Passés ou terminés/annulés, tri décroissant, pagination |
| `PUT /api/appointments/{id}` | Modifier un rendez-vous actif en revalidant la réservation |
| `PATCH /api/appointments/{id}/cancel` | Annuler son rendez-vous actif |
| `PATCH /api/appointments/{id}` | Ancienne route conservée : USER limité à `CANCELLED` |
| `GET /api/appointments/availability?date=2030-01-08` | Créneaux futurs du calendrier partagé ; `userId` facultatif |
| `GET /api/notifications` | Ses notifications, pagination et filtres existants |
| `PATCH /api/notifications/{id}/read` | Marquer sa notification comme lue |

Les listes personnelles ignorent toute identité extérieure au JWT. Les accès
individuels et mutations vérifient aussi le propriétaire dans le service.
La disponibilité indique l'occupation du calendrier ; la réservation revalide
toujours le créneau et la limite par utilisateur au moment de l'écriture.

## Administration

Toutes les routes `/api/admin/...` nécessitent `ROLE_ADMIN`.

| Méthode et route | Usage |
|---|---|
| `GET /api/admin/appointments` | Tous les rendez-vous ; `page`, `size`, `sort`, `userId`, `status`, `startFrom`, `startTo`, `query` |
| `GET /api/admin/appointments/{id}` | Détail |
| `PATCH /api/admin/appointments/{id}/status` | Confirmer, terminer ou annuler |
| `GET /api/admin/appointments/{id}/history` | Audit existant des actions et acteurs |
| `GET /api/admin/statistics` | Statistiques existantes enrichies |
| `GET /api/admin/users` | Utilisateurs avec pagination et filtres existants |
| `GET /api/admin/notifications` | Notifications avec les filtres existants |

`query` recherche l'email, le prénom ou le nom sans distinction de casse.
Les filtres temporels existants sélectionnent les rendez-vous chevauchant la période.
Le dashboard conserve ses anciens champs et ajoute `todayAppointments`,
`upcomingAppointments` (actifs futurs) et `completedAppointments`. Les totaux sont
calculés par des requêtes SQL de comptage, sans chargement intégral des entités.

Transitions : `PENDING → CONFIRMED/CANCELLED`, `CONFIRMED → COMPLETED/CANCELLED`.
Pour compatibilité : `PENDING → SCHEDULED → CONFIRMED/CANCELLED`.
Les états terminaux ne sont ni réouverts, ni modifiés, ni annulés une deuxième fois.
Une transition incohérente retourne `409`. Un USER ne peut plus confirmer ou terminer
lui-même un rendez-vous, y compris via l'ancienne route PATCH.

## Notifications et historique

Création : notification de demande en attente. Confirmation ADMIN : titre
« Rendez-vous confirmé » et message contenant la date réelle du rendez-vous.
Annulation : « Votre rendez-vous a été annulé. » Les types de notifications
existants restent inchangés. L'écriture du rendez-vous, de l'audit et de sa notification
partage la même transaction. Les rappels excluent les rendez-vous terminés/annulés.

## Concurrence et migration

`V3__booking_lock_and_appointment_timestamps.sql` crée une ligne de verrou technique
et ajoute `created_at` / `updated_at` si la table existe déjà. Aucune suppression ni
correction automatique des anciennes réservations n'est effectuée. Leurs dates
d'audit inconnues restent nulles ; les nouvelles écritures sont horodatées.

Le starter Flyway Spring Boot est nécessaire pour exécuter les migrations ; la
dépendance Flyway seule ne chargeait pas l'auto-configuration avec Spring Boot 4.
La version Spring Boot reste inchangée. Le comportement JPA `update` est conservé.

Chaque mutation prend `SELECT ... FOR UPDATE` sur la ligne du calendrier, avant
les vérifications, et conserve le verrou jusqu'au commit/rollback. Cette stratégie
protège les créneaux vides et les demandes d'un même utilisateur sur des dates
différentes, y compris entre plusieurs instances utilisant la même base.
Le scheduler et l'initialisation démo utilisent également ce verrou.

Compromis : les écritures du calendrier sont sérialisées. Pour un agenda à fort
volume ou plusieurs praticiens, envisager des verrous par ressource et des
contraintes adaptées. Les écritures SQL manuelles ou services externes contournant
ce protocole ne sont pas couvertes. Ne pas supprimer la ligne du verrou.

Les tests couvrent les courses entre transactions sur H2 et la conservation des
données par la migration. Avant un futur déploiement, vérifier la migration et les
verrous sur une base PostgreSQL de préproduction ; aucune connexion Neon/Render
n'a été utilisée. Les éventuelles incohérences historiques restent à examiner
manuellement. Le jeu démo neuf contient un seul rendez-vous actif futur.

## Fichiers concernés par cette évolution

### Créés

- `docs/appointment-booking.md`
- `src/main/java/com/kangoute/appointment/config/TimeConfig.java`
- `src/main/resources/db/migration/V3__booking_lock_and_appointment_timestamps.sql`
- `src/test/java/com/kangoute/appointment/AppointmentBookingIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentConcurrencyIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentMigrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentTestDates.java`

### Modifiés

- `README.md`
- `pom.xml`
- `src/main/java/com/kangoute/appointment/config/DemoDataInitializer.java`
- `src/main/java/com/kangoute/appointment/controller/AdminAppointmentController.java`
- `src/main/java/com/kangoute/appointment/controller/AdminStatisticsController.java`
- `src/main/java/com/kangoute/appointment/controller/AppointmentController.java`
- `src/main/java/com/kangoute/appointment/dto/request/AppointmentCreateRequest.java`
- `src/main/java/com/kangoute/appointment/dto/request/AppointmentUpdateRequest.java`
- `src/main/java/com/kangoute/appointment/dto/response/AdminStatisticsResponse.java`
- `src/main/java/com/kangoute/appointment/dto/response/AppointmentResponse.java`
- `src/main/java/com/kangoute/appointment/entity/Appointment.java`
- `src/main/java/com/kangoute/appointment/enums/AppointmentStatus.java`
- `src/main/java/com/kangoute/appointment/exception/GlobalExceptionHandler.java`
- `src/main/java/com/kangoute/appointment/mapper/AppointmentMapper.java`
- `src/main/java/com/kangoute/appointment/repository/AppointmentRepository.java`
- `src/main/java/com/kangoute/appointment/repository/specification/AppointmentSpecifications.java`
- `src/main/java/com/kangoute/appointment/service/AppointmentService.java`
- `src/main/java/com/kangoute/appointment/service/impl/AdminStatisticsServiceImpl.java`
- `src/main/java/com/kangoute/appointment/service/impl/AppointmentAvailabilityServiceImpl.java`
- `src/main/java/com/kangoute/appointment/service/impl/AppointmentNotificationServiceImpl.java`
- `src/main/java/com/kangoute/appointment/service/impl/AppointmentServiceImpl.java`
- `src/test/java/com/kangoute/appointment/AdminStatisticsIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentAccessControlIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentAuditIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentAvailabilityIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentNotificationIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/AppointmentStatusPatchIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/DemoDataInitializerEnabledTests.java`
- `src/test/java/com/kangoute/appointment/GlobalExceptionHandlerTests.java`
- `src/test/java/com/kangoute/appointment/NotificationPaginationIntegrationTests.java`
- `src/test/java/com/kangoute/appointment/PaginationAndFilterIntegrationTests.java`
