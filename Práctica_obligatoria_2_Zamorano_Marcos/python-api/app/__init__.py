from flask import Flask
from app.routes.health_routes import health_bp
from app.routes.lab_routes import lab_bp
from app.errors.handlers import register_error_handlers


def create_app():
    app = Flask(__name__)

    app.register_blueprint(health_bp)
    app.register_blueprint(lab_bp)

    register_error_handlers(app)

    return app