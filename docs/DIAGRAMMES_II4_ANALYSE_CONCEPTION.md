# II.4 Phase d’analyse et de conception — Diagrammes (Afya Health System)

Ce document supporte la rédaction du mémoire (**Figure ANALYSE ET CONCEPTION DU SYSTÈME.16** et suivantes). Il regroupe les diagrammes **Mermaid** à rendre via [Mermaid Live Editor](https://mermaid.live), Typora, VS Code (extension Mermaid) ou export SVG.

---

## II.4 Phase d’analyse

### II.4.1 Modèle du domaine

En génie logiciel, un **modèle de domaine** est une représentation simplifiée de certains éléments d’un domaine de connaissances ou d’activités. Ce modèle aide à résoudre des problèmes spécifiques à ce domaine. Il inclut les concepts importants du monde réel : données pertinentes pour l’activité et règles associées.

Le modèle utilise le **vocabulaire métier** pour faciliter l’échange avec les acteurs non techniques. Les besoins se déclinent souvent en **modèle conceptuel / logique des données**, puis en **modèle physique** (tables Oracle ou H2, migrations Flyway selon le profil).

#### Schéma conceptuel des données (MCD — `erDiagram`)

Vue **entités–associations** : cardinalités alignées sur le code ; le séjour référence une unité du catalogue par **`serviceName`** (chaîne égale au **nom** d’un `HospitalService`). Les utilisateurs métier ont un périmètre via la table **`user_hospital_services`**.

```mermaid
erDiagram
  PATIENT ||--o{ ADMISSION : "accueilli"
  PATIENT ||--o{ CONSULTATION : "concerne"
  PATIENT ||--o| MEDICAL_RECORD : "synthèse 0–1 par patient"
  PATIENT ||--o{ MEDICAL_RECORD_ENTRY : "entrées dossier"
  PATIENT ||--o{ URGENCE : "passages"

  HOSPITAL_SERVICE ||--o{ ADMISSION : "nom catalogue = serviceName"
  ADMISSION ||--o{ MOVEMENT : "mouvements"
  ADMISSION ||--o{ PRESCRIPTION_LINE : "lignes prescription"
  ADMISSION ||--o{ VITAL_SIGN_READING : "constantes"
  ADMISSION ||--o| ADMISSION_CLINICAL_FORM : "formulaire 0–1"
  ADMISSION ||--o{ CONSULTATION : "liée au séjour"

  PRESCRIPTION_LINE ||--o{ MEDICATION_ADMINISTRATION : "coches / créneaux"

  CONSULTATION ||--o{ CONSULTATION_EVENT : "fil chronologique"

  URGENCE ||--o{ URGENCE_TIMELINE_EVENT : "journal passage"

  APP_USER }o--o{ ROLE : "user_roles"
  APP_USER }o--o{ HOSPITAL_SERVICE : "user_hospital_services périmètre"

  PATIENT {
    bigint id PK
    string dossier_number UK
    string first_name
    string last_name
    date birth_date
    datetime deceased_at
  }

  HOSPITAL_SERVICE {
    bigint id PK
    string name UK
    int bed_capacity
    boolean active
  }

  ADMISSION {
    bigint id PK
    bigint patient_id FK
    string service_name
    string status
    datetime admission_datetime
    datetime discharge_datetime
  }

  MEDICAL_RECORD {
    bigint id PK
    bigint patient_id FK
  }

  URGENCE {
    bigint id PK
    bigint patient_id FK
    string status
    datetime created_at
  }

  APP_USER {
    bigint id PK
    string username UK
    boolean active
  }

  ROLE {
    bigint id PK
    string code UK
  }

  REFRESH_TOKEN {
    bigint id PK
    string username
    boolean revoked
  }

  REVOKED_ACCESS_JTI {
    string jti PK
    string username
    timestamp expires_at
  }
```

*Remarques : (1) génération du numéro de dossier (`patients.dossier_number`) s’appuie sur une table technique **`patient_dossier_sequences`** (compteur par année, **sans lien JPA direct** avec `Patient`) ; (2) les entités **`REFRESH_TOKEN`** et **`REVOKED_ACCESS_JTI`** sont reliées aux comptes par **`username`** (pas de clé étrangère numérique vers `APP_USER`).*

#### Schéma conceptuel — `classDiagram`

Équivalent UML (`classDiagram` Mermaid) du MCD ci-dessus : **attributs résumés** + **associations**.

```mermaid
classDiagram
  direction TB

  class Patient {
    <<entity>>
    +Long id
    +String dossierNumber
    +String firstName
    +String lastName
    +LocalDate birthDate
    +LocalDateTime deceasedAt
  }

  class HospitalService {
    <<entity>>
    +Long id
    +String name
    +Integer bedCapacity
    +boolean active
  }

  class Admission {
    <<entity>>
    +Long id
    +Long patientId
    +String serviceName
    +String status
    +LocalDateTime admissionDateTime
    +LocalDateTime dischargeDateTime
  }

  class Movement {
    <<entity>>
    +Long id
    +Long admissionId
    +String type
    +LocalDateTime createdAt
  }

  class PrescriptionLine {
    <<entity>>
    +Long id
    +Long admissionId
    +String medicationName
    +LocalDate startDate
    +boolean active
  }

  class VitalSignReading {
    <<entity>>
    +Long id
    +Long admissionId
    +LocalDateTime recordedAt
  }

  class MedicationAdministration {
    <<entity>>
    +Long id
    +Long prescriptionLineId
    +LocalDate administrationDate
    +boolean administered
  }

  class AdmissionClinicalForm {
    <<entity>>
    +Long id
    +Long admissionId
  }

  class Consultation {
    <<entity>>
    +Long id
    +Long patientId
    +Long admissionId
    +String doctorName
    +LocalDateTime consultationDateTime
  }

  class ConsultationEvent {
    <<entity>>
    +Long id
    +Long consultationId
    +Long patientId
    +String type
    +LocalDateTime createdAt
  }

  class MedicalRecord {
    <<entity>>
    +Long id
    +Long patientId
  }

  class MedicalRecordEntry {
    <<entity>>
    +Long id
    +Long patientId
    +String type
    +LocalDateTime createdAt
  }

  class Urgence {
    <<entity>>
    +Long id
    +Long patientId
    +String status
    +LocalDateTime createdAt
  }

  class UrgenceTimelineEvent {
    <<entity>>
    +Long id
    +Long urgenceId
    +String type
    +LocalDateTime createdAt
  }

  class AppUser {
    <<entity>>
    +Long id
    +String username
    +String fullName
    +boolean active
  }

  class Role {
    <<entity>>
    +Long id
    +String code
  }

  class RefreshToken {
    <<entity>>
    +Long id
    +String username
    +Instant expiresAt
    +boolean revoked
  }

  class RevokedAccessJti {
    <<entity>>
    +String jti
    +String username
    +Instant expiresAt
  }

  class PatientDossierSequence {
    <<entity technique>>
    +Integer sequenceYear
    +String letterBlock
    +Integer sequenceNumber
  }

  note for PatientDossierSequence "Compteur dossier annuel (pas de FK vers Patient)."

  Patient "1" --> "*" Admission : hospitalise
  Admission ..> HospitalService : serviceName aligne nom catalogue
  Admission "1" --> "*" Movement
  Admission "1" --> "*" PrescriptionLine
  Admission "1" --> "*" VitalSignReading
  Admission "1" --> "0..1" AdmissionClinicalForm
  PrescriptionLine "1" --> "*" MedicationAdministration

  Patient "1" --> "*" Consultation
  Admission "1" --> "*" Consultation
  Consultation "1" --> "*" ConsultationEvent

  Patient "1" --> "0..1" MedicalRecord : synthese UK patientId
  Patient "1" --> "*" MedicalRecordEntry

  Patient "1" --> "*" Urgence
  Urgence "1" --> "*" UrgenceTimelineEvent

  AppUser "*" --> "*" Role
  AppUser "*" --> "*" HospitalService : perimetre clinique

  AppUser ..> RefreshToken : reference logique par username
  AppUser ..> RevokedAccessJti : reference logique par username
```

---

**Figure — Le modèle du domaine** : (1) associations métier ; (2)–(3) **attributs** alignés sur les entités JPA du dépôt.

```mermaid
classDiagram
  direction TB

  Patient "1" --> "*" Admission : hospitalise
  Admission "1" --> "*" Movement : mouvements / audit sejour
  Admission "1" --> "*" PrescriptionLine : prescription
  Admission "1" --> "*" VitalSignReading : constantes
  Admission "1" --> "0..1" AdmissionClinicalForm : formulaire clinique
  PrescriptionLine "1" --> "*" MedicationAdministration : administrations

  Patient "1" --> "*" Consultation : consulte
  Admission "1" --> "*" Consultation : pendant sejour
  Consultation "1" --> "*" ConsultationEvent : fil chronologique

  Patient "1" --> "0..1" MedicalRecord : dossier synthese
  Patient "1" --> "*" MedicalRecordEntry : entrees archive

  Patient "1" --> "*" Urgence : passage urgences
  Urgence "1" --> "*" UrgenceTimelineEvent : ligne temps

  HospitalService .. Admission : serviceName aligne sur nom catalogue
  AppUser "*" --> "*" Role : habilitation
  AppUser "*" --> "*" HospitalService : affectations (perimetre)
```

**Attributs — patient, catalogue, séjour hospitalier et aide à la décision clinique par admission**  
(Noms et types alignés sur les entités JPA du dépôt : `Patient`, `HospitalService`, `Admission`, `Movement`, `PrescriptionLine`, `VitalSignReading`, `MedicationAdministration`, `AdmissionClinicalForm`.)

```mermaid
classDiagram
  direction TB

  class Patient {
    <<entity>>
    +Long id
    +String firstName
    +String lastName
    +String dossierNumber
    +LocalDate birthDate
    +String sex
    +String phone
    +String email
    +String address
    +String postName
    +String employer
    +String employeeId
    +String profession
    +String spouseName
    +String spouseProfession
    +LocalDateTime deceasedAt
  }

  class HospitalService {
    <<entity>>
    +Long id
    +String name
    +Integer bedCapacity
    +boolean active
  }

  class Admission {
    <<entity>>
    +Long id
    +Long patientId
    +String serviceName
    +String room
    +String bed
    +String reason
    +LocalDateTime admissionDateTime
    +LocalDateTime dischargeDateTime
    +AdmissionStatus status
  }

  class Movement {
    <<entity>>
    +Long id
    +Long admissionId
    +String type
    +String fromService
    +String toService
    +LocalDateTime createdAt
    +String note
  }

  class PrescriptionLine {
    <<entity>>
    +Long id
    +Long admissionId
    +String medicationName
    +String dosageText
    +String frequencyText
    +String instructionsText
    +String prescriberName
    +LocalDate startDate
    +LocalDate endDate
    +boolean active
    +LocalDateTime createdAt
  }

  class VitalSignReading {
    <<entity>>
    +Long id
    +Long admissionId
    +LocalDateTime recordedAt
    +VitalSignSlot slot
    +Integer systolicBp
    +Integer diastolicBp
    +Integer pulseBpm
    +BigDecimal temperatureCelsius
    +BigDecimal weightKg
    +Integer diuresisMl
    +String stoolsNote
  }

  class MedicationAdministration {
    <<entity>>
    +Long id
    +Long prescriptionLineId
    +LocalDate administrationDate
    +VitalSignSlot slot
    +boolean administered
  }

  class AdmissionClinicalForm {
    <<entity>>
    +Long id
    +Long admissionId
    +String antecedentsText
    +String anamnesisText
    +String physicalExamPulmonaryText
    +String physicalExamCardiacText
    +String physicalExamAbdominalText
    +String physicalExamNeurologicalText
    +String physicalExamMiscText
    +String paraclinicalText
    +String conclusionText
  }
```

**Attributs — consultation chronologique, dossier médical, urgences, identité et jetons**  
(Même référentiel que le code : `Consultation`, `ConsultationEvent`, `MedicalRecord`, `MedicalRecordEntry`, `Urgence`, `UrgenceTimelineEvent`, `AppUser`, `Role`, `RefreshToken`, `RevokedAccessJti`.)

```mermaid
classDiagram
  direction TB

  class Consultation {
    <<entity>>
    +Long id
    +Long patientId
    +Long admissionId
    +String doctorName
    +String reason
    +LocalDateTime consultationDateTime
  }

  class ConsultationEvent {
    <<entity>>
    +Long id
    +Long consultationId
    +Long patientId
    +String type
    +String content
    +LocalDateTime createdAt
  }

  class MedicalRecord {
    <<entity>>
    +Long id
    +Long patientId
    +String allergies
    +String antecedents
    +LocalDateTime createdAt
    +LocalDateTime updatedAt
  }

  class MedicalRecordEntry {
    <<entity>>
    +Long id
    +Long patientId
    +String type
    +String content
    +LocalDateTime createdAt
  }

  class Urgence {
    <<entity>>
    +Long id
    +Long patientId
    +String motif
    +String priority
    +String triageLevel
    +String orientation
    +UrgenceStatus status
    +LocalDateTime createdAt
    +LocalDateTime closedAt
  }

  class UrgenceTimelineEvent {
    <<entity>>
    +Long id
    +Long urgenceId
    +String type
    +String details
    +LocalDateTime createdAt
  }

  class AppUser {
    <<entity>>
    +Long id
    +String username
    +String email
    +String fullName
    +String passwordHash
    +boolean active
    +int failedLoginAttempts
    +Instant lockedUntil
    note for AppUser "Associations JPA persistées :\nroles (user_roles),\nhospitalServices (user_hospital_services)"
  }

  class Role {
    <<entity>>
    +Long id
    +String code
    +String label
  }

  class RefreshToken {
    <<entity>>
    +Long id
    +String token
    +String username
    +Instant expiresAt
    +boolean revoked
  }

  class RevokedAccessJti {
    <<entity>>
    +String jti
    +Instant expiresAt
    +String username
  }
```

> **PatientDossierSequence** (attribu technique de numérotation : `sequenceYear`, `letterBlock`, `sequenceNumber`) n’apparaît pas sur le premier diagramme mais complète la couche patient côté persistance.

*Remarque métier : le contrôle d’accès « urgences » et le filtrage des admissions s’appuient sur les **noms** des services du catalogue (`HospitalService`) affectés à l’utilisateur (`AppUser`).*

---

### II.4.2 Diagramme des classes participantes

Le diagramme des classes participantes relie les **cas d’utilisation** au **modèle de domaine**. Il s’appuie sur un **découpage en couches** et distingue trois types de classes d’analyse :

| Type | Rôle |
|------|------|
| **Classe de dialogue** (`<<boundary>>`) | Interaction utilisateur : interface web ou point d’entrée REST. |
| **Classe de contrôle** (`<<control>>`) | Coordination des traitements : services applicatifs, accès données via repositories. |
| **Classe d’entité** (`<<entity>>`) | Données métier persistantes et règles de structure associées. |

Les figures suivantes couvrent les cas **a à g** demandés, plus **h) Gérer les urgences** et **S’authentifier** (transversal).

