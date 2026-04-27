from flask import Blueprint, jsonify

health_bp = Blueprint("health", __name__)


@health_bp.get("/")
def root():
    return jsonify({
        "service": "python-api",
        "message": "API Flask operativa"
    }), 200


@health_bp.get("/health")
def health():
    return jsonify({
        "status": "ok",
        "service": "python-api"
    }), 200