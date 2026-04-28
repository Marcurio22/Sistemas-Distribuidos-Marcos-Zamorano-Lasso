from pathlib import Path
from app.errors.custom_exceptions import FileAccessException

# Directorio base donde se encuentran los ficheros de prueba del laboratorio.
FILES_BASE_PATH = Path(__file__).resolve().parents[2] / "files"


def read_file(filename: str) -> dict:
    """
    Lee un fichero de prueba del laboratorio o lanza una excepción controlada.

    Casos cubiertos:
    - nombre de fichero no válido
    - acceso prohibido simulado
    - fichero inexistente
    - error real de lectura
    """
    safe_name = Path(filename).name

    # Se evita que el usuario pueda intentar rutas arbitrarias como ../..
    # y se restringe la lectura al nombre simple del fichero.
    if safe_name != filename:
        raise FileAccessException(
            error_code="INVALID_FILE_NAME",
            user_message="El nombre del fichero no es válido.",
            technical_message=f"Se ha intentado acceder a una ruta no permitida: '{filename}'.",
            http_status=400,
            critical=False
        )

    # Caso didáctico: acceso prohibido simulado.
    if safe_name == "confidential.txt":
        raise FileAccessException(
            error_code="FILE_FORBIDDEN",
            user_message="No tienes permisos para leer ese fichero de prueba.",
            technical_message="Se ha simulado un acceso prohibido al fichero 'confidential.txt'.",
            http_status=403,
            critical=False
        )

    file_path = FILES_BASE_PATH / safe_name

    if not file_path.exists():
        raise FileAccessException(
            error_code="FILE_NOT_FOUND",
            user_message="No se ha encontrado el fichero solicitado.",
            technical_message=f"El fichero '{file_path}' no existe.",
            http_status=404,
            critical=False
        )

    try:
        content = file_path.read_text(encoding="utf-8")
        return {
            "filename": safe_name,
            "content": content,
            "sizeBytes": file_path.stat().st_size
        }
    except PermissionError as exc:
        raise FileAccessException(
            error_code="FILE_PERMISSION_ERROR",
            user_message="No se tienen permisos suficientes para leer el fichero.",
            technical_message=str(exc),
            http_status=403,
            critical=False
        ) from exc
    except OSError as exc:
        raise FileAccessException(
            error_code="FILE_READ_ERROR",
            user_message="Se ha producido un error al leer el fichero.",
            technical_message=str(exc),
            http_status=500,
            critical=True
        ) from exc