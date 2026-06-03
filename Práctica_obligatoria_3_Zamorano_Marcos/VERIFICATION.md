<!--
  El Plantío 360 - Verificación local.
  Autor: Marcos Zamorano Lasso
  Práctica 3 - Sistemas Distribuidos
-->

# Verificación realizada

En este entorno se han realizado comprobaciones estáticas y de compilación Java directa porque no están disponibles los binarios `mvn` ni `docker`.

## Comprobaciones correctas

- Compilación de fuentes Java con `javac`, classpath de dependencias local y Lombok como annotation processor.
- Compilación sintáctica del microservicio Flask con `python -m py_compile`.
- Revisión de estructura de Docker Compose, Dockerfiles, plantillas Thymeleaf, estáticos y documentación.

## Comandos recomendados en máquina del profesor

```bash
docker compose up --build
```

```bash
mvn clean test
```

```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN
```