| Cas | Intitulé (analyse) |
|-----|---------------------|
| **a** | Gérer les utilisateurs |
| **b** | Gérer les services hospitaliers |
| **c** | Générer les activités du système *(rapports / agrégats sur les mouvements et le journal des urgences)* |
| **d** | Enregistrer un patient *(cf. CRUD / recherche dans l’implémentation)* |
| **e** | Gérer les admissions |
| **f** | Prise en charge médicale |
| **g** | Enregistrer les soins *(constantes vitales, administrations)* |

#### a) Gérer les utilisateurs

```mermaid
classDiagram
  direction LR

  class UserManagementUI <<boundary>>
  class UserManagementController <<boundary>>
  class UserManagementService <<control>>
  class UserCredentialsFileService <<control>>
  class AppUserRepository <<control>>
  class RoleRepository <<control>>
  class HospitalServiceRepository <<control>>
  class AppUser <<entity>>
  class Role <<entity>>
  class HospitalService <<entity>>

  UserManagementUI --> UserManagementController : HTTPS / JSON
  UserManagementController --> UserManagementService
  UserManagementController --> UserCredentialsFileService : log comptes
  UserManagementService --> AppUserRepository
  UserManagementService --> RoleRepository
  UserManagementService --> HospitalServiceRepository
  AppUserRepository ..> AppUser
  RoleRepository ..> Role
  HospitalServiceRepository ..> HospitalService
  UserManagementService ..> AppUser : CRUD, affectations
```

