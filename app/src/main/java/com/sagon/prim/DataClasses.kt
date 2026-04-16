package com.sagon.prim

/**
 * MODELOS DE DATOS CENTRALIZADOS - LUXE EDITION
 */

/**
 * Representa el resultado oficial capturado o histórico.
 * @param date Fecha del sorteo en formato legible.
 * @param numbers Lista de los 6 números principales.
 * @param reintegro Número de reintegro (0-9).
 * @param complementario Número complementario.
 * @param frequent Lista de números con mayor aparición histórica (Estadística de Oro).
 * @param isLive Indica si los datos vienen de la web en tiempo real.
 */
data class LotteryResult(
    val date: String,
    val numbers: List<Int>,
    val reintegro: Int,
    val complementario: Int,
    val frequent: List<Int>,
    val isLive: Boolean
)

/**
 * Representa una jugada guardada por el usuario o generada por la Estrategia Maestra.
 * @param id Identificador único (timestamp).
 * @param name Nombre personalizado de la jugada.
 * @param numbers Los 6 números elegidos.
 * @param reintegro El reintegro elegido.
 * @param type Categoría: "ESTRATEGIA" o "MANUAL".
 */
data class Combination(
    val id: Long,
    val name: String,
    val numbers: List<Int>,
    val reintegro: Int,
    val type: String
)

/**
 * Estados de la UI para el flujo de datos.
 */
sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val data: LotteryResult) : MainUiState()
    data class Error(val message: String) : MainUiState()
}
