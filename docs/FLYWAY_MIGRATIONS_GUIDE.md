# Flyway Migrations - Technical Deep Dive & Best Practices

## Understanding Flyway Validation Failures

### What Happened with V11

**Symptom:**
```
Validate failed: Migrations have failed validation
Detected failed migration to version 11 (refresh tokens user fk).
```

**Root Cause Chain:**
1. V11 migration executed and reached step 3 (orphan validation)
2. Found orphaned refresh_tokens with NULL user_id after backfill
3. Raised `RAISE_APPLICATION_ERROR(-20011, ...)`
4. Exception rolled back all changes
5. Flyway marked V11 as **FAILED** in `flyway_schema_history` table
6. On next startup, Flyway validation sees failed migration and blocks execution

**Why It Matters:**
- Flyway uses checksums to track migration changes
- Failed migrations must be explicitly repaired or removed from history
- Can't re-run V11 without marking failure as resolved
- Application won't start until issue is fixed

### Anatomy of a Bad Migration

```sql
-- ❌ BAD: Rigid validation, hard failure
BEGIN
    -- ... do stuff ...
    
    SELECT COUNT(*) INTO v_count FROM refresh_tokens WHERE user_id IS NULL;
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20011, 'Cannot continue with orphaned data!');
    END IF;
END;
/
```

**Problems:**
- Any data issue causes entire migration to fail
- Difficult to recover from (requires manual intervention)
- Production deployments get stuck
- No graceful degradation

### Anatomy of a Good Migration

```sql
-- ✅ GOOD: Graceful handling, idempotent, robust
BEGIN
    -- Get fallback reference (e.g., admin user)
    BEGIN
        SELECT id INTO v_admin_id 
        FROM app_users 
        WHERE username = 'admin' 
        AND ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN v_admin_id := NULL;
    END;
    
    -- Handle data issues gracefully
    IF v_admin_id IS NOT NULL THEN
        -- Option 1: Assign to fallback
        UPDATE refresh_tokens 
        SET user_id = v_admin_id 
        WHERE user_id IS NULL;
    ELSE
        -- Option 2: Delete problematic data
        DELETE FROM refresh_tokens WHERE user_id IS NULL;
    END IF;
    
    -- Continue with rest of migration...
END;
/
```

**Advantages:**
- Handles edge cases automatically
- No hard errors
- Production-safe
- Self-documenting (intent is clear)

## Migration Patterns for Afya Health System

### Pattern 1: Add Column with Backfill (Most Common)

```sql
DECLARE
    v_count NUMBER := 0;
BEGIN
    -- 1) Check if column already exists (idempotent)
    SELECT COUNT(*) INTO v_count FROM user_tab_cols
    WHERE table_name = 'REFRESH_TOKENS' AND column_name = 'USER_ID';
    
    -- 2) Add if missing
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens ADD (user_id NUMBER(19,0))';
    END IF;
    
    -- 3) Backfill existing rows
    EXECUTE IMMEDIATE 'UPDATE refresh_tokens SET user_id = <default_value> WHERE user_id IS NULL';
    
    -- 4) Enforce NOT NULL
    EXECUTE IMMEDIATE 'ALTER TABLE refresh_tokens MODIFY (user_id NOT NULL)';
    
    -- 5) Add index (usually following constraint)
    EXECUTE IMMEDIATE 'CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id)';
END;
/
```

**Key Points:**
- ✅ Check constraints exist before creating (IDEMPOTENT)
- ✅ Backfill with sensible defaults
- ✅ Wrap statements in exception handlers if risky
- ✅ Add indexes for foreign keys

### Pattern 2: Foreign Key with Referenced Type

```sql
-- Assume parent_table has ID
DECLARE
    v_count NUMBER := 0;
    v_parent_id NUMBER(19,0);
BEGIN
    -- Find a valid parent (fallback for existing rows)
    BEGIN
        SELECT id INTO v_parent_id FROM parent_table WHERE ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN v_parent_id := NULL;
    END;
    
    -- Add FK column if needed
    SELECT COUNT(*) INTO v_count FROM user_tab_cols
    WHERE table_name = 'CHILD_TABLE' AND column_name = 'PARENT_ID';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE child_table ADD (parent_id NUMBER(19,0))';
    END IF;
    
    -- Backfill
    IF v_parent_id IS NOT NULL THEN
        EXECUTE IMMEDIATE 'UPDATE child_table SET parent_id = ' || v_parent_id || ' WHERE parent_id IS NULL';
    ELSE
        -- OR delete rows without parent reference
        EXECUTE IMMEDIATE 'DELETE FROM child_table WHERE parent_id IS NULL';
    END IF;
    
    -- Make NOT NULL
    EXECUTE IMMEDIATE 'ALTER TABLE child_table MODIFY (parent_id NOT NULL)';
    
    -- Add FK (with error handling for duplicate)
    SELECT COUNT(*) INTO v_count FROM user_constraints
    WHERE table_name = 'CHILD_TABLE' AND constraint_type = 'R';
    
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE child_table ADD CONSTRAINT fk_child_parent 
            FOREIGN KEY (parent_id) REFERENCES parent_table(id)';
    END IF;
END;
/
```

### Pattern 3: Delete Old Data Before Constraint

