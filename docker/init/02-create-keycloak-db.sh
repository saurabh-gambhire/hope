#!/bin/bash
set -e
psql -v ON_ERROR_STOP=0 -U master_user -d master_db -c "CREATE DATABASE keycloak;" 2>/dev/null || true