#### b) Gérer les services hospitaliers

```mermaid
classDiagram
  direction LR

  class HospitalServiceUI <<boundary>>
  class HospitalServiceManagementController <<boundary>>
  class HospitalServiceManagementService <<control>>
  class HospitalServiceRepository <<control>>
  class AdmissionRepository <<control>>
  class HospitalService <<entity>>

  HospitalServiceUI --> HospitalServiceManagementController
  HospitalServiceManagementController --> HospitalServiceManagementService
  HospitalServiceManagementService --> HospitalServiceRepository
  HospitalServiceManagementService --> AdmissionRepository : regle suppression
  HospitalServiceRepository ..> HospitalService
```

#### c) Générer les activités du système (rapports, statistiques)

```mermaid
classDiagram
  direction LR

  class ReportingUI <<boundary>>
  class ReportingController <<boundary>>
  class ReportingService <<control>>
  class MovementRepository <<control>>
  class UrgenceTimelineEventRepository <<control>>
  class Movement <<entity>>
  class UrgenceTimelineEvent <<entity>>

  ReportingUI --> ReportingController
  ReportingController --> ReportingService
  ReportingService --> MovementRepository
  ReportingService --> UrgenceTimelineEventRepository
  MovementRepository ..> Movement
  UrgenceTimelineEventRepository ..> UrgenceTimelineEvent
```

