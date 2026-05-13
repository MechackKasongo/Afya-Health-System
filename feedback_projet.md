# 📋 Feedback — Afya Health System

> Analyse complète du projet : **132 fichiers Java** (~6 300 lignes) + **35 fichiers TypeScript/TSX**
> Architecture SOA · Spring Boot 3.3.5 · React + Vite · Java 21

---

## ✅ Points Forts

### Architecture & Organisation

| Aspect | Évaluation |
|--------|------------|
| Structure des modules SOA | ⭐⭐⭐⭐⭐ — Découpage clair par domaine métier |
| Séparation des couches (Controller → Service → Repository) | ⭐⭐⭐⭐⭐ — Respectée partout |
| DTOs sous forme de Records Java | ⭐⭐⭐⭐⭐ — Moderne, immutable, concis |
| Injection par constructeur (pas de @Autowired) | ⭐⭐⭐⭐⭐ — Testable et explicite |
| Versionnement des endpoints `/api/v1/` | ⭐⭐⭐⭐☆ — Bon, pensez à documenter la politique de versionnement |

### Sécurité & Authentification

- ✅ **JWT avec deux secrets distincts** (access + refresh) — très bonne pratique
- ✅ **Rotation des refresh tokens** : l'ancien est révoqué dès qu'un nouveau est émis
- ✅ **Stratégie "un seul refresh token actif par utilisateur"** : réduit les risques de tokens zombies
- ✅ **Tokens persistés en base** avec `RefreshToken` entity + `expiresAt` + `revoked` — robuste
- ✅ **Nettoyage des tokens expirés** au login (`deleteExpired`)
- ✅ **Logout implémenté** : révoque tous les tokens actifs de l'utilisateur
- ✅ **Bootstrap user via `@PostConstruct`** configurable par variables d'environnement (plus de hardcoding)
- ✅ **Protection du dernier admin actif** : impossible de se supprimer ou de se désactiver

### Qualité du Code

- ✅ **`GlobalExceptionHandler`** unifié — contrat JSON cohérent sur toute l'API
- ✅ **Messages d'erreur en français** — cohérence culturelle pour les utilisateurs
- ✅ **Pagination sécurisée** avec valeurs par défaut et limites max (100)
- ✅ **`FetchType.EAGER`** sur les rôles — justifié ici car les rôles sont petits et toujours nécessaires
- ✅ **CORS configurable** via variable d'environnement `app.cors.allowed-origins`
- ✅ **`@Transactional`** bien placé sur les méthodes de mutation

### Tests

- ✅ **10 classes de tests d'intégration** couvrant : Auth, Patient, Admission, Urgence, Consultation, MedicalRecord, VitalSign, Prescription, GlobalExceptionHandler
- ✅ Profil `test` isolé avec H2 + Flyway désactivé

### Frontend

- ✅ **Contrôle des routes par rôle** (`RoleRoute`) — sécurité côté UI correcte
- ✅ **21 pages React** structurées, coverage fonctionnel complet
- ✅ **`AuthContext`** centralisé pour la gestion de session
- ✅ Proxy Vite vers le backend en développement

---

## ⚠️ Points à Améliorer

### 🔴 Critique — Sécurité

#### 1. Mot de passe en clair sur le disque (`UserCredentialsFileService`)
```java
// Le mot de passe généré est écrit en clair dans un fichier TSV/CSV
Files.writeString(logFile, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
```
> **Risque** : Quiconque a accès au système de fichiers peut lire tous les mots de passe générés.

**Recommandation** :
- N'afficher le mot de passe généré qu'une seule fois dans la réponse API
- Ne jamais persister les mots de passe en clair sur disque
- Si un log est nécessaire pour audit, stocker seulement : `username`, `createdAt`, `isPasswordGenerated = true`

