# Réservation, QR et email

## Contrat de réservation

`POST /api/appointments` reste authentifié par JWT. Le formulaire doit désormais envoyer les trois champs de contact obligatoires :

```json
{
  "contactFirstName": "Jean",
  "contactLastName": "Dupont",
  "contactEmail": "jean@example.com",
  "startDateTime": "2030-01-08T10:00:00",
  "endDateTime": "2030-01-08T10:30:00",
  "reason": "Consultation"
}
```

Les noms sont nettoyés avec `trim` et contiennent entre 2 et 80 caractères. L'email est nettoyé, mis en minuscules et limité à 254 caractères. Ces champs sont un snapshot du rendez-vous : le profil utilisateur n'est pas modifié. Le client utilisateur n'a pas besoin d'envoyer `userId`. La compatibilité de l'option administrative existante est conservée avec ses contrôles d'accès.

Le statut initial reste `PENDING`. La règle du rendez-vous actif unique, les disponibilités, le verrou transactionnel, les transitions et les restrictions DEMO restent appliqués. La réponse et les listes admin ajoutent `publicReference` et les coordonnées de contact, sans token. La recherche admin inclut la référence et les champs de contact ; elle reste limitée aux données démo pour le DEMO ADMIN.

## Référence, migration et vérification

V5 ajoute les coordonnées, `public_reference` et `verification_token`, sans modifier les migrations précédentes. Les champs des rendez-vous historiques restent null ; les données historiques sont conservées. Deux contraintes uniques protègent référence et token.

La référence comprend la date et 96 bits aléatoires. Le token indépendant utilise 256 bits issus de `SecureRandom`, encodés en Base64 URL. Il est conservé en base pour réutiliser le même lien dans les emails de changement de statut. Ce choix évite de révoquer le QR initial à chaque email ; l'accès à la base doit donc protéger ce token comme une capacité d'accès. Il n'apparaît ni dans les DTO privés/admin ni dans les logs applicatifs.

Le QR PNG de 300 × 300 pixels (ZXing) contient uniquement :

`APP_FRONTEND_URL/verify-appointment?token=TOKEN`

Il ne contient aucun nom, email, motif, identifiant utilisateur ni JWT. La page frontend correspondante doit appeler :

`GET /api/public/appointments/verify?token=TOKEN`

Seul cet endpoint GET est public. Sa réponse contient `publicReference`, `contactFirstName`, `contactLastName`, `startDateTime`, `endDateTime`, `reason`, `status`. Aucun email, identifiant interne, audit ou token n'est retourné. Un token invalide ou inconnu reçoit la même erreur 404. Les réponses réussies portent `Cache-Control: no-store` et `Referrer-Policy: no-referrer`.

Toute personne possédant le lien peut lire ces informations : le lien doit être traité comme confidentiel et utilisé via HTTPS en production. Ne pas enregistrer les paramètres de ce lien dans les outils d'analyse ou journaux du frontend/proxy. Le frontend n'est pas modifié dans cette évolution ; la page `/verify-appointment` doit exister pour afficher le résultat du scan.

## Configuration email sur Render

Configurer les variables suivantes dans l'environnement ; aucun fournisseur SMTP n'est imposé :

| Variable | Utilisation |
|---|---|
| `APP_MAIL_ENABLED` | `true` pour envoyer, `false` par défaut |
| `APP_MAIL_FROM` | Adresse expéditeur autorisée par le fournisseur |
| `APP_FRONTEND_URL` | Origine HTTPS du frontend, sans slash final |
| `SPRING_MAIL_HOST` | Serveur SMTP |
| `SPRING_MAIL_PORT` | Port SMTP indiqué par le fournisseur |
| `SPRING_MAIL_USERNAME` | Identifiant SMTP, à configurer uniquement dans Render |
| `SPRING_MAIL_PASSWORD` | Secret SMTP, à configurer uniquement dans Render |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | Selon les exigences du fournisseur, généralement `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | Selon le fournisseur, généralement `true` pour STARTTLS |

Spring Boot lie directement les variables `SPRING_MAIL_*`. Les délais de connexion, lecture et écriture SMTP sont limités à 5 secondes par défaut. Utiliser les réglages TLS exigés par le fournisseur. Aucun secret SMTP ne doit être ajouté au dépôt.

## Envoi et notifications

Un événement est publié pendant la transaction ; le mail est envoyé uniquement après son commit. Une transaction annulée n'envoie rien. Une erreur SMTP est interceptée et ne remet pas en cause le rendez-vous enregistré. Le log indique seulement le type d'erreur, sans destinataire ni contenu. Avec `APP_MAIL_ENABLED=false`, la réservation fonctionne et un message générique indique l'absence d'envoi.

- Création : « Demande de rendez-vous enregistrée — [référence] », statut « En attente de confirmation ».
- Confirmation : « Rendez-vous confirmé — [référence] ».
- Annulation utilisateur ou admin : « Rendez-vous annulé — [référence] ».
- `COMPLETED` : aucun email supplémentaire.

Chaque email contient la référence, la date/heure, le motif, le statut, le QR inline CID et un lien cliquable. Les données saisies sont échappées dans le HTML. L'envoi après commit est immédiat et sans file persistante ni relance automatique : une indisponibilité SMTP nécessite une intervention opérationnelle si un renvoi est souhaité.

La notification utilisateur existante est conservée. Une notification « Nouveau rendez-vous » est adressée aux vrais admins, et au DEMO ADMIN pour les réservations du DEMO USER. Elle contient référence, contact, date/heure et motif. Le DEMO ADMIN ne peut lire que les notifications du rendez-vous démo destinées aux comptes démo ; les destinataires réels ne lui sont pas exposés.

## Tests

Les tests utilisent un `JavaMailSender` simulé et n'envoient aucun email réel. Ils couvrent le commit/rollback, l'échec SMTP, les trois emails, le décodage effectif du PNG, le DTO public, la validation, la recherche et les restrictions DEMO. Les tests Flyway H2 et PostgreSQL Testcontainers vérifient la préservation des données ; PostgreSQL vérifie aussi les contraintes uniques V5.

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Docker doit être actif pour exécuter les tests PostgreSQL existants.