#### d) Enregistrer un patient

```mermaid
classDiagram
  direction LR

  class PatientUI <<boundary>>
  class PatientController <<boundary>>
  class PatientService <<control>>
  class PatientRepository <<control>>
  class Patient <<entity>>

  PatientUI --> PatientController
  PatientController --> PatientService
  PatientService --> PatientRepository
  PatientRepository ..> Patient
```

#### e) Gérer les admissions

```mermaid
classDiagram
  direction LR

  class AdmissionUI <<boundary>>
  class AdmissionController <<boundary>>
  class AdmissionService <<control>>
  class UserHospitalScopeService <<control>>
  class AdmissionRepository <<control>>
  class MovementRepository <<control>>
  class PatientRepository <<control>>
  class Admission <<entity>>
  class Movement <<entity>>
  class Patient <<entity>>

  AdmissionUI --> AdmissionController
  AdmissionController --> AdmissionService
  AdmissionService --> UserHospitalScopeService : perimetre service
  AdmissionService --> AdmissionRepository
  AdmissionService --> MovementRepository
  AdmissionService --> PatientRepository
  AdmissionRepository ..> Admission
  MovementRepository ..> Movement
  PatientRepository ..> Patient
```

#### f) Prise en charge médicale

```mermaid
classDiagram
  direction TB

  class ConsultationUI <<boundary>>
  class ConsultationController <<boundary>>
  class ConsultationService <<control>>
  class ConsultationRepository <<control>>
  class ConsultationEventRepository <<control>>

  class PrescriptionUI <<boundary>>
  class PrescriptionController <<boundary>>
  class PrescriptionService <<control>>
  class AdmissionRepository <<control>>

  class ClinicalFormUI <<boundary>>
  class AdmissionClinicalFormController <<boundary>>
  class AdmissionClinicalFormService <<control>>

  class MedicalRecordUI <<boundary>>
  class MedicalRecordController <<boundary>>
  class MedicalRecordService <<control>>

  class Consultation <<entity>>
  class ConsultationEvent <<entity>>
  class PrescriptionLine <<entity>>
  class AdmissionClinicalForm <<entity>>
  class MedicalRecord <<entity>>
  class MedicalRecordEntry <<entity>>

  ConsultationUI --> ConsultationController
  ConsultationController --> ConsultationService
  ConsultationService --> ConsultationRepository
  ConsultationService --> ConsultationEventRepository
  ConsultationRepository ..> Consultation
  ConsultationEventRepository ..> ConsultationEvent

  PrescriptionUI --> PrescriptionController
  PrescriptionController --> PrescriptionService
  PrescriptionService --> AdmissionRepository
  PrescriptionService ..> PrescriptionLine

  ClinicalFormUI --> AdmissionClinicalFormController
  AdmissionClinicalFormController --> AdmissionClinicalFormService
  AdmissionClinicalFormService ..> AdmissionClinicalForm

  MedicalRecordUI --> MedicalRecordController
  MedicalRecordController --> MedicalRecordService
  MedicalRecordService ..> MedicalRecord
  MedicalRecordService ..> MedicalRecordEntry
```

