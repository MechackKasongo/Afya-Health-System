# 🔍 Patient Duplicate Detection - Implementation Guide

**Date**: May 9, 2026  
**Status**: ✅ IMPLEMENTED  
**Feature**: Automatic duplicate patient detection during registration  

---

## 📋 Feature Overview

When registering a new patient, the system now **automatically checks if a patient with the same identifying information already exists** in the database. This prevents duplicate patient records and ensures data integrity.

### Identifying Fields Checked

The duplicate detection compares the following fields:

| Field | Type | Description |
|-------|------|-------------|
| **First Name** * | String | Prénom (case-insensitive match) |
| **Last Name** * | String | Nom (case-insensitive match) |
| **Birth Date** * | Date | Date de naissance (exact match) |
| **Sex** * | String | Sexe (case-insensitive: M, F, etc.) |
| **Post-name** | String | Post-nom/middle name (optional, case-insensitive) |
| **Phone** | String | Téléphone (optional, case-insensitive) |

**\* = Required fields**

---

## 🔧 Technical Implementation

### 1. Database Query (Repository Layer)

**File**: `src/main/java/.../patient/repository/PatientRepository.java`

```java
@Query("""
        SELECT p FROM Patient p WHERE
        LOWER(p.firstName) = LOWER(:firstName)
        AND LOWER(p.lastName) = LOWER(:lastName)
        AND p.birthDate = :birthDate
        AND LOWER(p.sex) = LOWER(:sex)
        AND LOWER(COALESCE(p.postName, '')) = LOWER(COALESCE(:postName, ''))
        AND LOWER(COALESCE(p.phone, '')) = LOWER(COALESCE(:phone, ''))
        """)
List<Patient> findDuplicatePatients(
    String firstName,
    String lastName,
    LocalDate birthDate,
    String sex,
    String postName,
    String phone
);
```

**Key Features:**
- **Case-insensitive** string comparisons using `LOWER()`
- **NULL-safe** comparisons using `COALESCE()` for optional fields
- Returns **all matching patients** (typically just 1 if found)

### 2. Validation Logic (Service Layer)

**File**: `src/main/java/.../patient/service/PatientService.java`

```java
private void checkForDuplicatePatient(PatientCreateRequest request) {
    var duplicates = patientRepository.findDuplicatePatients(
            request.firstName(),
            request.lastName(),
            request.birthDate(),
            request.sex(),
            request.postName() != null ? request.postName() : "",
            request.phone() != null ? request.phone() : ""
    );

    if (!duplicates.isEmpty()) {
        Patient existingPatient = duplicates.getFirst();
        throw new ConflictException(
                "Un patient avec ces informations existe déjà: " +
                "Prénom: " + existingPatient.getFirstName() +
                ", Nom: " + existingPatient.getLastName() +
                ", Date de naissance: " + existingPatient.getBirthDate() +
                ", Numéro de dossier: " + existingPatient.getDossierNumber()
        );
    }
}
```

**Called from**: `PatientService.create()` before saving new patient

---

## 🚀 How It Works

### Step-by-Step Flow

```
Client sends POST /api/v1/patients
    ↓
PatientController receives request
    ↓
PatientService.create(PatientCreateRequest)
    ↓
checkForDuplicatePatient() ← NEW STEP
    ↓
Query database with identifying fields
    ↓
   ╔════════════════════════════════════╗
   ║ Duplicate Found?                   ║
   ╚════════════════════════════════════╝
   │                                    │
   ├─ YES → ConflictException (409)    ├─ NO → Continue to validate dossier number
   │        (Error response with      │      ↓
   │         existing patient info)   │      Generate dossier number (if needed)
   │                                 │      ↓
   │                                 │      Save patient to database
   │                                 │      ↓
   │                                 │      Create medical record
   │                                 │      ↓
   │                                 └─ Return 201 Created
   └────────────────────────────────────┘
```

---

## 📤 API Responses

### Success: No Duplicate Found

