# 🔧 SCHEMA FIX GUIDE - Missing Columns

## Problem
```
Caused by: org.hibernate.tool.schema.spi.SchemaManagementException: 
Schema validation: missing column [reviewed_at] in table [restaurants]
```

**Issue:** The `restaurants` table is missing `reviewed_at` and `reviewed_by` columns that are defined in the Restaurant entity.

---

## ✅ Solution

### Method 1: Using Bash Script (Recommended - Fastest)

```bash
cd /home/shroyash/IdeaProjects/LavaFlowAI
chmod +x fix_schema.sh
./fix_schema.sh
```

This script will automatically:
- Add `reviewed_at` (TIMESTAMP) to restaurants
- Add `reviewed_by` (UUID) to restaurants  
- Verify `email_verified` exists in users
- Display the updated schema

### Method 2: Manual psql Command

```bash
psql -U postgres -d lavaflow_db << 'ENDSQL'
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE restaurants ADD COLUMN IF NOT EXISTS reviewed_by UUID;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ENDSQL
```

### Method 3: Using Flyway Directly

```bash
cd /home/shroyash/IdeaProjects/LavaFlowAI
mvn flyway:migrate
```

The updated `V5__sync_database_schema_with_entities.sql` includes PHASE 1 with these ALTER TABLE statements.

---

## 🔍 Verify the Fix

After applying the fix, verify the schema:

```bash
# Check restaurants table
psql -U postgres -d lavaflow_db -c "
  SELECT column_name, data_type 
  FROM information_schema.columns 
  WHERE table_name = 'restaurants' 
  ORDER BY ordinal_position;"

# Check users table
psql -U postgres -d lavaflow_db -c "
  SELECT column_name, data_type 
  FROM information_schema.columns 
  WHERE table_name = 'users' 
  ORDER BY ordinal_position;"
```

Expected output for restaurants:
```
 column_name | data_type | ...
------+----------+-----
 id          | uuid       
 name        | character varying
 description | text
 phone       | character varying
 email       | character varying
 logo_url    | character varying
 status      | character varying
 rejection_reason | character varying
 reviewed_at | timestamp         ← NEW
 reviewed_by | uuid              ← NEW
 created_at  | timestamp with time zone
 updated_at  | timestamp with time zone
```

---

## 🚀 After Applying Fix

1. **Rebuild the project:**
   ```bash
   cd /home/shroyash/IdeaProjects/LavaFlowAI
   mvn clean compile
   ```

2. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Check logs for successful startup:**
   - Look for: "Started LavaFlowApplication in X seconds"
   - No schema validation errors should appear

---

## 📝 What Changed in V5

The migration file was updated to include **PHASE 1: ALTER EXISTING TABLES**:

```sql
-- ============================================================================
-- PHASE 1: ALTER EXISTING TABLES TO ADD MISSING COLUMNS
-- ============================================================================

ALTER TABLE IF EXISTS restaurants 
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

ALTER TABLE IF EXISTS restaurants 
    ADD COLUMN IF NOT EXISTS reviewed_by UUID;

ALTER TABLE IF EXISTS users 
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
```

This ensures that:
1. If the restaurants table exists (it does), missing columns are added
2. If `reviewed_at` / `reviewed_by` already exist, they're not duplicated
3. The `email_verified` column is verified/created if missing

---

## 🎯 Root Cause Analysis

### Why This Happened

1. **V1 migration** was empty (should have created base tables)
2. **V2 migration** assumes users table exists but creates email verification
3. **V3 migration** creates restaurant_documents
4. **V4 migration** only added ONE column: `rejection_reason`
5. **Missing:** The Restaurant entity has `reviewed_at` and `reviewed_by` fields that were never created in any prior migration

### Why V5 Fixes It

**V5__sync_database_schema_with_entities.sql** now:
- Phase 1: Adds ALL missing columns to existing tables
- Phase 2-14: Creates all missing tables and indexes
- Uses `IF NOT EXISTS` for complete idempotency
- Handles both scenarios (tables exist or don't)

---

## 🚨 If You Still Get Errors

### Error: "Unable to start embedded Tomcat"
→ Schema not yet fixed. Run one of the fix methods above.

### Error:  "Schema validation: missing column [xxx]"
→ More columns are missing. Check the V5 migration output for details.

### Error: "Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'"
→ Same root cause. Fix database schema, then restart.

### Error: "Column already exists"
→ This is normal and not an error. The `ADD COLUMN IF NOT EXISTS` handles this gracefully.

---

## ✅ Checklist

- [ ] Applied one of the fix methods (bash script, psql, or mvn flyway)
- [ ] Verified schema changes with the verification commands
- [ ] Ran `mvn clean compile` to rebuild
- [ ] Started application with `mvn spring-boot:run`
- [ ] Confirmed app started without schema validation errors
- [ ] Checked logs contain "Started LavaFlowApplication"

---

## 📞 Support

If the fix doesn't work:

1. **Check PostgreSQL is running:**
   ```bash
   psql -U postgres -l | grep lavaflow_db
   ```

2. **Verify tables exist:**
   ```bash
   psql -U postgres -d lavaflow_db -c "\dt restaurants users"
   ```

3. **Check full V5 migration logs:**
   ```bash
   mvn flyway:validate -X 2>&1 | grep -i "v5\|migration"
   ```

4. **Review the actual restaurant columns:**
   ```bash
   psql -U postgres -d lavaflow_db -c "\d restaurants"
   ```
