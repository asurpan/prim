package com.sagon.prim

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.*
import kotlin.random.Random

/**
 * VIEWMODEL EMERALD TITAN V17 - DEEP ORACLE
 * Misión: Scraping dinámico de resultados Y estadísticas históricas reales.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("prim_luxe_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _savedCombinations = MutableStateFlow<List<Combination>>(loadSavedData())
    val savedCombinations: StateFlow<List<Combination>> = _savedCombinations.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()

    // Lista dinámica que se llenará con datos reales de la web
    private var liveHistoricalStats: List<Int> = loadStatsFromPrefs()

    init {
        loadLastResult()
        syncWithOfficialWeb()
    }

    private fun loadLastResult() {
        val json = prefs.getString("last_lottery_result", null)
        if (json != null) {
            try {
                val cached = gson.fromJson(json, LotteryResult::class.java)
                _uiState.value = MainUiState.Success(cached)
                liveHistoricalStats = cached.frequent
            } catch (e: Exception) {
                Log.e("PRIM_DEBUG", "Error persistencia: ${e.message}")
            }
        }
    }

    /**
     * Sincronización Maestra: Obtiene el sorteo y las estadísticas de frecuencia.
     */
    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            Log.d("PRIM_DEBUG", ">>> EJECUTANDO DEEP ORACLE V17: DATOS Y ESTADÍSTICAS REALES <<<")
            try {
                val result = withContext(Dispatchers.IO) {
                    val lastDraw = fetchLastDraw()
                    val stats = fetchLiveFrequencies()

                    if (lastDraw != null) {
                        lastDraw.copy(frequent = stats.ifEmpty { liveHistoricalStats })
                    } else null
                }

                if (result != null) {
                    liveHistoricalStats = result.frequent
                    _uiState.value = MainUiState.Success(result)
                    persistResult(result)
                    saveStatsToPrefs(result.frequent)
                } else {
                    handleSyncError("Error de sincronización")
                }
            } catch (e: Exception) {
                handleSyncError(e.message ?: "Error de red")
            }
        }
    }

    private fun handleSyncError(message: String) {
        val currentState = _uiState.value
        if (currentState is MainUiState.Success) {
            _uiState.value = MainUiState.Success(currentState.data.copy(isLive = false))
        } else {
            _uiState.value = MainUiState.Success(
                LotteryResult("MODO OFFLINE", listOf(1, 2, 3, 4, 5, 6), 0, 0, liveHistoricalStats, false)
            )
        }
    }

    // --- SCRAPING DINÁMICO DE ESTADÍSTICAS ---

    private fun fetchLiveFrequencies(): List<Int> {
        return try {
            // Navegamos a la sección específica de estadísticas de frecuencia
            val doc = Jsoup.connect("https://www.loteriadehoy.com/primitiva/frecuencia-de-numeros/")
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get()

            // Extraemos los números de la tabla de frecuencias (los que más aparecen)
            val stats = doc.select(".bola, .num_lot, .numero")
                .mapNotNull { it.text().toIntOrNull() }
                .filter { it in 1..49 }
                .distinct()
                .take(18) // Los 18 más frecuentes

            if (stats.size >= 6) {
                Log.d("PRIM_DEBUG", "Estadísticas actualizadas: $stats")
                stats
            } else emptyList()
        } catch (e: Exception) {
            Log.e("PRIM_DEBUG", "Error scraping stats: ${e.message}")
            emptyList()
        }
    }

    private fun fetchLastDraw(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.loteriadehoy.com/primitiva/")
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .get()

            val nums = doc.select(".bola, .num").mapNotNull { it.text().toIntOrNull() }.filter { it in 1..49 }.distinct().take(6)
            if (nums.size == 6) {
                val r = doc.select(".reintegro").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val c = doc.select(".complementario").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                LotteryResult("SORTEO OFICIAL", nums.sorted(), r, c, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    // --- ALGORITMO SMART-LUCK V17 ---

    fun generateSmartCombination() {
        val finalNumbers = mutableSetOf<Int>()
        val pool = if (liveHistoricalStats.size >= 6) liveHistoricalStats else listOf(38, 47, 39, 45, 3, 40)

        // Usamos la estadística REAL descargada
        val goldNumbers = pool.shuffled()
        finalNumbers.add(goldNumbers[0])
        finalNumbers.add(goldNumbers[1])
        finalNumbers.add(goldNumbers[2])

        while (finalNumbers.size < 6) {
            finalNumbers.add(Random.nextInt(1, 50))
        }

        val combination = Combination(
            id = System.currentTimeMillis(),
            name = "PREDICCIÓN REAL",
            numbers = finalNumbers.toList().sorted(),
            reintegro = Random.nextInt(0, 10),
            type = "SMART"
        )

        saveNewCombination(combination)
        _confettiTrigger.value += 1
    }

    // --- PERSISTENCIA LUXE ---

    private fun saveNewCombination(combi: Combination) {
        val updated = (listOf(combi) + _savedCombinations.value).take(50)
        _savedCombinations.value = updated
        persistToDisk(updated)
    }

    fun deleteEntry(id: Long) {
        val updated = _savedCombinations.value.filter { it.id != id }
        _savedCombinations.value = updated
        persistToDisk(updated)
    }

    fun clearAll() {
        _savedCombinations.value = emptyList()
        persistToDisk(emptyList())
    }

    private fun persistToDisk(list: List<Combination>) {
        prefs.edit().putString("saved_combis", gson.toJson(list)).apply()
    }

    private fun persistResult(result: LotteryResult) {
        prefs.edit().putString("last_lottery_result", gson.toJson(result)).apply()
    }

    private fun saveStatsToPrefs(stats: List<Int>) {
        prefs.edit().putString("historical_stats", gson.toJson(stats)).apply()
    }

    private fun loadStatsFromPrefs(): List<Int> {
        val json = prefs.getString("historical_stats", null) ?: return listOf(38, 47, 39, 45, 3, 40, 30, 1, 11, 22, 17, 5)
        return try { gson.fromJson(json, object : TypeToken<List<Int>>() {}.type) } catch (e: Exception) { emptyList() }
    }

    private fun loadSavedData(): List<Combination> {
        val json = prefs.getString("saved_combis", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<Combination>>() {}.type) } catch (e: Exception) { emptyList() }
    }
}