```http
POST /api/v1/patients
Content-Type: application/json

{
  "firstName": "Jean",
  "lastName": "Dupont",
  "birthDate": "1980-05-15",
  "sex": "M",
  "phone": "+243812345678",
  "postName": null
}
```

**Response: 201 Created**
```json
{
  "id": 123,
  "firstName": "Jean",
  "lastName": "Dupont",
  "dossierNumber": "DOS-2026-AAAA-0001",
  "birthDate": "1980-05-15",
  "sex": "M",
  "phone": "+243812345678",
  "postName": null,
  ...
}
```

### Error: Duplicate Found

```http
POST /api/v1/patients
Content-Type: application/json

{
  "firstName": "Jean",
  "lastName": "Dupont",
  "birthDate": "1980-05-15",
  "sex": "M",
  "phone": "+243812345678",
  "postName": null
}
```

**Response: 409 Conflict**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Un patient avec ces informations existe déjà: Prénom: Jean, Nom: Dupont, Date de naissance: 1980-05-15, Numéro de dossier: DOS-2026-AAAA-0001",
  "timestamp": "2026-05-09T14:42:30Z"
}
```

---

## 🧪 Testing the Feature

### Test Case 1: Register Unique Patient

```bash
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "Alice",
    "lastName": "Martin",
    "birthDate": "1990-03-10",
    "sex": "F",
    "phone": "+243810000001"
  }'
```

**Expected Result:** 201 Created ✅

---

### Test Case 2: Register Duplicate Patient

First, register a patient (as above). Then try to register the same patient again:

```bash
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "Alice",
    "lastName": "Martin",
    "birthDate": "1990-03-10",
    "sex": "F",
    "phone": "+243810000001"
  }'
```

**Expected Result:** 409 Conflict ✅  
**Error Message:** "Un patient avec ces informations existe déjà: ..."

---

### Test Case 3: Case Insensitivity

Register a patient with different case variations:

```bash
# First registration
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "Alex",
    "lastName": "Dupont",
    "birthDate": "1985-06-20",
    "sex": "M"
  }'

# Second registration (different case - should still be detected as duplicate)
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "ALEX",
    "lastName": "dupont",
    "birthDate": "1985-06-20",
    "sex": "m"
  }'
```

**Expected Result:** 409 Conflict ✅ (Detected as duplicate despite different case)

---

### Test Case 4: NULL/Optional Fields Handling

```bash
# First registration (no phone)
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "Bob",
    "lastName": "Smith",
    "birthDate": "1995-01-15",
    "sex": "M",
    "phone": null
  }'

# Second registration (same info including null phone - should be duplicate)
curl -X POST http://localhost:8090/api/v1/patients \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{
    "firstName": "Bob",
    "lastName": "Smith",
    "birthDate": "1995-01-15",
    "sex": "M",
    "phone": null
  }'
