import time
from datetime import datetime, timezone
from flask import Blueprint, jsonify, request

from app.errors.custom_exceptions import AppException
from app.services.file_service import read_file
from app.services.db_service import list_users, force_database_error
from app.services.pokemon_service import get_pokemon

# Blueprint principal del laboratorio de Flask.
# Agrupa todos los endpoints de pruebas que serán invocados desde Spring
# o desde Postman para demostrar el tratamiento de excepciones.
lab_bp = Blueprint("lab", __name__, url_prefix="/api/lab")


def _success_response(operation: str, data: dict, http_status: int = 200):
    """
    Construye una respuesta homogénea de éxito.

    Todas las operaciones correctas del API Flask devuelven la misma estructura:
    - success
    - source
    - operation
    - data
    - timestamp

    Esto facilita mucho el trabajo de Spring al interpretar la respuesta.
    """
    payload = {
        "success": True,
        "source": "python-api",
        "operation": operation,
        "data": data,
        "timestamp": datetime.now(timezone.utc).isoformat()
    }
    return jsonify(payload), http_status


@lab_bp.get("/files/read/<path:filename>")
def read_file_route(filename: str):
    """
    Prueba de acceso a ficheros.

    Dependiendo del nombre recibido, el servicio puede:
    - leer correctamente un fichero de prueba
    - lanzar un error de fichero inexistente
    - simular un acceso prohibido
    """
    data = read_file(filename)
    return _success_response("read_file", data)


@lab_bp.get("/database/users")
def list_users_route():
    """
    Prueba correcta de acceso a base de datos desde Flask.
    Recupera los usuarios persistidos en MySQL.
    """
    data = list_users()
    return _success_response("list_users", data)


@lab_bp.get("/database/force-error")
def force_database_error_route():
    """
    Prueba controlada de error SQL.
    Se usa para demostrar cómo se capturan y traducen errores de acceso a datos.
    """
    data = force_database_error()
    return _success_response("force_database_error", data)


@lab_bp.get("/pokemon/<name>")
def get_pokemon_route(name: str):
    """
    Invocación a la API externa de Pokémon.

    Flask actúa aquí como servicio intermedio:
    - recibe la petición desde Spring
    - consulta la API externa
    - devuelve una respuesta homogénea o un error estructurado
    """
    data = get_pokemon(name)
    return _success_response("get_pokemon", data)


@lab_bp.get("/network/timeout")
def timeout_route():
    """
    Simulación de timeout de red.

    Este endpoint duerme el número de segundos indicado. El timeout real no lo
    provoca Flask, sino el cliente que llama a este endpoint si su tiempo máximo
    de espera es menor que el retardo configurado.
    """
    raw_seconds = request.args.get("seconds", "10")

    try:
        seconds = int(raw_seconds)
    except ValueError as exc:
        raise AppException(
            category="NETWORK_ERROR",
            error_code="INVALID_TIMEOUT_VALUE",
            user_message="El parámetro 'seconds' debe ser numérico.",
            technical_message=str(exc),
            http_status=400,
            critical=False
        ) from exc

    if seconds < 1 or seconds > 30:
        raise AppException(
            category="NETWORK_ERROR",
            error_code="INVALID_TIMEOUT_RANGE",
            user_message="El valor de 'seconds' debe estar entre 1 y 30.",
            technical_message=f"Valor recibido fuera de rango: {seconds}",
            http_status=400,
            critical=False
        )

    time.sleep(seconds)

    return _success_response(
        "timeout_simulation",
        {
            "sleptSeconds": seconds,
            "message": "La API ha esperado el tiempo indicado. El timeout real lo provocará el cliente si su espera máxima es menor."
        }
    )