#!/bin/bash
# repair-flyway-v11.sh - Repair V11 migration failure and re-run application

set -e

echo "=== Afya Health System: Flyway V11 Migration Repair ==="
echo ""

# Check if we're in the project root
if [ ! -f "pom.xml" ]; then
    echo "ERROR: pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

# Set Oracle profile
export SPRING_PROFILES_ACTIVE=oracle

echo "Step 1: Running Flyway repair to clear failed migration marker..."
echo ""

# Run Flyway repair
./mvnw flyway:repair \
    -Dspring.profiles.active=oracle \
    -q || {
    echo "Flyway repair may have encountered issues. Continuing..."
}

echo ""
echo "Step 2: Clearing any old build artifacts..."
./mvnw clean -q

echo ""
echo "Step 3: Compiling application..."
./mvnw compile -q

echo ""
echo "Step 4: Starting application with Oracle profile..."
echo "Running: ./mvnw spring-boot:run"
echo ""
echo "The application will now:"
echo "  1. Re-run Flyway migrations (including repaired V11 and V12)"
echo "  2. Start the Spring Boot application on http://localhost:8090"
echo ""

./mvnw spring-boot:run