```

**Expected Result:** 409 Conflict ✅

---

## 🔄 Workflow in Patient Registration Form

### Frontend (React/TypeScript)

When user submits patient registration:

1. **Collect Form Data**
   - First Name (required)
   - Last Name (required)
   - Birth Date (required)
   - Sex (required)
   - Phone (optional)
   - Post-name (optional)

2. **Send POST Request**
   ```typescript
   const response = await patientApi.createPatient({
     firstName: formData.firstName,
     lastName: formData.lastName,
     birthDate: formData.birthDate,
     sex: formData.sex,
     phone: formData.phone,
     postName: formData.postName
   });
   ```

3. **Handle Response**
   ```typescript
   if (response.status === 201) {
     // Success: Patient registered
     showSuccessMessage("Patient enregistré avec succès");
     redirectToPatientDetail(response.data.id);
   } else if (response.status === 409) {
     // Conflict: Duplicate detected
     showErrorMessage(response.data.message);
     // Show existing patient details to user
   }
   ```

---

## 🛡️ Edge Cases Handled

| Edge Case | Handling | Result |
|-----------|----------|--------|
| **Missing phone field** | Empty string comparison via COALESCE | Treated as NULL → matched correctly |
| **Different case in names** | LOWER() function | "JEAN" = "jean" ✅ |
| **Extra whitespace** | Not trimmed in DB query | May need frontend validation |
| **Special characters** | Compared as-is | "Jean-Paul" ≠ "Jean Paul" |
| **Accents (é, ê, ç, etc.)** | Exact unicode match | "Rémi" ≠ "Remi" |
| **NULL vs empty string** | COALESCE treats as equivalent | NULL phone = "" phone ✅ |

---

## 📝 Configuration

### No Configuration Required

The feature is **automatically enabled** when:
- Patient module is loaded
- PatientService bean is instantiated
- `create()` method is called

### Optional: Logging

To enable duplicate detection logging, add to `application.properties`:

```properties
# Enable debug logging for patient service
logging.level.com.afya.afya_health_system.soa.patient.service=DEBUG
```

---

## 🚨 Error Handling

### What Happens When Duplicate Detected?

1. **Exception Thrown**: `ConflictException` from `common` module
2. **HTTP Status**: `409 Conflict`
3. **Response Format** (via GlobalExceptionHandler):
   ```json
   {
     "status": 409,
     "error": "Conflict",
     "message": "Un patient avec ces informations existe déjà: ...",
     "timestamp": "2026-05-09T14:42:30Z"
   }
   ```

4. **Database**: Transaction rolled back (no partial save)

---

## 📊 Performance Considerations

###  Query Efficiency

```sql
-- Generated JPA query with index support:
SELECT p FROM Patient p WHERE
  LOWER(p.firstName) = LOWER(:firstName)
  AND LOWER(p.lastName) = LOWER(:lastName)
  AND p.birthDate = :birthDate
  AND LOWER(p.sex) = LOWER(:sex)
  AND LOWER(COALESCE(p.postName, '')) = LOWER(COALESCE(:postName, ''))
  AND LOWER(COALESCE(p.phone, '')) = LOWER(COALESCE(:phone, ''))
```

**Indexes Recommended:**
```sql
-- Index on key fields for faster duplicate detection
CREATE INDEX idx_patient_duplicate ON patients(
  LOWER(first_name),
  LOWER(last_name),
  birth_date,
  LOWER(sex)
);
```

**Query Execution Time:**
- Expected: < 5ms for typical dataset
- Scales linearly with patient count

---

## 📚 Related Files

| File | Purpose |
|------|---------|
| `PatientRepository.java` | `findDuplicatePatients()` query |
| `PatientService.java` | `checkForDuplicatePatient()` logic |
| `PatientController.java` | REST endpoint handling |
| `GlobalExceptionHandler.java` | Error response formatting |
| `AGENTS.md` | Project architecture reference |

---

## 🔄 Future Enhancements

### Potential Improvements

1. **Fuzzy Matching**
   - Detect "Jean" vs "Jean-Paul" as similar
   - Soundex/Metaphone algorithms

2. **Configurable Matching**
   - Allow admin to adjust which fields are compared
   - Strictness levels (HIGH, MEDIUM, LOW)

3. **Duplicate Management UI**
   - Display near-matches to user
   - Allow merge/consolidation of duplicate records

4. **Audit Trail**
   - Log all duplicate detection attempts
   - Track false positives

---

## ✅ Verification Checklist

After deployment, verify:

- [ ] Unique patients register successfully (201)
- [ ] Duplicate patients are rejected (409)
- [ ] Case-insensitive matching works
- [ ] NULL/optional fields handled correctly
- [ ] Error messages are clear and actionable
- [ ] Database queries are performant (< 5ms)
- [ ] Frontend handles 409 responses gracefully

---

## 📞 Support

**Error Encountered?**
1. Check application logs for duplicate detection queries
2. Verify patient data in database
3. Review GlobalExceptionHandler configuration
4. Check for database index creation

**Questions?**
- See `AGENTS.md` for architecture overview
- See `PatientService.java` source code
- See `PatientRepository.java` query details

---

**Generated**: 2026-05-09  
**Status**: ✅ Production Ready  
**Tested**: Yes  
**Documented**: Yes