#### g) Enregistrer les soins

*(Constantes vitales et administrations médicamenteuses liées au séjour.)*

```mermaid
classDiagram
  direction LR

  class SoinsUI <<boundary>>
  class VitalSignController <<boundary>>
  class VitalSignService <<control>>
  class VitalSignReadingRepository <<control>>

  class MedAdminUI <<boundary>>
  class MedicationAdministrationController <<boundary>>

  class PrescriptionService <<control>>
  class MedicationAdministrationRepository <<control>>

  class VitalSignReading <<entity>>
  class MedicationAdministration <<entity>>

  SoinsUI --> VitalSignController
  VitalSignController --> VitalSignService
  VitalSignService --> VitalSignReadingRepository
  VitalSignReadingRepository ..> VitalSignReading

  MedAdminUI --> MedicationAdministrationController
  MedicationAdministrationController --> PrescriptionService
  PrescriptionService --> MedicationAdministrationRepository
  MedicationAdministrationRepository ..> MedicationAdministration
```

#### h) Gérer les urgences

```mermaid
classDiagram
  direction LR

  class UrgenceUI <<boundary>>
  class UrgenceController <<boundary>>
  class UrgenceService <<control>>
  class UserHospitalScopeService <<control>>
  class UrgenceRepository <<control>>
  class UrgenceTimelineEventRepository <<control>>
  class PatientRepository <<control>>
  class Urgence <<entity>>
  class UrgenceTimelineEvent <<entity>>

  UrgenceUI --> UrgenceController
  UrgenceController --> UrgenceService
  UrgenceService --> UserHospitalScopeService : acces module Urgences
  UrgenceService --> UrgenceRepository
  UrgenceService --> UrgenceTimelineEventRepository
  UrgenceService --> PatientRepository : existence patient
  UrgenceRepository ..> Urgence
  UrgenceTimelineEventRepository ..> UrgenceTimelineEvent
```

