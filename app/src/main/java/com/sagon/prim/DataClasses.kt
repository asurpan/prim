package com.sagon.prim

import androidx.compose.runtime.Immutable

@Immutable
data class LotteryResult(
    val date: String,
    val numbers: List<Int>,
    val reintegro: Int,
    val complementario: Int,
    val frequent: List<Int>,
    val isLive: Boolean
)

@Immutable
data class Combination(
    val id: Long,
    val name: String,
    val numbers: List<Int>,
    val reintegro: Int,
    val type: String,
    val date: String = ""
)

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(val data: LotteryResult) : MainUiState()
    data class Error(val message: String) : MainUiState()
}