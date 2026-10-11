# 🎯 IMMEDIATE ACTION REQUIRED

## Issue
Spring Boot application fails to start with:
```
Schema validation: missing column [reviewed_at] in table [restaurants]
```

## Root Cause
The `restaurants` table is missing two columns defined in the Restaurant entity:
- `reviewed_at` (TIMESTAMP)
- `reviewed_by` (UUID)

These were never created in V1-V4 migrations.

---

## ✅ QUICK FIX (Choose One)

### Option A: Automated Script (Easiest)
```bash
cd /home/shroyash/IdeaProjects/LavaFlowAI
chmod +x fix_schema.sh
./fix_schema.sh
```

### Option B: Direct SQL (Fastest)
```bash
psql -U postgres -d lavaflow_db -c "
ALTER TABLE restaurants ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE restaurants ADD COLUMN reviewed_by UUID;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;"
```

### Option C: Flyway Migration (Proper)
```bash
cd /home/shroyash/IdeaProjects/LavaFlowAI
mvn clean compile
mvn flyway:migrate
```

---

## ✨ After Fix

### Rebuild application
```bash
mvn clean compile
mvn spring-boot:run
```

### Verify schema was updated
```bash
psql -U postgres -d lavaflow_db -c "\d restaurants" | grep reviewed
```

---

## 📋 Files Modified

### V5 Migration Updated ✅
- **File:** `src/main/resources/db/migration/V5__sync_database_schema_with_entities.sql`
- **Change:** Added PHASE 1 with ALTER TABLE statements
- **Impact:** Fixes schema validation errors

### New Fix Script Added ✅
- **File:** `fix_schema.sh`
- **Purpose:** Automated schema fix
- **Usage:** `./fix_schema.sh`

### Documentation Created ✅
- **File:** `SCHEMA_FIX_GUIDE.md`
- **Content:** Complete fix procedures and verification steps

---

## 📊 Schema Changes Needed

| Column | Table | Type | Status |
|--------|-------|------|--------|
| `reviewed_at` | restaurants | TIMESTAMP | ✅ Missing |
| `reviewed_by` | restaurants | UUID | ✅ Missing |
| `email_verified` | users | BOOLEAN | ⚠️ May exist |

---

## 🚀 What Happens Next

The V5 migration (with updated PHASE 1) will:
1. ✅ Add `reviewed_at` and `reviewed_by` to restaurants
2. ✅ Ensure `email_verified` exists in users
3. ✅ Create all 15 missing tables
4. ✅ Add 65 performance indexes
5. ✅ Implement foreign key relationships

Result: **Complete schema synchronization with all 19 JPA entities**

---

## 🎉 Success Indicator

When the fix works, you'll see:
```
2026-10-09T19:XX:XX.XXX+05:45 INFO  16xxx --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http)
2026-10-09T19:XX:XX.XXX+05:45 INFO  16xxx --- [main] c.l.LavaFlowApplication : Started LavaFlowApplication in X.XXX seconds (JVM running for X.XXs)
```

No schema validation errors. Application ready on `http://localhost:8080`

---

## 📞 If Still Issues

Run detailed diagnostics:
```bash
# 1. Check PostgreSQL connection
psql -U postgres -d lavaflow_db -c "SELECT version();"

# 2. List all tables
psql -U postgres -d lavaflow_db -c "\dt"

# 3. Verify restaurants table columns
psql -U postgres -d lavaflow_db -c "\d restaurants"

# 4. Check Flyway version tracking
psql -U postgres -d lavaflow_db -c "SELECT * FROM flyway_schema_history;"
```

See `SCHEMA_FIX_GUIDE.md` for complete troubleshooting.