```sql
-- Use when adding a constraint would violate existing data
DECLARE
    v_orphan_count NUMBER := 0;
BEGIN
    -- Check for orphans
    SELECT COUNT(*) INTO v_orphan_count FROM tokens t
    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = t.user_id);
    
    -- Log orphans for audit
    IF v_orphan_count > 0 THEN
        -- Could insert to audit table, but for migrations just log intent
        NULL;  -- In real scenario: insert into audit_delete_log
    END IF;
    
    -- Delete orphans
    DELETE FROM tokens t
    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = t.user_id);
    
    -- Now safe to add FK
    EXECUTE IMMEDIATE 'ALTER TABLE tokens ADD CONSTRAINT fk_tokens_user 
        FOREIGN KEY (user_id) REFERENCES users(id)';
END;
/
```

## Flyway Repair Scenarios

### Scenario 1: Failed Migration (V11 Case)

**Status in flyway_schema_history:**
```
| version | description | installed_on | state    |
|---------|-------------|--------------|----------|
| 11      | refresh ... | 2026-05-09   | FAILED   |
```

**Recovery:**
```sql
-- Option A: Remove failed entry and rerun
DELETE FROM flyway_schema_history WHERE version = 11;

-- Option B: Fix the database manually, then mark as success
UPDATE flyway_schema_history 
SET success = 1, state = 'SUCCESS' 
WHERE version = 11;
```

**Then:**
```bash
./mvnw flyway:repair  # Mark manually fixed entry
./run-oracle.sh       # Restart app
```

### Scenario 2: Partial Execution

**Signs:**
- Column exists but not NULL
- Index doesn't exist
- FK constraint missing

**Recovery:**
Create a new migration (V12+) that:
- Checks for existing objects before creating
- Wraps all in exception handlers
- Continues despite errors
- Safe to run multiple times

### Scenario 3: Checksum Mismatch

**Message:**
```
Migration checksum of version X does not match the database version
```

**Causes:**
- Migration file modified after execution
- Different line endings (git autocrlf)
- Encoding issues

**Recovery:**
```bash
# Option 1: Update checksum in database
UPDATE flyway_schema_history 
SET checksum = <new_checksum> 
WHERE version = X;

# Option 2: Reset and rerun
./mvnw flyway:clean   # CAUTION: Deletes all!
./mvnw spring-boot:run
```

## Best Practices Checklist

### Before Writing a Migration

- [ ] Data assumptions clear (e.g., "assume no orphans")
- [ ] Fallback strategy documented (e.g., "assign to ID 1" or "delete")
- [ ] Tested against populated schema
- [ ] Handles NULL values explicitly
- [ ] No hardcoded assumptions about data state

### In Migration Code

- [ ] Idempotent (exists checks before create)
- [ ] Exception handlers for risky operations
- [ ] Query user_tab_cols/user_indexes/user_constraints for existence checks
- [ ] Use UPPER() for constraint name checks (Oracle normalizes to uppercase)
- [ ] Separate concerns: add column, backfill, constrain

### After Migration

- [ ] Run on test database first
- [ ] Verify schema with SQL queries
- [ ] Check for unintended data loss
- [ ] Document assumptions in comment block
- [ ] Plan rollback if needed

## Useful SQL for Migration Debugging

### Check current schema state
```sql
-- Columns on a table
SELECT column_name, data_type, nullable FROM user_tab_cols 
WHERE table_name = 'REFRESH_TOKENS'
ORDER BY column_id;

-- Constraints
SELECT constraint_name, constraint_type FROM user_constraints 
WHERE table_name = 'REFRESH_TOKENS';

-- Indexes
SELECT index_name, uniqueness FROM user_indexes 
WHERE table_name = 'REFRESH_TOKENS';

-- Foreign key details
SELECT constraint_name, column_name, r_constraint_name 
FROM user_cons_columns 
WHERE table_name = 'REFRESH_TOKENS' AND position = 1;
```

### Check Flyway history
```sql
SELECT version, description, installed_on, execution_time, success 
FROM flyway_schema_history 
ORDER BY version DESC;

-- Find failures
SELECT * FROM flyway_schema_history WHERE success = 0;
```

### Rollback data issues
```sql
-- Find orphaned rows
SELECT * FROM refresh_tokens rt
WHERE NOT EXISTS (SELECT 1 FROM app_users au WHERE au.id = rt.user_id);

-- Assign to default user
UPDATE refresh_tokens SET user_id = 1 WHERE user_id IS NULL;

-- Or delete
DELETE FROM refresh_tokens WHERE user_id IS NULL;
```

## Reference: Afya Schema Patterns

From existing migrations, common patterns:

**V1-V2:** Foreign key setup
- ALT TABLE ... ADD CONSTRAINT
- Uses NOT NULL before FK

**V3:** Complex adds (vital_signs)
- Multiple columns at once
- Indexes for queries

**V5:** Enum/catalog table
- INSERT reference data
- UPDATE existing rows to reference IDs

**V10:** Optional column with default
- Add nullable, then UPDATE, then enforce NOT NULL

## See Also

- `docs/FLYWAY_ET_PROFILS.md` - Profile configuration
- `docs/FLYWAY_REPAIR_V11.md` - V11 recovery
- `AGENTS.md` - Project conventions

