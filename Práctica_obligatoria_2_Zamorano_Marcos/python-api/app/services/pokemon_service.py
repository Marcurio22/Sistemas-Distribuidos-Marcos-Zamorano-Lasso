import os
import requests
from app.errors.custom_exceptions import ExternalApiException, NetworkTimeoutException

POKEAPI_BASE_URL = os.getenv("POKEAPI_BASE_URL", "https://pokeapi.co/api/v2")


def get_pokemon(name: str) -> dict:
    pokemon_name = name.strip().lower()

    if not pokemon_name:
        raise ExternalApiException(
            error_code="POKEMON_NAME_REQUIRED",
            user_message="Debes indicar un nombre de Pokémon válido.",
            technical_message="El parámetro 'name' ha llegado vacío.",
            http_status=400,
            critical=False
        )

    url = f"{POKEAPI_BASE_URL}/pokemon/{pokemon_name}"

    try:
        response = requests.get(url, timeout=5)

        if response.status_code == 404:
            raise ExternalApiException(
                error_code="POKEMON_NOT_FOUND",
                user_message=f"No existe el Pokémon '{pokemon_name}' en el servicio externo.",
                technical_message=f"La API externa respondió 404 para '{pokemon_name}'.",
                http_status=404,
                critical=False
            )

        response.raise_for_status()
        payload = response.json()

        return {
            "name": payload["name"],
            "id": payload["id"],
            "height": payload["height"],
            "weight": payload["weight"],
            "types": [item["type"]["name"] for item in payload.get("types", [])]
        }

    except requests.exceptions.Timeout as exc:
        raise NetworkTimeoutException(
            error_code="EXTERNAL_API_TIMEOUT",
            user_message="La API externa de Pokémon ha tardado demasiado en responder.",
            technical_message=str(exc),
            http_status=504,
            critical=True
        ) from exc
    except requests.exceptions.RequestException as exc:
        raise ExternalApiException(
            error_code="EXTERNAL_API_ERROR",
            user_message="No se ha podido completar la llamada a la API externa de Pokémon.",
            technical_message=str(exc),
            http_status=502,
            critical=True
        ) from exc