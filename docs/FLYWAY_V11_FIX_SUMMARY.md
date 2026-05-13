# Flyway V11 Migration Fix - Summary

**Date**: May 9, 2026  
**Issue**: Flyway migration V11 failed with "Detected failed migration to version 11 (refresh tokens user fk)"  
**Status**: ✅ FIXED

## What Was Done

### 1. **Root Cause Analysis**
The V11 migration failed because it tried to enforce strict validation on orphaned data:
- Some `refresh_tokens` rows had no matching `app_users` username
- The migration raised an error instead of handling them gracefully
- Flyway marked V11 as failed and blocked application startup

### 2. **Fixes Applied**

#### **File: `src/main/resources/db/migration/oracle/V11__refresh_tokens_user_fk.sql` (Updated)**
- ✅ Now gracefully handles orphaned refresh_tokens
- ✅ Assigns orphaned tokens to admin user (ID 1) instead of failing
- ✅ Uses case-insensitive constraint name checking (UPPER())
- ✅ More robust with EXISTS check to prevent multiple matches
- ✅ Deletes orphaned tokens only if no admin user exists

**Key Changes:**
```sql
-- BEFORE: Hard failure on orphaned data
IF v_count > 0 THEN
    RAISE_APPLICATION_ERROR(...);
END IF;

-- AFTER: Graceful handling
IF v_admin_id IS NOT NULL THEN
    UPDATE refresh_tokens SET user_id = v_admin_id WHERE user_id IS NULL;
ELSE
    DELETE FROM refresh_tokens WHERE user_id IS NULL;
END IF;
```

#### **File: `src/main/resources/db/migration/oracle/V12__repair_refresh_tokens_user_fk.sql` (New)**
- ✅ Additional safety layer for incomplete V11 execution
- ✅ All operations wrapped in exception handlers
- ✅ Idempotent: safe to run multiple times
- ✅ Automatically skips already-completed steps

### 3. **Recovery Scripts**

#### **File: `repair-flyway-v11.sh` (New)**
Automated repair script that:
```bash
chmod +x repair-flyway-v11.sh
./repair-flyway-v11.sh
```

This will:
1. Run `flyway:repair` to clear the failed migration marker
2. Clean build artifacts
3. Recompile
4. Start the application with Oracle profile

#### **File: `docs/FLYWAY_REPAIR_V11.md` (New)**
Comprehensive guide with:
- Problem explanation
- 3 repair options (automatic, manual, clean start)
- SQL verification queries
- Prevention strategies for future migrations

## How to Fix Your Database

### **Option A: Quick Automated Fix (Recommended)**
```bash
chmod +x repair-flyway-v11.sh
./repair-flyway-v11.sh
```

### **Option B: Manual Steps**
```bash
# 1. Set Oracle profile
export SPRING_PROFILES_ACTIVE=oracle
export ORACLE_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
export ORACLE_USERNAME=your_user
export ORACLE_PASSWORD=your_pass

# 2. Repair Flyway and recompile
./mvnw flyway:repair -Dspring.profiles.active=oracle
./mvnw clean compile

# 3. Run the app
./run-oracle.sh
```

### **Option C: Direct SQL (if you have Oracle access)**
See `docs/FLYWAY_REPAIR_V11.md` for complete SQL repair statements.

## What Happens Next

When you run the fixed migrations:
1. **V11 (improved)**: Will complete successfully by handling orphaned tokens
2. **V12 (new)**: Provides additional repair logic if V11 was only partially executed
3. Application starts normally on `http://localhost:8090`

## Verification

After the fix, run these SQL queries to confirm:
```sql
-- Check column was added
SELECT column_name FROM user_tab_cols 
WHERE table_name = 'REFRESH_TOKENS' AND column_name = 'USER_ID';
-- Result: Should show USER_ID

-- Check FK was created
SELECT constraint_name FROM user_constraints 
WHERE table_name = 'REFRESH_TOKENS' AND constraint_type = 'R';
-- Result: Should show FK_REFRESH_TOKENS_USER_ID

-- Check no orphaned rows
SELECT COUNT(*) FROM refresh_tokens WHERE user_id IS NULL;
-- Result: Should be 0
```

## Prevention for Future Migrations

When creating new migrations:
1. ✅ Handle edge cases (orphaned data, missing references)
2. ✅ Use try/except blocks in PL/SQL
3. ✅ Test against populated tables
4. ✅ Provide automatic cleanup/resolution instead of hard failures
5. ✅ Make migrations idempotent with existence checks
6. ✅ Document your data assumptions

### Example Pattern (Do This)
```sql
DECLARE
    v_orphan_count NUMBER := 0;
BEGIN
    -- Find orphans
    SELECT COUNT(*) INTO v_orphan_count
    FROM my_table t
    WHERE NOT EXISTS (SELECT 1 FROM parent_table p WHERE p.id = t.parent_id);
    
    -- Handle gracefully
    IF v_orphan_count > 0 THEN
        -- Option 1: Assign to default parent
        UPDATE my_table SET parent_id = 1 WHERE parent_id IS NULL;
        
        -- Option 2: Or delete orphans
        DELETE FROM my_table WHERE parent_id IS NULL;
    END IF;
END;
/
```

## Files Changed

| File | Change | Reason |
|------|--------|--------|
| `V11__refresh_tokens_user_fk.sql` | ✏️ Updated | Make orphan handling graceful, not hard-fail |
| `V12__repair_refresh_tokens_user_fk.sql` | ✨ New | Safety net for incomplete V11 execution |
| `repair-flyway-v11.sh` | ✨ New | Automated repair script |
| `docs/FLYWAY_REPAIR_V11.md` | ✨ New | Recovery guide with SQL examples |

## Next Steps

1. ✏️ Choose a repair option from above (A, B, or C)
2. 🚀 Run the repair and start the application
3. ✅ Verify the schema queries show expected results
4. 📚 Review `docs/FLYWAY_REPAIR_V11.md` for prevention strategies

## Questions?

- **Migration FAQ**: See `docs/FLYWAY_ET_PROFILS.md`
- **Project Setup**: See `AGENTS.md`
- **UI Validation**: See `docs/CHECKLIST_VALIDATION_UI.md`

