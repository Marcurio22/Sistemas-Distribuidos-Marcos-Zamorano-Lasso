class AppException(Exception):
    """
    Excepción base de la aplicación Flask.

    Todas las excepciones específicas del laboratorio heredan de esta clase
    para mantener un formato homogéneo de error en la API.
    """

    def __init__(
            self,
            *,
            category: str,
            error_code: str,
            user_message: str,
            technical_message: str | None = None,
            http_status: int = 400,
            critical: bool = False
    ):
        super().__init__(technical_message or user_message)
        self.category = category
        self.error_code = error_code
        self.user_message = user_message
        self.technical_message = technical_message or user_message
        self.http_status = http_status
        self.critical = critical


class FileAccessException(AppException):
    """
    Errores relacionados con acceso a ficheros del laboratorio.
    """

    def __init__(self, *, error_code: str, user_message: str, technical_message: str, http_status: int, critical: bool = False):
        super().__init__(
            category="FILE_ERROR",
            error_code=error_code,
            user_message=user_message,
            technical_message=technical_message,
            http_status=http_status,
            critical=critical
        )


class DatabaseAccessException(AppException):
    """
    Errores relacionados con consultas o acceso a base de datos.
    """

    def __init__(self, *, error_code: str, user_message: str, technical_message: str, http_status: int = 500, critical: bool = True):
        super().__init__(
            category="DATABASE_ERROR",
            error_code=error_code,
            user_message=user_message,
            technical_message=technical_message,
            http_status=http_status,
            critical=critical
        )


class ExternalApiException(AppException):
    """
    Errores producidos al invocar servicios externos, como la PokeAPI.
    """

    def __init__(self, *, error_code: str, user_message: str, technical_message: str, http_status: int, critical: bool):
        super().__init__(
            category="EXTERNAL_API_ERROR",
            error_code=error_code,
            user_message=user_message,
            technical_message=technical_message,
            http_status=http_status,
            critical=critical
        )


class NetworkTimeoutException(AppException):
    """
    Error específico para timeouts de red.
    Se separa de ExternalApiException para poder distinguir claramente
    los fallos de espera excedida respecto a otros errores remotos.
    """

    def __init__(self, *, error_code: str, user_message: str, technical_message: str, http_status: int = 504, critical: bool = True):
        super().__init__(
            category="NETWORK_ERROR",
            error_code=error_code,
            user_message=user_message,
            technical_message=technical_message,
            http_status=http_status,
            critical=critical
        )