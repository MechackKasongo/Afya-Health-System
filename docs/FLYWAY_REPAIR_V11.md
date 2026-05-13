# Flyway Migration V11 Failure - Recovery Guide

## Problem
Flyway migration V11 (`refresh_tokens_user_fk`) has partially failed on Oracle, blocking application startup with:
```
Detected failed migration to version 11 (refresh tokens user fk).
Please remove any half-completed changes then run repair to fix the schema history.
```

## Root Cause
V11 was failing because it encountered orphaned refresh_tokens rows (tokens with null or non-matching usernames) that couldn't be linked to any app_users record.

## Solution

### Option 1: Automatic Repair with V12 (Recommended)
I've created two improved migrations:
- **V11 (Updated)**: Now handles orphaned rows gracefully by assigning them to the admin user or deleting them
- **V12 (New)**: Provides additional repair logic in case V11 was only partially executed

#### Steps:
1. Ensure your Oracle database is accessible
2. Set environment variables:
   ```bash
   export SPRING_PROFILES_ACTIVE=oracle
   export ORACLE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
   export ORACLE_USERNAME=your_username
   export ORACLE_PASSWORD=your_password
   ```

3. Use Flyway's repair feature to mark V11 as resolved:
   ```bash
   # In your project directory:
   ./mvnw flyway:repair -Dspring.profiles.active=oracle
   ```

4. Then run the application:
   ```bash
   ./run-oracle.sh
   ```

### Option 2: Manual Repair Query
If you have direct Oracle access via SQL*Plus or similar:

```sql
-- 1. First, mark V11 as failed in Flyway history
DELETE FROM flyway_schema_history WHERE version = 11;

-- 2. Clean up any orphaned refresh_tokens
-- Assign them to admin user (ID 1) or delete if no admin:
UPDATE refresh_tokens 
SET user_id = 1 
WHERE user_id IS NULL;

-- OR delete orphaned tokens:
DELETE FROM refresh_tokens WHERE user_id IS NULL;

-- 3. Add the user_id column and constraints if they don't exist:
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD (user_id NUMBER(19,0))';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1430 THEN RAISE; END IF;  -- Ignore if column exists
END;
/

-- 4. Backfill user_id from username
UPDATE refresh_tokens rt
SET user_id = (
    SELECT au.id FROM app_users au WHERE au.username = rt.username
)
WHERE rt.user_id IS NULL AND rt.username IS NOT NULL;

-- 5. Make NOT NULL and add constraints
ALTER TABLE refresh_tokens MODIFY (user_id NOT NULL);
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user_id 
  FOREIGN KEY (user_id) REFERENCES app_users(id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

### Option 3: Clean Database Start
If you want a fresh start:
```bash
# Drop all tables and restart (WARNING: Data loss!)
# Use only if you're in development and have no critical data

# Then run:
./run-oracle.sh
```

## What Changed

### V11 (Improved)
- ✅ Gracefully handles orphaned refresh_tokens (assigns to admin user or deletes)
- ✅ No longer raises hard errors on data issues
- ✅ Uses UPPER() for case-insensitive constraint name checking
- ✅ More robust with EXISTS check in UPDATE statement

### V12 (New Repair Migration)
- ✅ Additional safety belt for fixing V11 partial states
- ✅ Wraps operations in exception handlers to skip already-completed steps
- ✅ Handles case where admin user doesn't exist
- ✅ Idempotent: safe to run multiple times

## Verification

After repair, verify the schema:
```sql
-- Check user_id column exists
SELECT column_name FROM user_tab_cols 
WHERE table_name = 'REFRESH_TOKENS' AND column_name = 'USER_ID';

-- Check FK exists
SELECT constraint_name FROM user_constraints 
WHERE table_name = 'REFRESH_TOKENS' AND constraint_type = 'R';

-- Check index exists
SELECT index_name FROM user_indexes 
WHERE table_name = 'REFRESH_TOKENS' AND index_name = 'IDX_REFRESH_TOKENS_USER_ID';

-- Verify no orphaned rows
SELECT COUNT(*) FROM refresh_tokens WHERE user_id IS NULL;
```

## Prevention
For future migrations:
- Use try/except patterns in PL/SQL blocks
- Handle edge cases (orphaned data, missing references)
- Test against non-empty tables before production migration
- Always provide a repair/rollback vision when data integrity is at risk

