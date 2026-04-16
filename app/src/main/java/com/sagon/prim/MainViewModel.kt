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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.*
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("prim_luxe_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val defaultStats = listOf(3, 38, 39, 40, 45, 47, 1, 5, 11, 17, 22, 30)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _savedCombinations = MutableStateFlow<List<Combination>>(loadSavedData())
    val savedCombinations: StateFlow<List<Combination>> = _savedCombinations.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()

    private var estadisticasReales: List<Int> = loadStatsFromPrefs()

    private val agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    init {
        loadLastResult()
        syncWithOfficialWeb()
    }

    // Sistema de reintento robusto para asegurar conexión perfecta
    private suspend fun <T> retry(times: Int = 2, block: suspend () -> T?): T? {
        repeat(times) { i ->
            try {
                val result = block()
                if (result != null) return result
            } catch (e: Exception) {
                Log.w("PRIM_CONN", "Intento ${i + 1} fallido: ${e.message}")
            }
            if (i < times - 1) kotlinx.coroutines.delay(1500L * (i + 1))
        }
        return null
    }

    private fun loadLastResult() {
        val json = prefs.getString("last_lottery_result", null)
        if (json != null) {
            try {
                val cached = gson.fromJson(json, LotteryResult::class.java)
                _uiState.value = MainUiState.Success(cached.copy(isLive = true))
                estadisticasReales = ensureTwelve(cached.frequent)
            } catch (e: Exception) {
                Log.e("PRIM_LUXE", "Error carga")
            }
        }
    }

    private fun ensureTwelve(list: List<Int>): List<Int> {
        val result = list.distinct().filter { it in 1..49 }.toMutableList()
        if (result.size < 12) {
            defaultStats.forEach { if (!result.contains(it) && result.size < 12) result.add(it) }
        }
        return result.take(12)
    }

    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            try {
                if (_uiState.value !is MainUiState.Success) {
                    _uiState.value = MainUiState.Loading
                }
                
                val result = withContext(Dispatchers.IO) {
                    // Intento en cascada con reintentos individuales para máxima fiabilidad
                    retry { fetchOfficial() } 
                        ?: retry { fetchOfficialSpecific() } 
                        ?: retry { fetchCombinacionGanadora() }
                }

                if (result != null && result.numbers.size >= 6) {
                    val stats = withContext(Dispatchers.IO) { 
                        retry { fetchLiveFrequencies() } ?: emptyList() 
                    }
                    val validatedStats = ensureTwelve(if (stats.isEmpty()) estadisticasReales else stats)
                    val finalResult = result.copy(frequent = validatedStats)
                    
                    estadisticasReales = validatedStats
                    _uiState.value = MainUiState.Success(finalResult.copy(isLive = true))
                    persistResult(finalResult)
                    saveStatsToPrefs(validatedStats)
                } else {
                    _uiState.value = MainUiState.Error("Servidores saturados. Inténtelo de nuevo.")
                }
            } catch (e: Exception) {
                Log.e("PRIM_LUXE", "Sync falló: ${e.message}")
                _uiState.value = MainUiState.Error("Sin conexión o red inestable.")
            }
        }
    }

    private fun fetchOfficial(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.loteriasyapuestas.es/es/resultados").userAgent(agent).timeout(15000).get()
            val gameBlock = doc.select(".modulo-juego, .regionCuerpo").firstOrNull { it.text().contains("Primitiva", true) }
            val nums = gameBlock?.select(".bola.principal, .num")?.mapNotNull { it.text().trim().toIntOrNull() }?.distinct() ?: emptyList()
            if (nums.size >= 6) {
                val r = gameBlock?.select(".bola.reintegro, .reint")?.firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val d = gameBlock?.select(".fecha")?.firstOrNull()?.text() ?: "ÚLTIMO SORTEO"
                LotteryResult(d.uppercase(), nums.take(6).sorted(), r, 0, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    private fun fetchOfficialSpecific(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.loteriasyapuestas.es/es/resultados/primitiva").userAgent(agent).timeout(15000).get()
            val nums = doc.select(".bola.principal").mapNotNull { it.text().trim().toIntOrNull() }
            if (nums.size >= 6) {
                val r = doc.select(".bola.reintegro").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0
                val d = doc.select(".fechaSorteo").firstOrNull()?.text() ?: "PRIMITIVA"
                LotteryResult(d.uppercase(), nums.take(6).sorted(), r, 0, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    private fun fetchCombinacionGanadora(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/").userAgent(agent).timeout(10000).get()
            val nums = doc.select(".numbers.sexy li:not([data-extra])").mapNotNull { it.text().trim().toIntOrNull() }
            val r = doc.select("li[data-extra='R']").firstOrNull()?.text()?.toIntOrNull() ?: 0
            val d = doc.select(".fdPrimitiva, h2").firstOrNull { it.text().contains("/") }?.text() ?: "SORTEO RECIENTE"
            if (nums.size >= 6) LotteryResult(d.uppercase(), nums.take(6).sorted(), r, 0, emptyList(), true) else null
        } catch (e: Exception) { null }
    }

    private fun fetchLiveFrequencies(): List<Int> {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/estadisticas/").userAgent(agent).timeout(10000).get()
            doc.select(".bola").mapNotNull { it.text().trim().toIntOrNull() }.filter { it in 1..49 }.distinct().take(18)
        } catch (e: Exception) { emptyList() }
    }

    fun generarPrediccionMaestra() {
        viewModelScope.launch(Dispatchers.Default) {
            val pool = if (estadisticasReales.size >= 12) estadisticasReales else defaultStats
            val nums = (pool.shuffled().take(4) + (1..49).filter { it !in pool }.shuffled().take(2)).sorted()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val c = Combination(System.nanoTime(), "PREDICCIÓN MAESTRA", nums, Random.nextInt(0, 10), "COMBINACIÓN", sdf.format(Date()))
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
            withContext(Dispatchers.Main) { _confettiTrigger.value += 1 }
        }
    }

    fun guardarJugadaManual(nombre: String, n: List<Int>, r: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val c = Combination(System.nanoTime(), nombre.ifBlank { "MI JUGADA" }.uppercase(), n.sorted(), r, "MANUAL", sdf.format(Date()))
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
        }
    }

    fun deleteEntry(id: Long) {
        _savedCombinations.update { it.filter { it.id != id } }
        persistToDisk(_savedCombinations.value)
    }

    private fun persistToDisk(l: List<Combination>) = prefs.edit().putString("saved_combis", gson.toJson(l)).apply()
    private fun persistResult(r: LotteryResult) = prefs.edit().putString("last_lottery_result", gson.toJson(r)).apply()
    private fun saveStatsToPrefs(s: List<Int>) = prefs.edit().putString("historical_stats", gson.toJson(s)).apply()

    private fun loadStatsFromPrefs(): List<Int> {
        val j = prefs.getString("historical_stats", null) ?: return defaultStats
        return try { gson.fromJson(j, object : TypeToken<List<Int>>() {}.type) } catch (e: Exception) { defaultStats }
    }

    private fun loadSavedData(): List<Combination> {
        val j = prefs.getString("saved_combis", null) ?: return emptyList()
        return try { gson.fromJson(j, object : TypeToken<List<Combination>>() {}.type) } catch (e: Exception) { emptyList() }
    }
}