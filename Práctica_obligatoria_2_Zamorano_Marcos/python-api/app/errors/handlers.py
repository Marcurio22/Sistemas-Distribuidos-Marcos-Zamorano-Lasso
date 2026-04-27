from datetime import datetime, timezone
from flask import jsonify, request
from app.errors.custom_exceptions import AppException


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def _build_error_payload(error: AppException) -> dict:
    return {
        "success": False,
        "source": "python-api",
        "category": error.category,
        "errorCode": error.error_code,
        "httpStatus": error.http_status,
        "userMessage": error.user_message,
        "technicalMessage": error.technical_message,
        "critical": error.critical,
        "path": request.path,
        "timestamp": _utc_now_iso()
    }


def register_error_handlers(app):
    @app.errorhandler(AppException)
    def handle_app_exception(error: AppException):
        return jsonify(_build_error_payload(error)), error.http_status

    @app.errorhandler(404)
    def handle_not_found(_error):
        payload = {
            "success": False,
            "source": "python-api",
            "category": "HTTP_ERROR",
            "errorCode": "ROUTE_NOT_FOUND",
            "httpStatus": 404,
            "userMessage": "La ruta solicitada no existe en el servicio Flask.",
            "technicalMessage": f"No existe la ruta '{request.path}'.",
            "critical": False,
            "path": request.path,
            "timestamp": _utc_now_iso()
        }
        return jsonify(payload), 404

    @app.errorhandler(405)
    def handle_method_not_allowed(_error):
        payload = {
            "success": False,
            "source": "python-api",
            "category": "HTTP_ERROR",
            "errorCode": "METHOD_NOT_ALLOWED",
            "httpStatus": 405,
            "userMessage": "El método HTTP utilizado no está permitido para esta ruta.",
            "technicalMessage": f"Método HTTP no permitido en '{request.path}'.",
            "critical": False,
            "path": request.path,
            "timestamp": _utc_now_iso()
        }
        return jsonify(payload), 405

    @app.errorhandler(Exception)
    def handle_unexpected_exception(error: Exception):
        payload = {
            "success": False,
            "source": "python-api",
            "category": "INTERNAL_ERROR",
            "errorCode": "UNEXPECTED_ERROR",
            "httpStatus": 500,
            "userMessage": "Se ha producido un error interno no controlado en la API Flask.",
            "technicalMessage": str(error),
            "critical": True,
            "path": request.path,
            "timestamp": _utc_now_iso()
        }
        return jsonify(payload), 500