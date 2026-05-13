# Quick Fix: Flyway V11 Failure

**Problem**: Application won't start because migration V11 failed on Oracle  
**Solution**: Run one command

## 🚀 Quick Fix (2 minutes)

```bash
chmod +x repair-flyway-v11.sh
./repair-flyway-v11.sh
```

That's it! The script will:
1. Repair Flyway migration history
2. Clean build
3. Recompile
4. Start the app

## 📚 Detailed Docs

If that doesn't work or you want more control, see:

| Document | Purpose |
|----------|---------|
| `docs/FLYWAY_V11_FIX_SUMMARY.md` | 🎯 Complete overview of what was fixed |
| `docs/FLYWAY_REPAIR_V11.md` | 🔧 3 repair options + SQL examples |
| `docs/FLYWAY_MIGRATIONS_GUIDE.md` | 📖 Technical deep dive + best practices |

## What Changed

### Files Updated
- ✏️ `src/main/resources/db/migration/oracle/V11__refresh_tokens_user_fk.sql` - Now handles orphaned data gracefully

### Files Created
- ✨ `src/main/resources/db/migration/oracle/V12__repair_refresh_tokens_user_fk.sql` - Safety net migration
- ✨ `repair-flyway-v11.sh` - Automated repair script
- ✨ `docs/FLYWAY_V11_FIX_SUMMARY.md` - Summary
- ✨ `docs/FLYWAY_REPAIR_V11.md` - Detailed guide
- ✨ `docs/FLYWAY_MIGRATIONS_GUIDE.md` - Technical reference

## ✅ Verify It Worked

After running the script, check:
```sql
-- Should show USER_ID column
SELECT column_name FROM user_tab_cols 
WHERE table_name = 'REFRESH_TOKENS' AND column_name = 'USER_ID';

-- Should show FK constraint
SELECT constraint_name FROM user_constraints 
WHERE table_name = 'REFRESH_TOKENS' AND constraint_type = 'R';

-- Should be zero
SELECT COUNT(*) FROM refresh_tokens WHERE user_id IS NULL;
```

If app still won't start:
1. Check `docs/FLYWAY_REPAIR_V11.md` Option B (manual steps)
2. Or Option C (direct SQL via sqlplus)

## Manual Fixes

### If you prefer not to use the script

**Option 1: Maven command**
```bash
export SPRING_PROFILES_ACTIVE=oracle
./mvnw flyway:repair -Dspring.profiles.active=oracle
./run-oracle.sh
```

**Option 2: Direct SQL (if you have Oracle access)**
See `docs/FLYWAY_REPAIR_V11.md` for complete SQL statements and line-by-line explanation.

## Prevention

For future migrations, always:
- ✅ Check for existing objects before creating
- ✅ Handle data edge cases gracefully (don't raise errors)
- ✅ Handle if it's idempotent (can run multiple times)
- ✅ Use exception handlers for risky operations

See `docs/FLYWAY_MIGRATIONS_GUIDE.md` for migration patterns to copy.

## Questions?

- Backend issues → Check `AGENTS.md` (Project overview)
- Profile setup → Check `docs/FLYWAY_ET_PROFILS.md`
- Business logic → Check `HELP.md`

