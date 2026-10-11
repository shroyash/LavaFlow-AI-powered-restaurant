#!/bin/bash

# Manual Schema Fix for LavaFlowAI
# This script applies the missing schema columns to fix Hibernate validation errors

echo "==================================="
echo "LavaFlowAI Schema Fix"
echo "==================================="
echo ""

# PostgreSQL connection details
PGHOST="localhost"
PGUSER="postgres"
PGDATABASE="lavaflow_db"
PGPASSWORD=""

export PGHOST PGUSER PGDATABASE PGPASSWORD

echo "Connecting to PostgreSQL..."

# SQL commands to fix schema
SQL_COMMANDS="
-- Add missing columns to restaurants table
ALTER TABLE restaurants 
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

ALTER TABLE restaurants 
    ADD COLUMN IF NOT EXISTS reviewed_by UUID;

-- Ensure email_verified exists in users table  
ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Verify changes
SELECT 'RESTAURANTS TABLE COLUMNS:' as status;
SELECT column_name, data_type FROM information_schema.columns 
WHERE table_name = 'restaurants' ORDER BY ordinal_position;

SELECT 'USERS TABLE COLUMNS:' as status;
SELECT column_name, data_type FROM information_schema.columns 
WHERE table_name = 'users' ORDER BY ordinal_position;
"

# Execute SQL
echo "$SQL_COMMANDS" | psql -q 2>&1

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS: Schema has been updated!"
    echo ""
    echo "Next steps:"
    echo "  1. Rebuild project: mvn clean compile"
    echo "  2. Start application: mvn spring-boot:run"
    echo ""
else
    echo "❌ ERROR: Schema update failed!"
    echo "Please run the SQL commands manually:"
    echo ""
    echo "$SQL_COMMANDS"
    exit 1
fi