#### 2. Génération de mot de passe mémorable prévisible
La méthode `MemorablePasswordGenerator.generate(first, last, post, year, ordinal, ...)` utilise des données **publiques** (prénom, nom, année, numéro d'ordre). Le résultat est donc potentiellement prédictible par un attaquant connaissant ces informations.

**Recommandation** : Ajouter un suffixe aléatoire cryptographiquement sûr (`SecureRandom`) toujours présent, indépendamment des autres données.

---

### 🟠 Important — Architecture

#### 3. `countActiveAdmins()` — Performance
```java
private long countActiveAdmins() {
    return appUserRepository.findAll().stream()    // ← charge TOUS les utilisateurs en mémoire
            .filter(this::isActiveAdmin)
            .count();
}
```
> **Risque** : Si la table `app_users` grossit (milliers d'utilisateurs), cette méthode devient un goulet d'étranglement.

**Recommandation** :
```java
// Ajouter dans AppUserRepository :
@Query("SELECT COUNT(u) FROM AppUser u JOIN u.roles r WHERE r.code = 'ROLE_ADMIN' AND u.active = true")
long countActiveAdmins();
```

#### 4. Validation incomplète de l'email à la mise à jour
```java
// UserManagementService.update()
if (request.password() != null && !request.password().isBlank()) {
    user.setPasswordHash(passwordEncoder.encode(request.password()));
}
```
> Il n'y a pas de vérification de la longueur minimale du mot de passe lors de la mise à jour, contrairement à la création (8 caractères).

#### 5. `resolveRoles` dans `AuthService` lance `UNAUTHORIZED` (401) pour un rôle introuvable
```java
.orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Rôle introuvable: " + name))
```
> Ce cas devrait retourner 500 (Internal Server Error) ou 400 : si un rôle bootstrap est introuvable, c'est un problème de configuration, pas une erreur d'authentification.

---

### 🟡 Améliorations — Qualité de Code

#### 6. `sex` est une `String` (sans contrainte d'enum)
```java
@Column(nullable = false, length = 10)
private String sex;   // "M", "F", "AUTRE" ? Pas de validation
```
**Recommandation** : Utiliser un `@Enumerated(EnumType.STRING)` avec un enum `Sex { M, F, AUTRE }` ou valider avec `@Pattern`.

#### 7. Modèle `Patient` sans annotations de validation JSR-380
L'entité `Patient` n'a pas de contraintes `@NotBlank`, `@Size`, etc. sur ses champs. Ces validations sont appliquées au niveau DTO, mais pas au niveau entité — ce qui est acceptable, mais peut créer des problèmes si l'entité est instanciée directement dans des tests.

#### 8. Absence de `@Column(updatable = false)` sur `dossierNumber`
Le numéro de dossier est un identifiant métier unique — il ne devrait jamais être modifié après création.

#### 9. Nom du package avec underscore
```
com.afya.afya_health_system
```
> Convention Java : les noms de packages n'utilisent pas d'underscore. Ce serait idéalement `com.afya.afyahealthsystem` ou `com.afya.health`. Difficile à corriger maintenant, mais à noter pour de futurs projets.

#### 10. `pom.xml` — Champs vides
```xml
<name/>
<description/>
<url/>
```
Ces champs devraient être remplis pour un projet professionnel.

---

### 🟡 Tests

#### 11. Absence de tests unitaires
Seuls des **tests d'intégration** (`@SpringBootTest`) existent. Il manque des **tests unitaires** (JUnit 5 + Mockito) pour :
- Les services (`PatientService`, `AuthService`, `UserManagementService`)
- Les générateurs de mots de passe (`MemorablePasswordGenerator`, `SecurePasswordGenerator`)
- `JwtService`

> Les tests d'intégration sont lents et coûteux. Les tests unitaires testent la logique métier de façon isolée et rapide.

#### 12. Aucun test frontend
Pas de tests React (Vitest / Testing Library). À envisager pour les composants critiques (AuthContext, RoleRoute, formulaires).

---

### 🔵 Suggestions — Évolutions Futures

| N° | Suggestion |
|----|------------|
| 13 | **Rate limiting** sur `/api/v1/auth/login` pour prévenir les attaques brute-force (Spring Security + Bucket4j) |
| 14 | **Audit log** : tracer les actions sensibles (création/suppression d'utilisateur, changement de mot de passe) |
| 15 | **Email de bienvenue** : envoyer les identifiants par email plutôt que par fichier disque (Spring Mail) |
| 16 | **Swagger/OpenAPI** : déjà présent (`springdoc-openapi`), s'assurer que les endpoints sont documentés avec `@Operation` et `@ApiResponse` |
| 17 | **Lombok** : envisager de l'ajouter pour réduire le boilerplate (getters/setters sur 180+ lignes dans `Patient.java`) |
| 18 | **Profil de déploiement production** : ajouter un profil `prod` distinct d'`oracle` avec des configurations de sécurité renforcées |
| 19 | **`@Transactional(readOnly = true)`** sur toutes les méthodes de lecture dans les services |

---

## 📊 Résumé

| Catégorie | Note | Commentaire |
|-----------|------|-------------|
| Architecture | ⭐⭐⭐⭐⭐ | Excellente structure SOA, patterns respectés |
| Sécurité | ⭐⭐⭐⭐☆ | Très solide mais **mots de passe en clair sur disque** = point rouge |
| Qualité du code | ⭐⭐⭐⭐☆ | Propre, lisible, quelques optimisations à faire |
| Tests | ⭐⭐⭐☆☆ | Bonne couverture intégration, manque tests unitaires |
| Frontend | ⭐⭐⭐⭐☆ | Fonctionnel, bien structuré, manque tests |
| Documentation | ⭐⭐⭐⭐⭐ | `AGENTS.md` très complet, docs Flyway bien faites |

### Priorité des actions

```
🔴 URGENT    → Supprimer l'écriture des mots de passe en clair sur disque
🟠 IMPORTANT → Optimiser countActiveAdmins() avec une requête SQL
🟠 IMPORTANT → Ajouter des tests unitaires (services + JWT)
🟡 NORMAL    → Valider le mot de passe minimum à la mise à jour
🟡 NORMAL    → Typer sex comme enum
🔵 PLUS TARD → Rate limiting, audit log, emails de bienvenue
```
