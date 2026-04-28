import os
import requests
from app.errors.custom_exceptions import ExternalApiException, NetworkTimeoutException

# URL base del servicio externo.
# Se deja parametrizable por variable de entorno para que el código sea
# flexible y no dependa de una constante rígida.
POKEAPI_BASE_URL = os.getenv("POKEAPI_BASE_URL", "https://pokeapi.co/api/v2")


def get_pokemon(name: str) -> dict:
    """
    Consulta un Pokémon en la API externa y devuelve una estructura enriquecida
    con datos útiles para el laboratorio.
    """
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

        # Caso funcional esperado: el recurso no existe.
        # No es un fallo interno del sistema, sino un error controlado.
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

        # Se devuelve un diccionario con la información que más aporta valor
        # visual y funcional al laboratorio.
        return {
            "name": payload["name"],
            "id": payload["id"],
            "height": payload["height"],
            "weight": payload["weight"],
            "base_experience": payload.get("base_experience"),
            "types": [item["type"]["name"] for item in payload.get("types", [])],
            "abilities": [
                item["ability"]["name"] for item in payload.get("abilities", [])
            ],
            "stats": [
                {
                    "name": item["stat"]["name"],
                    "value": item["base_stat"]
                }
                for item in payload.get("stats", [])
            ],
            "sprites": {
                "official_artwork": (
                    payload.get("sprites", {})
                    .get("other", {})
                    .get("official-artwork", {})
                    .get("front_default")
                ),
                "front_default": payload.get("sprites", {}).get("front_default")
            }
        }

    except requests.exceptions.Timeout as exc:
        # Timeout real del servicio externo.
        raise NetworkTimeoutException(
            error_code="EXTERNAL_API_TIMEOUT",
            user_message="La API externa de Pokémon ha tardado demasiado en responder.",
            technical_message=str(exc),
            http_status=504,
            critical=True
        ) from exc
    except requests.exceptions.RequestException as exc:
        # Cualquier otro fallo general de comunicación con el servicio externo.
        raise ExternalApiException(
            error_code="EXTERNAL_API_ERROR",
            user_message="No se ha podido completar la llamada a la API externa de Pokémon.",
            technical_message=str(exc),
            http_status=502,
            critical=True
        ) from exc