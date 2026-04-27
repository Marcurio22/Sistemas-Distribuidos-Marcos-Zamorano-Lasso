class AppException(Exception):
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
    def __init__(self, *, error_code: str, user_message: str, technical_message: str, http_status: int = 504, critical: bool = True):
        super().__init__(
            category="NETWORK_ERROR",
            error_code=error_code,
            user_message=user_message,
            technical_message=technical_message,
            http_status=http_status,
            critical=critical
        )