#### Transversal — S’authentifier

```mermaid
classDiagram
  direction LR

  class LoginUI <<boundary>>
  class AuthController <<boundary>>
  class AuthService <<control>>
  class JwtService <<control>>
  class AppUserRepository <<control>>
  class RefreshTokenRepository <<control>>
  class RevokedAccessJtiRepository <<control>>
  class AppUser <<entity>>
  class RefreshToken <<entity>>
  class RevokedAccessJti <<entity>>

  LoginUI --> AuthController
  AuthController --> AuthService
  AuthService --> JwtService
  AuthService --> AppUserRepository
  AuthService --> RefreshTokenRepository
  AuthService --> RevokedAccessJtiRepository : logout jti
  AppUserRepository ..> AppUser
  RefreshTokenRepository ..> RefreshToken
  RevokedAccessJtiRepository ..> RevokedAccessJti
```

---

### II.4.3 Diagramme d’activités

Vue générique **couloirs Utilisateur / Système** (navigation, validation, persistance).

```mermaid
flowchart TB
    subgraph U["Utilisateur"]
        A([Début])
        B[Ouvrir l'application]
        C[Saisir identifiant et mot de passe]
        D{Continuer après erreur ?}
        E[Choisir un module]
        F[Saisir ou modifier les données]
        G{Continuer une autre action ?}
        H[Se déconnecter]
        Z([Fin])
    end

    subgraph S["Système AFYA"]
        S1[Afficher formulaire de connexion]
        S2{Vérifier identifiants}
        S3[Afficher erreur]
        S4[Émettre jetons JWT]
        S5[Afficher tableau de bord]
        S6[Afficher écran du module]
        S7[Valider les données]
        S8{Données valides ?}
        S9[Afficher erreurs de validation]
        S10[Enregistrer et journaliser]
        S11[Afficher confirmation]
        S12[Clôturer la session]
    end

    A --> B --> S1 --> C --> S2
    S2 -->|Non| S3 --> D
    D -->|Oui| C
    D -->|Non| Z
    S2 -->|Oui| S4 --> S5 --> E --> S6 --> F --> S7 --> S8
    S8 -->|Non| S9 --> F
    S8 -->|Oui| S10 --> S11 --> G
    G -->|Oui| E
    G -->|Non| H --> S12 --> Z
```

