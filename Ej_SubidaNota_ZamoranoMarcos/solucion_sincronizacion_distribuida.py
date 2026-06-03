"""
Solución del ejercicio opcional de Sistemas Distribuidos.

Autor: Marcos Zamorano Lasso

Este programa analiza una matriz de eventos distribuida, calcula relojes
vectoriales, obtiene relaciones de precedencia causal, detecta eventos
concurrentes y evalúa cortes consistentes para un instante T dado.

Formatos de entrada soportados por celda
---------------------------------------
- 0, "", None o "*"  : no hay evento en esa casilla.
- e                  : evento independiente.
- i-n-m              : invocación enviada desde Pn hacia Pm.
- e-i-n-m            : recepción en Pm de la invocación i-n-m.
- r-m-n              : respuesta enviada desde Pm hacia Pn.
- e-r-m-n            : recepción en Pn de la respuesta r-m-n.
- mX o m-X           : mensaje simple desde el proceso de la fila actual
                       hacia PX. Su recepción se genera automáticamente
                       en PX en T+1, tal como se indicó en clase.

Notas de diseño
---------------
1. Cada evento real recibe un nombre e<proceso>,<contador-local>. Por ejemplo,
   el tercer evento de P1 se llama e1,3.
2. En T0, si no hay evento, se imprime el vector inicial 0,0,...,0. En el resto
   de casillas vacías se imprime "*".
3. Para un evento de recepción, el reloj vectorial local se mezcla con el vector
   enviado por el emisor. El orden por defecto es "increment_then_merge" porque
   antes de marcar un suceso se incrementa la componente propia y, en recepción,
   se calcula el máximo componente a componente.
   Si se desea la convención alternativa clásica, puede usarse
   receive_update_order="merge_then_increment".
4. La relación causal completa se calcula comparando relojes vectoriales:
   V(e) < V(e') implica e -> e'. Si ni V(e) <= V(e') ni V(e') <= V(e), los
   eventos son concurrentes.

Uso rápido
----------
    python solucion_sincronizacion_distribuida.py
    python solucion_sincronizacion_distribuida.py --corte 7
    python solucion_sincronizacion_distribuida.py --archivo matriz.json --corte 5
    python solucion_sincronizacion_distribuida.py --archivo matriz.csv --corte 5
    python solucion_sincronizacion_distribuida.py --test

El archivo JSON debe contener una lista de listas. El CSV debe contener sólo la matriz de procesos,
sin columna de nombres P1/P2 ni cabecera T0/T1.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import unittest
from collections import defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Deque, Dict, Iterable, List, Optional, Sequence, Tuple

Vector = Tuple[int, ...]
MessageKey = Tuple[str, int, int, str]


# Matriz del enunciado.
EJEMPLO_ENUNCIADO: List[List[str]] = [
    ["0", "i-1-3", "e-i-2-1", "0", "e-r-3-1", "r-2-1", "r-3-1", "e"],
    ["0", "i-2-1", "0", "0", "0", "e-i-3-2", "e-r-2-1", "0"],
    ["0", "e", "e-i-1-3", "r-3-1", "i-3-2", "e", "0", "e-r-3-1"],
]


@dataclass(frozen=True)
class Evento:
    """Representa un evento ya fechado con reloj vectorial."""

    proceso: int
    tiempo: int
    indice_local: int
    nombre: str
    token_original: str
    tipo: str
    vector: Vector

    def clave_orden(self) -> Tuple[int, int, int]:
        """Orden físico usado sólo para mostrar resultados de forma estable."""
        return (self.tiempo, self.proceso, self.indice_local)


@dataclass(frozen=True)
class MensajePendiente:
    """Mensaje enviado que todavía no ha sido recibido."""

    clave: MessageKey
    emisor: int
    receptor: int
    tipo: str
    evento_envio: Evento


@dataclass(frozen=True)
class AristaMensaje:
    """Relación causal directa de tipo envío -> recepción."""

    clave: MessageKey
    emisor: int
    receptor: int
    tipo: str
    envio: Evento
    recepcion: Evento


@dataclass
class CeldaSalida:
    """Contenido final que se imprimirá para una casilla de la matriz."""

    tiempo: int
    eventos: List[Evento] = field(default_factory=list)
    vector_inicial: Optional[Vector] = None

    def nombre_eventos(self) -> str:
        if not self.eventos:
            return "*"
        return " | ".join(evento.nombre for evento in self.eventos)

    def vectores(self) -> str:
        if self.eventos:
            return " | ".join(formatear_vector(evento.vector) for evento in self.eventos)
        if self.vector_inicial is not None:
            return formatear_vector(self.vector_inicial)
        return "*"

    def como_texto_multilinea(self) -> str:
        return f"{self.tiempo}\n{self.nombre_eventos()}\n{self.vectores()}"


@dataclass(frozen=True)
class ViolacionCorte:
    """Violación de consistencia: una recepción está dentro y su envío fuera."""

    recepcion: Evento
    envio: Evento
    mensaje: AristaMensaje


@dataclass
class InformeCorte:
    """Resultado de evaluar un corte global por columna temporal."""

    tiempo: int
    consistente: bool
    eventos_ejecutados: List[Evento]
    eventos_no_ejecutados: List[Evento]
    violaciones: List[ViolacionCorte]


@dataclass
class ResultadoAnalisis:
    """Resultado completo del análisis de la matriz."""

    matriz_salida: List[List[CeldaSalida]]
    eventos: List[Evento]
    mensajes: List[AristaMensaje]
    relaciones_directas: List[Tuple[Evento, Evento]]
    relaciones_causales: List[Tuple[Evento, Evento]]
    concurrentes: List[Tuple[Evento, Evento]]
    advertencias: List[str]

    def evaluar_corte(self, tiempo: int) -> InformeCorte:
        """
        Evalúa el corte global C(T) que contiene todos los eventos con columna <= T.

        Un corte es consistente si, para cada recepción incluida en el corte,
        el envío del mensaje correspondiente también está incluido.
        """
        ejecutados = [evento for evento in self.eventos if evento.tiempo <= tiempo]
        no_ejecutados = [evento for evento in self.eventos if evento.tiempo > tiempo]
        ejecutados_set = set(ejecutados)
        violaciones: List[ViolacionCorte] = []

        for mensaje in self.mensajes:
            if mensaje.recepcion in ejecutados_set and mensaje.envio not in ejecutados_set:
                violaciones.append(
                    ViolacionCorte(
                        recepcion=mensaje.recepcion,
                        envio=mensaje.envio,
                        mensaje=mensaje,
                    )
                )

        return InformeCorte(
            tiempo=tiempo,
            consistente=not violaciones,
            eventos_ejecutados=ejecutados,
            eventos_no_ejecutados=no_ejecutados,
            violaciones=violaciones,
        )


class ErrorMatrizEventos(ValueError):
    """Error semántico o sintáctico en la matriz de entrada."""


class AnalizadorEventosDistribuidos:
    """Analizador de relojes vectoriales, causalidad, concurrencia y cortes."""

    _RE_INVOCACION = re.compile(r"^i-(\d+)-(\d+)$")
    _RE_RECEPCION_INVOCACION = re.compile(r"^e-i-(\d+)-(\d+)$")
    _RE_RESPUESTA = re.compile(r"^r-(\d+)-(\d+)$")
    _RE_RECEPCION_RESPUESTA = re.compile(r"^e-r-(\d+)-(\d+)$")
    _RE_MENSAJE_SIMPLE = re.compile(r"^m-?(\d+)$")
    _RE_RECEPCION_SIMPLE = re.compile(r"^e-m-(\d+)-(\d+)-(\d+)-(\d+)$")

    def __init__(
        self,
        matriz: Sequence[Sequence[object]],
        *,
        receive_update_order: str = "increment_then_merge",
        generar_recepciones_mx: bool = True,
        validar_filas: bool = True,
    ) -> None:
        if receive_update_order not in {"increment_then_merge", "merge_then_increment"}:
            raise ValueError(
                "receive_update_order debe ser 'increment_then_merge' o 'merge_then_increment'."
            )
        self.matriz_original = self._normalizar_matriz(matriz)
        self.num_procesos = len(self.matriz_original)
        self.num_tiempos = len(self.matriz_original[0]) if self.num_procesos else 0
        self.receive_update_order = receive_update_order
        self.generar_recepciones_mx = generar_recepciones_mx
        self.validar_filas = validar_filas
        self.advertencias: List[str] = []

    def analizar(self) -> ResultadoAnalisis:
        """Ejecuta el análisis completo de la matriz."""
        tokens = self._crear_grid_tokens_con_recepciones_automaticas()
        vectores_actuales: List[List[int]] = [
            [0 for _ in range(self.num_procesos)] for _ in range(self.num_procesos)
        ]
        indices_locales = [0 for _ in range(self.num_procesos)]
        pendientes: Dict[MessageKey, Deque[MensajePendiente]] = defaultdict(deque)
        mensajes_recibidos: List[AristaMensaje] = []
        eventos_por_proceso: Dict[int, List[Evento]] = defaultdict(list)
        matriz_salida = [
            [CeldaSalida(tiempo=t) for t in range(self.num_tiempos)]
            for _ in range(self.num_procesos)
        ]

        vector_cero = tuple(0 for _ in range(self.num_procesos))
        for proceso_idx in range(self.num_procesos):
            if not tokens[proceso_idx][0]:
                matriz_salida[proceso_idx][0].vector_inicial = vector_cero

        for tiempo in range(self.num_tiempos):
            for proceso_idx in range(self.num_procesos):
                proceso = proceso_idx + 1
                for token in tokens[proceso_idx][tiempo]:
                    descriptor = self._describir_token(token, proceso, tiempo)
                    vector_recepcion: Optional[Vector] = None
                    mensaje_pendiente: Optional[MensajePendiente] = None

                    if descriptor["es_recepcion"]:
                        clave = descriptor["clave"]
                        if not pendientes[clave]:
                            raise ErrorMatrizEventos(
                                f"En P{proceso}, T{tiempo}, aparece la recepción '{token}', "
                                f"pero no existe un envío previo pendiente para la clave {clave}."
                            )
                        mensaje_pendiente = pendientes[clave].popleft()
                        vector_recepcion = mensaje_pendiente.evento_envio.vector

                    self._actualizar_vector(
                        vector_local=vectores_actuales[proceso_idx],
                        proceso_idx=proceso_idx,
                        vector_mensaje=vector_recepcion,
                    )
                    indices_locales[proceso_idx] += 1
                    nombre = f"e{proceso},{indices_locales[proceso_idx]}"
                    evento = Evento(
                        proceso=proceso,
                        tiempo=tiempo,
                        indice_local=indices_locales[proceso_idx],
                        nombre=nombre,
                        token_original=token,
                        tipo=descriptor["tipo"],
                        vector=tuple(vectores_actuales[proceso_idx]),
                    )
                    eventos_por_proceso[proceso].append(evento)
                    matriz_salida[proceso_idx][tiempo].eventos.append(evento)

                    if descriptor["es_envio"]:
                        pendientes[descriptor["clave"]].append(
                            MensajePendiente(
                                clave=descriptor["clave"],
                                emisor=descriptor["emisor"],
                                receptor=descriptor["receptor"],
                                tipo=descriptor["tipo_mensaje"],
                                evento_envio=evento,
                            )
                        )

                    if descriptor["es_recepcion"] and mensaje_pendiente is not None:
                        mensajes_recibidos.append(
                            AristaMensaje(
                                clave=mensaje_pendiente.clave,
                                emisor=mensaje_pendiente.emisor,
                                receptor=mensaje_pendiente.receptor,
                                tipo=mensaje_pendiente.tipo,
                                envio=mensaje_pendiente.evento_envio,
                                recepcion=evento,
                            )
                        )

        for clave, cola in pendientes.items():
            for pendiente in cola:
                self.advertencias.append(
                    f"El envío {pendiente.evento_envio.nombre} ({pendiente.evento_envio.token_original}) "
                    f"no tiene recepción asociada. Clave pendiente: {clave}."
                )

        eventos = sorted(
            [evento for eventos_proceso in eventos_por_proceso.values() for evento in eventos_proceso],
            key=lambda evento: evento.clave_orden(),
        )
        relaciones_directas = self._calcular_relaciones_directas(
            eventos_por_proceso=eventos_por_proceso,
            mensajes=mensajes_recibidos,
        )
        relaciones_causales, concurrentes = self._calcular_causalidad_y_concurrencia(eventos)

        return ResultadoAnalisis(
            matriz_salida=matriz_salida,
            eventos=eventos,
            mensajes=mensajes_recibidos,
            relaciones_directas=relaciones_directas,
            relaciones_causales=relaciones_causales,
            concurrentes=concurrentes,
            advertencias=self.advertencias,
        )

    def _normalizar_matriz(self, matriz: Sequence[Sequence[object]]) -> List[List[str]]:
        if not matriz:
            raise ErrorMatrizEventos("La matriz no puede estar vacía.")
        normalizada: List[List[str]] = []
        anchura: Optional[int] = None

        for fila_idx, fila in enumerate(matriz, start=1):
            fila_normalizada = ["" if valor is None else str(valor).strip() for valor in fila]
            if anchura is None:
                anchura = len(fila_normalizada)
                if anchura == 0:
                    raise ErrorMatrizEventos("Las filas no pueden estar vacías.")
            elif len(fila_normalizada) != anchura:
                raise ErrorMatrizEventos(
                    f"La fila P{fila_idx} tiene {len(fila_normalizada)} columnas; "
                    f"se esperaban {anchura}."
                )
            normalizada.append(fila_normalizada)

        return normalizada

    def _crear_grid_tokens_con_recepciones_automaticas(self) -> List[List[List[str]]]:
        tokens = [
            [self._tokenizar_celda(celda) for celda in fila]
            for fila in self.matriz_original
        ]

        if not self.generar_recepciones_mx:
            return tokens

        for proceso_idx, fila in enumerate(list(tokens)):
            proceso = proceso_idx + 1
            for tiempo, celda_tokens in enumerate(list(fila)):
                for indice_token, token in enumerate(list(celda_tokens)):
                    match = self._RE_MENSAJE_SIMPLE.fullmatch(token)
                    if not match:
                        continue
                    receptor = int(match.group(1))
                    self._validar_proceso(receptor, token, proceso, tiempo)
                    tiempo_recepcion = tiempo + 1
                    if tiempo_recepcion >= self.num_tiempos:
                        raise ErrorMatrizEventos(
                            f"El mensaje simple '{token}' en P{proceso}, T{tiempo} debería recibirse "
                            f"en T{tiempo_recepcion}, pero la matriz sólo llega hasta T{self.num_tiempos - 1}."
                        )
                    token_recepcion = f"e-m-{proceso}-{receptor}-{tiempo}-{indice_token}"
                    tokens[receptor - 1][tiempo_recepcion].append(token_recepcion)

        return tokens

    @staticmethod
    def _tokenizar_celda(celda: str) -> List[str]:
        celda_limpia = celda.strip()
        if celda_limpia in {"", "0", "*"}:
            return []
        return [
            token.strip()
            for token in re.split(r"[;|\n]+", celda_limpia)
            if token.strip() and token.strip() not in {"0", "*"}
        ]

    def _describir_token(self, token: str, proceso_actual: int, tiempo: int) -> Dict[str, object]:
        """Clasifica una celda y devuelve la información necesaria para procesarla."""
        match = self._RE_INVOCACION.fullmatch(token)
        if match:
            emisor, receptor = map(int, match.groups())
            self._validar_emisor_receptor(token, proceso_actual, tiempo, emisor, receptor, emisor)
            return self._descriptor_envio(
                tipo="invocacion",
                tipo_mensaje="invocacion",
                clave=("invocacion", emisor, receptor, ""),
                emisor=emisor,
                receptor=receptor,
            )

        match = self._RE_RECEPCION_INVOCACION.fullmatch(token)
        if match:
            emisor, receptor = map(int, match.groups())
            self._validar_emisor_receptor(token, proceso_actual, tiempo, emisor, receptor, receptor)
            return self._descriptor_recepcion(
                tipo="recepcion_invocacion",
                tipo_mensaje="invocacion",
                clave=("invocacion", emisor, receptor, ""),
                emisor=emisor,
                receptor=receptor,
            )

        match = self._RE_RESPUESTA.fullmatch(token)
        if match:
            # Se interpreta la dirección real a partir de la fila donde
            # aparece el envío y de la fila donde aparece la recepción.
            # La clave mantiene los dos índices del token
            # para emparejar r-a-b con e-r-a-b.
            extremo_a, extremo_b = map(int, match.groups())
            self._validar_proceso(extremo_a, token, proceso_actual, tiempo)
            self._validar_proceso(extremo_b, token, proceso_actual, tiempo)
            if proceso_actual not in {extremo_a, extremo_b}:
                raise ErrorMatrizEventos(
                    f"El token '{token}' está en P{proceso_actual}, T{tiempo}, "
                    f"pero la respuesta sólo referencia P{extremo_a} y P{extremo_b}."
                )
            receptor = extremo_b if proceso_actual == extremo_a else extremo_a
            return self._descriptor_envio(
                tipo="respuesta",
                tipo_mensaje="respuesta",
                clave=("respuesta", extremo_a, extremo_b, ""),
                emisor=proceso_actual,
                receptor=receptor,
            )

        match = self._RE_RECEPCION_RESPUESTA.fullmatch(token)
        if match:
            extremo_a, extremo_b = map(int, match.groups())
            self._validar_proceso(extremo_a, token, proceso_actual, tiempo)
            self._validar_proceso(extremo_b, token, proceso_actual, tiempo)
            if proceso_actual not in {extremo_a, extremo_b}:
                raise ErrorMatrizEventos(
                    f"El token '{token}' está en P{proceso_actual}, T{tiempo}, "
                    f"pero la recepción sólo referencia P{extremo_a} y P{extremo_b}."
                )
            emisor = extremo_b if proceso_actual == extremo_a else extremo_a
            return self._descriptor_recepcion(
                tipo="recepcion_respuesta",
                tipo_mensaje="respuesta",
                clave=("respuesta", extremo_a, extremo_b, ""),
                emisor=emisor,
                receptor=proceso_actual,
            )

        match = self._RE_MENSAJE_SIMPLE.fullmatch(token)
        if match:
            receptor = int(match.group(1))
            emisor = proceso_actual
            self._validar_emisor_receptor(token, proceso_actual, tiempo, emisor, receptor, emisor)
            clave = ("simple", emisor, receptor, f"{tiempo}:0")
            return self._descriptor_envio(
                tipo="mensaje_simple",
                tipo_mensaje="simple",
                clave=clave,
                emisor=emisor,
                receptor=receptor,
            )

        match = self._RE_RECEPCION_SIMPLE.fullmatch(token)
        if match:
            emisor, receptor, tiempo_envio, indice_token = map(int, match.groups())
            self._validar_emisor_receptor(token, proceso_actual, tiempo, emisor, receptor, receptor)
            clave = ("simple", emisor, receptor, f"{tiempo_envio}:{indice_token}")
            return self._descriptor_recepcion(
                tipo="recepcion_mensaje_simple",
                tipo_mensaje="simple",
                clave=clave,
                emisor=emisor,
                receptor=receptor,
            )

        if token == "e":
            return {
                "tipo": "independiente",
                "tipo_mensaje": None,
                "es_envio": False,
                "es_recepcion": False,
                "clave": None,
                "emisor": None,
                "receptor": None,
            }

        raise ErrorMatrizEventos(
            f"Token no reconocido en P{proceso_actual}, T{tiempo}: '{token}'."
        )

    @staticmethod
    def _descriptor_envio(
        *,
        tipo: str,
        tipo_mensaje: str,
        clave: MessageKey,
        emisor: int,
        receptor: int,
    ) -> Dict[str, object]:
        return {
            "tipo": tipo,
            "tipo_mensaje": tipo_mensaje,
            "es_envio": True,
            "es_recepcion": False,
            "clave": clave,
            "emisor": emisor,
            "receptor": receptor,
        }

    @staticmethod
    def _descriptor_recepcion(
        *,
        tipo: str,
        tipo_mensaje: str,
        clave: MessageKey,
        emisor: int,
        receptor: int,
    ) -> Dict[str, object]:
        return {
            "tipo": tipo,
            "tipo_mensaje": tipo_mensaje,
            "es_envio": False,
            "es_recepcion": True,
            "clave": clave,
            "emisor": emisor,
            "receptor": receptor,
        }

    def _validar_emisor_receptor(
        self,
        token: str,
        proceso_actual: int,
        tiempo: int,
        emisor: int,
        receptor: int,
        proceso_esperado: int,
    ) -> None:
        self._validar_proceso(emisor, token, proceso_actual, tiempo)
        self._validar_proceso(receptor, token, proceso_actual, tiempo)
        if self.validar_filas and proceso_actual != proceso_esperado:
            raise ErrorMatrizEventos(
                f"El token '{token}' está en P{proceso_actual}, T{tiempo}, "
                f"pero por su formato debería estar en P{proceso_esperado}."
            )

    def _validar_proceso(self, proceso: int, token: str, proceso_actual: int, tiempo: int) -> None:
        if proceso < 1 or proceso > self.num_procesos:
            raise ErrorMatrizEventos(
                f"El token '{token}' en P{proceso_actual}, T{tiempo} referencia P{proceso}, "
                f"pero sólo existen procesos P1..P{self.num_procesos}."
            )

    def _actualizar_vector(
        self,
        *,
        vector_local: List[int],
        proceso_idx: int,
        vector_mensaje: Optional[Vector],
    ) -> None:
        if vector_mensaje is None:
            vector_local[proceso_idx] += 1
            return

        if self.receive_update_order == "merge_then_increment":
            for idx, valor in enumerate(vector_mensaje):
                vector_local[idx] = max(vector_local[idx], valor)
            vector_local[proceso_idx] += 1
            return

        vector_local[proceso_idx] += 1
        for idx, valor in enumerate(vector_mensaje):
            vector_local[idx] = max(vector_local[idx], valor)

    @staticmethod
    def _calcular_relaciones_directas(
        *,
        eventos_por_proceso: Dict[int, List[Evento]],
        mensajes: List[AristaMensaje],
    ) -> List[Tuple[Evento, Evento]]:
        relaciones: List[Tuple[Evento, Evento]] = []
        vistas = set()

        for proceso in sorted(eventos_por_proceso):
            eventos = eventos_por_proceso[proceso]
            for anterior, posterior in zip(eventos, eventos[1:]):
                par = (anterior, posterior)
                relaciones.append(par)
                vistas.add(par)

        for mensaje in mensajes:
            par = (mensaje.envio, mensaje.recepcion)
            if par not in vistas:
                relaciones.append(par)
                vistas.add(par)

        return sorted(relaciones, key=lambda par: (par[0].clave_orden(), par[1].clave_orden()))

    @staticmethod
    def _calcular_causalidad_y_concurrencia(
        eventos: List[Evento],
    ) -> Tuple[List[Tuple[Evento, Evento]], List[Tuple[Evento, Evento]]]:
        causales: List[Tuple[Evento, Evento]] = []
        concurrentes: List[Tuple[Evento, Evento]] = []

        for indice, evento_a in enumerate(eventos):
            for evento_b in eventos[indice + 1 :]:
                if vector_menor(evento_a.vector, evento_b.vector):
                    causales.append((evento_a, evento_b))
                elif vector_menor(evento_b.vector, evento_a.vector):
                    causales.append((evento_b, evento_a))
                else:
                    concurrentes.append((evento_a, evento_b))

        causales.sort(key=lambda par: (par[0].clave_orden(), par[1].clave_orden()))
        concurrentes.sort(key=lambda par: (par[0].clave_orden(), par[1].clave_orden()))
        return causales, concurrentes


def vector_menor_igual(vector_a: Vector, vector_b: Vector) -> bool:
    """Devuelve True si vector_a <= vector_b componente a componente."""
    return all(a <= b for a, b in zip(vector_a, vector_b))


def vector_menor(vector_a: Vector, vector_b: Vector) -> bool:
    """Devuelve True si vector_a < vector_b componente a componente y son distintos."""
    return vector_a != vector_b and vector_menor_igual(vector_a, vector_b)


def formatear_vector(vector: Vector) -> str:
    """Convierte un vector en el formato solicitado: 1,0,2."""
    return ",".join(str(valor) for valor in vector)


def formatear_lista_eventos(eventos: Iterable[Evento]) -> str:
    """Devuelve una lista compacta de nombres de evento."""
    return ", ".join(evento.nombre for evento in eventos) or "ninguno"


def cargar_matriz_desde_archivo(ruta: Path) -> List[List[str]]:
    """Carga una matriz desde JSON o CSV."""
    if not ruta.exists():
        raise FileNotFoundError(f"No existe el archivo: {ruta}")

    if ruta.suffix.lower() == ".json":
        with ruta.open("r", encoding="utf-8") as archivo:
            datos = json.load(archivo)
        if not isinstance(datos, list) or not all(isinstance(fila, list) for fila in datos):
            raise ErrorMatrizEventos("El JSON debe contener una lista de listas.")
        return [["" if celda is None else str(celda) for celda in fila] for fila in datos]

    if ruta.suffix.lower() == ".csv":
        with ruta.open("r", encoding="utf-8", newline="") as archivo:
            lector = csv.reader(archivo)
            return [[celda.strip() for celda in fila] for fila in lector if fila]

    raise ErrorMatrizEventos("Formato no soportado. Use .json o .csv.")


def imprimir_tabla_resultado(resultado: ResultadoAnalisis) -> None:
    """Imprime la matriz de salida con tres líneas por casilla."""
    matriz = resultado.matriz_salida
    num_tiempos = len(matriz[0]) if matriz else 0
    cabecera = [""] + [f"T{t}" for t in range(num_tiempos)]
    filas: List[List[str]] = []

    for idx, fila in enumerate(matriz, start=1):
        filas.append([f"P{idx}"] + [celda.como_texto_multilinea() for celda in fila])

    imprimir_tabla(cabecera, filas)


def imprimir_tabla(cabecera: List[str], filas: List[List[str]]) -> None:
    """Imprime una tabla ASCII básica que soporta celdas multilínea."""
    todas_las_filas = [cabecera] + filas
    anchos = [0 for _ in cabecera]

    for fila in todas_las_filas:
        for idx, celda in enumerate(fila):
            lineas = str(celda).splitlines() or [""]
            anchos[idx] = max(anchos[idx], *(len(linea) for linea in lineas))

    def linea_separadora() -> str:
        return "+" + "+".join("-" * (ancho + 2) for ancho in anchos) + "+"

    def imprimir_fila(fila: List[str]) -> None:
        lineas_por_celda = [str(celda).splitlines() or [""] for celda in fila]
        altura = max(len(lineas) for lineas in lineas_por_celda)
        for linea_idx in range(altura):
            partes = []
            for col_idx, lineas in enumerate(lineas_por_celda):
                texto = lineas[linea_idx] if linea_idx < len(lineas) else ""
                partes.append(f" {texto:<{anchos[col_idx]}} ")
            print("|" + "|".join(partes) + "|")

    print(linea_separadora())
    imprimir_fila(cabecera)
    print(linea_separadora())
    for fila in filas:
        imprimir_fila(fila)
        print(linea_separadora())


def imprimir_relaciones(titulo: str, relaciones: Sequence[Tuple[Evento, Evento]], simbolo: str) -> None:
    """Imprime pares de eventos, uno por línea."""
    print(f"\n{titulo} ({len(relaciones)}):")
    if not relaciones:
        print("  ninguna")
        return
    for evento_a, evento_b in relaciones:
        print(f"  {evento_a.nombre} {simbolo} {evento_b.nombre}")


def imprimir_corte(informe: InformeCorte) -> None:
    """Imprime el informe de corte consistente."""
    print(f"\nCorte global C(T{informe.tiempo}):")
    print(f"  Consistente: {'sí' if informe.consistente else 'no'}")
    print(f"  Eventos ejecutados: {formatear_lista_eventos(informe.eventos_ejecutados)}")
    print(f"  Eventos no ejecutados: {formatear_lista_eventos(informe.eventos_no_ejecutados)}")
    if informe.violaciones:
        print("  Violaciones detectadas:")
        for violacion in informe.violaciones:
            print(
                f"    {violacion.recepcion.nombre} recibe un mensaje cuyo envío "
                f"{violacion.envio.nombre} no está dentro del corte."
            )


def ejecutar(matriz: Sequence[Sequence[object]], corte: Optional[int]) -> ResultadoAnalisis:
    """Función principal reutilizable desde tests o desde otros programas."""
    analizador = AnalizadorEventosDistribuidos(matriz)
    resultado = analizador.analizar()

    imprimir_tabla_resultado(resultado)
    imprimir_relaciones("Relaciones causales directas", resultado.relaciones_directas, "->")
    imprimir_relaciones("Relaciones causales completas por reloj vectorial", resultado.relaciones_causales, "->")
    imprimir_relaciones("Eventos concurrentes", resultado.concurrentes, "||")

    if corte is not None:
        imprimir_corte(resultado.evaluar_corte(corte))

    if resultado.advertencias:
        print("\nAdvertencias:")
        for advertencia in resultado.advertencias:
            print(f"  - {advertencia}")

    return resultado


class TestsAnalizadorEventosDistribuidos(unittest.TestCase):
    """Pruebas mínimas para asegurar el comportamiento esencial."""

    def test_matriz_del_enunciado_vectores_clave(self) -> None:
        resultado = AnalizadorEventosDistribuidos(EJEMPLO_ENUNCIADO).analizar()
        por_nombre = {evento.nombre: evento for evento in resultado.eventos}

        self.assertEqual(por_nombre["e1,1"].vector, (1, 0, 0))
        # NOTA: En el PowerPoint aparece 2,0,0 para esta casilla, pero si e-i-2-1 es
        # realmente la recepción de i-2-1, el reloj vectorial correcto es 2,1,0.
        self.assertEqual(por_nombre["e1,2"].vector, (2, 1, 0))
        self.assertEqual(por_nombre["e3,2"].vector, (1, 0, 2))
        self.assertEqual(por_nombre["e1,3"].vector, (3, 1, 3))

    def test_causalidad_y_concurrencia_basicas(self) -> None:
        matriz = [
            ["0", "i-1-2", "0", "e"],
            ["0", "e", "e-i-1-2", "0"],
        ]
        resultado = AnalizadorEventosDistribuidos(matriz).analizar()
        pares_causales = {(a.nombre, b.nombre) for a, b in resultado.relaciones_causales}
        pares_concurrentes = {frozenset((a.nombre, b.nombre)) for a, b in resultado.concurrentes}

        self.assertIn(("e1,1", "e2,2"), pares_causales)
        self.assertIn(frozenset(("e1,1", "e2,1")), pares_concurrentes)

    def test_mensajes_mx_generan_recepcion_en_t_mas_1(self) -> None:
        matriz = [
            ["0", "m2", "0"],
            ["0", "0", "0"],
        ]
        resultado = AnalizadorEventosDistribuidos(matriz).analizar()
        por_nombre = {evento.nombre: evento for evento in resultado.eventos}

        self.assertEqual(len(resultado.mensajes), 1)
        self.assertEqual(por_nombre["e1,1"].vector, (1, 0))
        self.assertEqual(por_nombre["e2,1"].vector, (1, 1))

    def test_corte_consistente(self) -> None:
        resultado = AnalizadorEventosDistribuidos(EJEMPLO_ENUNCIADO).analizar()
        corte = resultado.evaluar_corte(7)
        self.assertTrue(corte.consistente)
        self.assertEqual(len(corte.eventos_no_ejecutados), 0)


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        description="Analiza matrices de eventos distribuidos con relojes vectoriales."
    )
    parser.add_argument(
        "--archivo",
        type=Path,
        help="Ruta a un JSON o CSV con la matriz de entrada. Si se omite, se usa el ejemplo del enunciado.",
    )
    parser.add_argument(
        "--corte",
        type=int,
        default=7,
        help="Instante T para evaluar el corte consistente. Por defecto: 7.",
    )
    parser.add_argument(
        "--sin-corte",
        action="store_true",
        help="No evaluar corte consistente.",
    )
    parser.add_argument(
        "--test",
        action="store_true",
        help="Ejecuta los tests incluidos y termina.",
    )
    args = parser.parse_args(argv)

    if args.test:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(TestsAnalizadorEventosDistribuidos)
        resultado_tests = unittest.TextTestRunner(verbosity=2).run(suite)
        return 0 if resultado_tests.wasSuccessful() else 1

    matriz = cargar_matriz_desde_archivo(args.archivo) if args.archivo else EJEMPLO_ENUNCIADO
    corte = None if args.sin_corte else args.corte
    ejecutar(matriz, corte)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ErrorMatrizEventos as exc:
        print(f"Error en la matriz: {exc}", file=sys.stderr)
        raise SystemExit(2)
