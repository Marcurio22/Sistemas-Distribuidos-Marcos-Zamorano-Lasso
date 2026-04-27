import mysql.connector
from app.errors.custom_exceptions import DatabaseAccessException
from app.infra.mysql_client import get_connection


def list_users() -> dict:
    connection = None
    cursor = None

    try:
        connection = get_connection()
        cursor = connection.cursor(dictionary=True)

        cursor.execute("""
                       SELECT id, username, email, role, enabled, last_login_at
                       FROM users
                       ORDER BY id
                       """)

        rows = cursor.fetchall()

        for row in rows:
            if row.get("last_login_at") is not None:
                row["last_login_at"] = row["last_login_at"].isoformat()

        return {
            "count": len(rows),
            "users": rows
        }

    except mysql.connector.Error as exc:
        raise DatabaseAccessException(
            error_code="DATABASE_READ_ERROR",
            user_message="Se ha producido un error al consultar la base de datos.",
            technical_message=str(exc),
            http_status=500,
            critical=True
        ) from exc
    finally:
        if cursor is not None:
            cursor.close()
        if connection is not None and connection.is_connected():
            connection.close()


def force_database_error() -> dict:
    connection = None
    cursor = None

    try:
        connection = get_connection()
        cursor = connection.cursor(dictionary=True)

        cursor.execute("SELECT * FROM tabla_que_no_existe")
        rows = cursor.fetchall()

        return {
            "count": len(rows),
            "rows": rows
        }

    except mysql.connector.Error as exc:
        raise DatabaseAccessException(
            error_code="DATABASE_FORCED_ERROR",
            user_message="Se ha provocado de forma controlada un error de acceso a base de datos.",
            technical_message=str(exc),
            http_status=500,
            critical=True
        ) from exc
    finally:
        if cursor is not None:
            cursor.close()
        if connection is not None and connection.is_connected():
            connection.close()