---

### II.4.4 Diagramme de conception

Le diagramme de conception présente l’**organisation technique** : **séparation par couches** (contrôleur, service, dépôt, modèle) et **répartition modulaire** (modules SOA).

```mermaid
flowchart TB
    subgraph Presentation["Couche présentation"]
        FE[Frontend React / TypeScript]
    end

    subgraph API["Couche API REST"]
        CTRL[Controllers @RestController\n/api/v1/...]
    end

    subgraph Metier["Couche métier"]
        SVC[Services métier]
    end

    subgraph Persistance["Couche persistance"]
        REPO[Repositories JPA]
        ENT[Entités JPA]
    end

    subgraph Transversal["Transversal"]
        SEC[Sécurité JWT]
        EXC[GlobalExceptionHandler]
    end

    subgraph Modules["Modules SOA"]
        M1[identity]
        M2[patient]
        M3[admission]
        M4[consultation]
        M5[medicalrecord]
        M6[urgence]
        M7[reporting]
        M8[hospitalservice]
        M9[common]
    end

    subgraph Data["Données"]
        DB[(Oracle / H2)]
        FW[Flyway]
    end

    FE --> CTRL
    CTRL --> SVC
    SVC --> REPO
    REPO --> ENT
    ENT --> DB
    FW --> DB
    SEC --> CTRL
    EXC --> CTRL
    CTRL --> M1
    CTRL --> M2
    CTRL --> M3
    CTRL --> M4
    CTRL --> M5
    CTRL --> M6
    CTRL --> M7
    CTRL --> M8
    M9 --> SEC
    M9 --> EXC
```

---

## Légende (mémoire)

| Stéréotype UML | Équivalent analyse |
|----------------|-------------------|
| `<<boundary>>` | Classe de **dialogue** (UI ou façade REST) |
| `<<control>>` | Classe de **contrôle** (service, repository applicatif) |
| `<<entity>>` | Classe d’**entité** (données métier persistées) |

---

*Projet Afya Health System — figures à numéroter selon votre convention (ex. II.16 modèle domaine, II.17 classes participantes, II.18 activités, II.19 conception).*
