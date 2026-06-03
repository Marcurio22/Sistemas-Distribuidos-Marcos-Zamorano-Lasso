#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# El Plantío 360 - Script de verificación local.
# Autor: Marcos Zamorano Lasso
# Práctica 3 - Sistemas Distribuidos
# -----------------------------------------------------------------------------
set -euo pipefail

echo "[1/3] Verificando Java con Maven"
mvn -q -DskipTests compile

echo "[2/3] Ejecutando tests"
mvn -q test

echo "[3/3] Validando microservicio Flask"
python -m py_compile python-api/app.py

echo "OK - Proyecto verificado"
