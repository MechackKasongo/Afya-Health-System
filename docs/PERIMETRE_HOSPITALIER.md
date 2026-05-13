# Périmètre hospitalier et affectations

Ce document décrit comment les **services hospitaliers du catalogue** sont liés aux **comptes utilisateurs** pour les règles d’accès (admissions, urgences, etc.), en environnement de production comme en développement.

## Nom canonique « Urgences »

Le contrôle d’accès au **module urgences** repose sur la présence du service catalogue dont le libellé correspond à la constante **`HospitalCatalogConstants.URGENCES_SERVICE_NAME`** (`"Urgences"`).

- Le catalogue est initialisé au démarrage (profils avec création de schéma) via `HospitalServiceManagementService.ensureDefaultCatalog()` ; pour les bases gérées uniquement par migrations Oracle/Flyway, une ligne équivalente doit exister dans `hospital_services`.
- Lors de l’**affectation d’un médecin ou d’un infirmier** (UI administration ou API utilisateurs), il faut sélectionner **exactement** ce service dans la liste — pas un libellé libre du type « Service urgences » ou « Urgence », qui ne sera pas traité comme le même service (la correspondance métier utilise une comparaison insensible à la casse sur le nom catalogue).

Référence code : `com.afya.afya_health_system.soa.common.constants.HospitalCatalogConstants`.

## Identité et affectations (production)

- Les utilisateurs applicatifs sont des lignes **`app_users`** avec rôles (`roles`) et liaison optionnelle **`user_hospital_services`** vers **`hospital_services`**.
- **`POST /api/v1/auth/login`** et **`POST /api/v1/auth/refresh`** renvoient un objet **`me`** identique à **`GET /api/v1/auth/me`** : identifiant, rôles, **`hospitalServiceIds`** et **`hospitalServiceNames`** triés. Les clients peuvent donc afficher le périmètre sans appeler `/me` immédiatement après connexion ; **`GET /auth/me`** reste la source de vérité pour rafraîchir le profil (ex. après modification des affectations par un administrateur).
- Les contrôles métier (liste urgences vide par affectation, admissions filtrées, etc.) chargent les affectations **depuis la base** à chaque requête via le nom d’utilisateur du JWT — pas depuis les claims du jeton.

### Check-list exploitation

1. Désactiver ou ajuster le compte bootstrap (`app.bootstrap.*`) lorsque les utilisateurs sont entièrement provisionnés en base ou via Flyway.
2. Vérifier que les catalogues **`hospital_services`** et les liaisons utilisateurs sont présents dans l’environnement cible (Oracle : migrations sous `db/migration/oracle`, etc.).
3. Après changement d’affectation d’un utilisateur, celui-ci peut rafraîchir sa session (reconnexion ou appel **`GET /auth/me`**) pour voir les libellés à jour dans l’interface ; les nouvelles règles de périmètre s’appliquent dès la prochaine requête API authentifiée.

## Journalisation (audit)

- Refus d’accès au module urgences (création ou lecture/actions sur un dossier alors que l’affectation ne couvre pas « Urgences ») : journaux **WARN** côté serveur avec **nom d’utilisateur**, **code d’opération** (ex. `CREATE`, `GET_DETAIL`, `TRIAGE`) et **identifiant numérique du dossier urgence** uniquement — aucun motif, nom de patient ou autre donnée clinique.
- Déconnexion (`POST /auth/logout`) : **INFO** avec utilisateur et indication si la révocation du refresh est globale ou ciblée (sans valeur de jeton dans les logs).
