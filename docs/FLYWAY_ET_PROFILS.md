# Profils Spring et Flyway (Oracle)

## Profils disponibles

| Profil | Fichier | Base de données | Flyway |
|--------|---------|-----------------|--------|
| **dev** (défaut) | `application-dev.properties` | H2 mémoire, mode PostgreSQL | **Désactivé** |
| **oracle** | `application-oracle.properties` | Oracle (`ORACLE_URL`, identifiants) | Activé (`classpath:db/migration/oracle`) |
| **test** | `application-test.properties` (tests uniquement) | H2 mémoire | **Désactivé** |

Sans variable d’environnement : `SPRING_PROFILES_ACTIVE` vaut **`dev`** (voir `application.properties`).  
Pour travailler contre **Oracle** en local ou en déploiement cible :

```bash
export SPRING_PROFILES_ACTIVE=oracle
export ORACLE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
export ORACLE_USERNAME=afya
export ORACLE_PASSWORD=***
# JWT + bootstrap : voir `.env.example`
```

## Première mise en place Flyway sur Oracle

1. Base **sans tables** ou schéma géré uniquement par vous : soit laisser **Hibernate** créer (`spring.jpa.hibernate.ddl-auto=update`), soit appliquer un script DDL — suivant votre choix équipe.

2. **Piège observé** : avec `spring.flyway.baseline-on-migrate=true`, un schéma déjà peuplé peut être **baseliné** sans exécuter la migration qui ajoute les **clés étrangères**. Le projet utilise donc **`spring.flyway.baseline-on-migrate=false`** en profil `oracle` avec des scripts FK **idempotents** (`V1` / `V2` dans `src/main/resources/db/migration/oracle/`).

3. Premier boot **véritablement vide** (aucune table) / conflits d’ordre Hibernate vs Flyway : vous pouvez **désactiver Flyway** le temps du premier démarrage, puis réactiver :

```bash
export SPRING_FLYWAY_ENABLED=false
./mvnw spring-boot:run
# Une fois les tables créées par Hibernate ou autre :
export SPRING_FLYWAY_ENABLED=true
```

4. Droits Oracle : l’utilisateur schéma doit avoir un **quota** sur le tablespace (ex. `USERS`), sinon erreur **ORA-01950** ; à corriger par un DBA (`ALTER USER ... QUOTA ... ON USERS`).

## Tests intégration

Maven exécute les tests avec le profil **`test`** uniquement (`@ActiveProfiles("test")`) : pas d’Oracle, pas de Flyway, pour des runs **isolés et déterministes**.

## Migrations Oracle (résumé)

- **V1 / V2** : clés étrangères (voir fichiers dans `db/migration/oracle/`).
- **V3** : colonnes démographiques étendues sur `patients` + table `vital_sign_readings` + FK vers `admissions`.
- **V4** : `prescription_lines`, `medication_administrations`, `admission_clinical_forms` + FK.
