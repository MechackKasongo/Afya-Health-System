#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env.local" ]]; then
  echo "Fichier .env.local introuvable. Crée-le d'abord."
  exit 1
fi

# Charge les variables locales (Oracle + JWT + bootstrap)
source ".env.local"

mkdir -p ".m2/repository"
./mvnw -Dmaven.repo.local="$ROOT_DIR/.m2/repository" spring-boot:run
