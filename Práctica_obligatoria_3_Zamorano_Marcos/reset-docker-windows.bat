@echo off
REM -----------------------------------------------------------------------------
REM El Plantio 360 - Limpieza y arranque Docker en Windows
REM Autor: Marcos Zamorano Lasso
REM Practica 3 - Sistemas Distribuidos
REM -----------------------------------------------------------------------------

echo [Plantio360] Parando contenedores y eliminando volumenes antiguos...
docker compose down -v --remove-orphans

echo [Plantio360] Reconstruyendo y arrancando servicios...
docker compose up --build -d

echo [Plantio360] Estado de servicios:
docker compose ps

echo.
echo URLs:
echo   App:      http://localhost:8080
echo   RabbitMQ: http://localhost:15672  usuario plantio / plantio
echo   MailHog:  http://localhost:8025
echo   Sonar:    http://localhost:9000
pause
