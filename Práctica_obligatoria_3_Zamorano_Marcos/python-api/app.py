"""
El Plantío 360 - Microservicio Flask de sensores simulados.
Autor: Marcos Zamorano Lasso
Práctica 3 - Sistemas Distribuidos
"""

from __future__ import annotations

from datetime import datetime, timezone
from random import randint, uniform
from typing import Any

from flask import Flask, jsonify

app = Flask(__name__)


BASE_SENSORS = [
    {"name": "Parking Norte", "type": "PARKING", "unit": "%", "latitude": 42.3512, "longitude": -3.6902},
    {"name": "Puerta 3", "type": "AFLUENCIA", "unit": "%", "latitude": 42.3497, "longitude": -3.6883},
    {"name": "Césped", "type": "HUMEDAD", "unit": "%", "latitude": 42.3500, "longitude": -3.6890},
    {"name": "Temperatura estadio", "type": "TEMPERATURA", "unit": "ºC", "latitude": 42.3500, "longitude": -3.6890},
]


def status_for(sensor_type: str, value: float) -> str:
    """
    Calcula el estado semafórico de una lectura simulada.

    Args:
        sensor_type: Tipo funcional del sensor.
        value: Valor numérico de la lectura.

    Returns:
        Estado NORMAL, WARNING o CRITICAL.
    """
    if sensor_type in {"PARKING", "AFLUENCIA"}:
        if value >= 90:
            return "CRITICAL"
        if value >= 70:
            return "WARNING"
        return "NORMAL"
    if sensor_type == "HUMEDAD" and value < 25:
        return "WARNING"
    return "NORMAL"


def generate_value(sensor_type: str) -> float:
    """
    Genera un valor realista según el tipo de sensor.

    Args:
        sensor_type: Tipo funcional del sensor.

    Returns:
        Valor numérico con escala apropiada.
    """
    if sensor_type in {"PARKING", "AFLUENCIA"}:
        return randint(35, 96)
    if sensor_type == "HUMEDAD":
        return randint(22, 48)
    return round(uniform(8.0, 22.0), 1)


@app.get("/health")
def health() -> tuple[dict[str, str], int]:
    """
    Expone un endpoint de salud para Docker.

    Returns:
        Estado del microservicio.
    """
    return {"status": "UP", "service": "plantio360-python-api"}, 200


@app.get("/api/sensors")
def sensors() -> Any:
    """
    Devuelve sensores simulados para el visor cartográfico.

    Returns:
        JSON con lecturas remotas simuladas.
    """
    now = datetime.now(timezone.utc).isoformat()
    readings = []
    for sensor in BASE_SENSORS:
        value = generate_value(sensor["type"])
        readings.append(
            {
                **sensor,
                "value": value,
                "status": status_for(sensor["type"], value),
                "capturedAt": now,
            }
        )
    return jsonify(readings)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
