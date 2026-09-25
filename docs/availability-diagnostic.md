# Diagnostic des disponibilités du 5 octobre 2026

## Résultat local reproductible

Avec une horloge fixée au 25 septembre 2026, le service initial retourne déjà 18 créneaux le lundi 5 octobre 2026 lorsque le calendrier est libre : de 09:00–09:30 à 17:30–18:00. Le test HTTP authentifié sans `userId` confirme le contrat `[{startDateTime, endDateTime}]`.

Les fichiers de configuration du profil `prod` sont vérifiés séparément, sans connexion distante : 09:00–18:00, 30 minutes, lundi au vendredi. Des variables externes peuvent remplacer ces valeurs en production ; ce test ne prouve pas les valeurs effectives du serveur déployé.

Le calendrier est global. `userId` contrôle l'existence et les droits de l'utilisateur, pas les rendez-vous exclus du calcul. Le service conserve la requête :

```java
findByStatusInAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
    AppointmentStatus.activeStatuses(), windowEnd, windowStart)
```

Les statuts actifs sont `PENDING`, `CONFIRMED`, `SCHEDULED`. `CANCELLED` et `COMPLETED` ne bloquent rien. Le chevauchement utilise strictement `start < windowEnd && end > windowStart` et les mêmes bornes pour chaque créneau : un rendez-vous finissant à 09:00 ou commençant à 18:00 ne bloque pas la journée.

Le test de diagnostic liste uniquement les statuts et horaires des rendez-vous de test bloquants. Des comptes démo et réels simulés participent au même calendrier. L'initialiseur démo actuel crée les comptes, sans créer de rendez-vous.

## Quand une réponse vide est-elle possible ?

- Le jour demandé n'est pas dans les `workingDays` effectifs.
- La fenêtre effective ne contient aucun créneau entier (heures inversées, fenêtre trop courte ou durée trop longue). Une durée nulle ou négative provoque une erreur métier, pas une liste vide.
- Tous les débuts de créneaux sont inférieurs ou égaux à l'heure du `Clock`.
- Tous les créneaux sont occupés, ou une combinaison d'occupation et de temps écoulé les élimine.

Un seul rendez-vous de 10:00 à 10:30 laisse 17 créneaux. En revanche, un intervalle historique actif couvrant toute la journée, même s'il commence la veille, bloque les 18 créneaux. Le test reproduit explicitement ce cas sans supprimer ni ignorer l'intervalle. Les données historiques incohérentes ne sont pas corrigées automatiquement.

`Clock.systemDefaultZone()` reste utilisé. Les rendez-vous et les DTO sont des `LocalDateTime`, sans fuseau embarqué. Le 5 octobre étant futur par rapport au 25 septembre, UTC et Europe/Paris donnent tous deux 18 créneaux ; ce décalage horaire seul n'explique pas une journée vide à cette distance. Le fuseau joue en revanche à proximité de l'heure courante.

## Logs ajoutés

Une ligne INFO par calcul sur un jour ouvré indique :

```text
Availability date=2026-10-05 windowStart=2026-10-05T09:00 windowEnd=2026-10-05T18:00 now=... zone=... activeAppointments=1 candidateSlots=18 busySlots=1 elapsedSlots=0 generatedSlots=17
```

`busySlots` compte les créneaux occupés. `elapsedSlots` compte les créneaux libres mais déjà commencés ; les deux compteurs ne se chevauchent pas. Le total avec `generatedSlots` vaut `candidateSlots`. L'heure est échantillonnée une fois par calcul pour garder une référence cohérente.

Un jour fermé donne `generatedSlots=0 reason=NON_WORKING_DAY`. Le niveau DEBUG du logger `com.kangoute.appointment.service.impl.AppointmentAvailabilityServiceImpl` ajoute les jours ouvrés chargés, la durée, les bornes et le fuseau, puis le statut/début/fin de chaque intervalle actif récupéré. Aucun email, identité, token ou secret n'est journalisé.

## Limite du diagnostic

Aucune lecture ou modification de Render/Neon n'a été effectuée. La cause précise du comportement en production reste à confirmer avec le statut HTTP et le corps réels de la réponse, la version déployée, puis la configuration et les intervalles bloquants effectifs. Le message frontend « Aucun créneau disponible » ne prouve pas à lui seul une réponse HTTP 200 contenant `[]`.

Les modifications apportent une couverture de non-régression et de l'observabilité, sans prétendre corriger une cause de production non démontrée. Aucun créneau artificiel n'est généré ; aucun conflit, weekend ou contrôle d'accès n'est ignoré.
