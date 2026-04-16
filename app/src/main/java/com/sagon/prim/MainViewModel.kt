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

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _savedCombinations = MutableStateFlow<List<Combination>>(loadSavedData())
    val savedCombinations: StateFlow<List<Combination>> = _savedCombinations.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()

    private var estadisticasReales: List<Int> = loadStatsFromPrefs()

    // User-Agent de alta fidelidad para evitar bloqueos
    private val agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    init {
        limpiarCacheObsoleta()
        loadLastResult()
        syncWithOfficialWeb()
    }

    private fun limpiarCacheObsoleta() {
        val cached = prefs.getString("last_lottery_result", null)
        if (cached != null && !cached.contains("202")) { 
            prefs.edit().remove("last_lottery_result").apply()
        }
    }

    private fun loadLastResult() {
        val json = prefs.getString("last_lottery_result", null)
        if (json != null) {
            try {
                val cached = gson.fromJson(json, LotteryResult::class.java)
                _uiState.value = MainUiState.Success(cached.copy(isLive = true))
                estadisticasReales = cached.frequent
            } catch (e: Exception) {
                Log.e("PRIM_LUXE", "Error carga")
            }
        }
    }

    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            try {
                _uiState.value = MainUiState.Loading
                val result = withContext(Dispatchers.IO) {
                    // INTENTO 1: WEB OFICIAL (Resultados generales)
                    fetchOfficial() 
                        ?: fetchOfficialSpecific() // INTENTO 2: WEB OFICIAL (Primitiva específica)
                        ?: fetchCombinacionGanadora() // INTENTO 3: RESPALDO ESTABLE
                }

                if (result != null) {
                    val stats = withContext(Dispatchers.IO) { fetchLiveFrequencies() }
                    val finalResult = result.copy(frequent = stats.ifEmpty { estadisticasReales })
                    estadisticasReales = finalResult.frequent
                    _uiState.value = MainUiState.Success(finalResult.copy(isLive = true))
                    persistResult(finalResult)
                    saveStatsToPrefs(finalResult.frequent)
                } else {
                    _uiState.value = MainUiState.Error("Servidores ocupados. Por favor, reintente en unos instantes.")
                }
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error("Sin conexión: Verifique su internet.")
            }
        }
    }

    private fun fetchOfficial(): LotteryResult? {
        return try {
            val url = "https://www.loteriasyapuestas.es/es/resultados"
            val doc = Jsoup.connect(url)
                .userAgent(agent)
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Cache-Control", "max-age=0")
                .timeout(15000)
                .get()
            
            val gameBlock = doc.select(".modulo-juego, .regionCuerpo").firstOrNull { it.text().contains("Primitiva", true) }
            val nums = gameBlock?.select(".bola.principal, .num")?.mapNotNull { it.text().trim().toIntOrNull() }?.distinct() ?: emptyList()

            if (nums.size >= 6) {
                val c = gameBlock?.select(".bola.complementario, .comp")?.firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val r = gameBlock?.select(".bola.reintegro, .reint")?.firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val d = gameBlock?.select(".fecha")?.firstOrNull()?.text() ?: "ÚLTIMO SORTEO"
                LotteryResult(d.uppercase(), nums.take(6).sorted(), r, c, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    private fun fetchOfficialSpecific(): LotteryResult? {
        return try {
            val url = "https://www.loteriasyapuestas.es/es/resultados/primitiva"
            val doc = Jsoup.connect(url).userAgent(agent).timeout(15000).get()
            val nums = doc.select(".bola.principal").mapNotNull { it.text().trim().toIntOrNull() }
            if (nums.size >= 6) {
                val c = doc.select(".bola.complementario").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0
                val r = doc.select(".bola.reintegro").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0
                val d = doc.select(".fechaSorteo").firstOrNull()?.text() ?: "PRIMITIVA"
                LotteryResult(d.uppercase(), nums.take(6).sorted(), r, c, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    private fun fetchCombinacionGanadora(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/").userAgent(agent).timeout(10000).get()
            val nums = doc.select(".numbers.sexy li:not([data-extra])").mapNotNull { it.text().trim().toIntOrNull() }
            val c = doc.select("li[data-extra='C']").firstOrNull()?.text()?.toIntOrNull() ?: 0
            val r = doc.select("li[data-extra='R']").firstOrNull()?.text()?.toIntOrNull() ?: 0
            val d = doc.select(".fdPrimitiva, h2").firstOrNull { it.text().contains("/") }?.text() ?: "SORTEO RECIENTE"
            if (nums.size >= 6) LotteryResult(d.uppercase(), nums.take(6).sorted(), r, c, emptyList(), true) else null
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
            val pool = if (estadisticasReales.size >= 12) estadisticasReales else listOf(38, 47, 39, 45, 3, 40)
            val nums = (pool.shuffled().take(4) + (1..49).filter { it !in pool }.shuffled().take(2)).sorted()
            val c = Combination(System.nanoTime(), "PREDICCIÓN MAESTRA", nums, Random.nextInt(0, 10), "COMBINACIÓN")
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
            withContext(Dispatchers.Main) { _confettiTrigger.value += 1 }
        }
    }

    fun guardarJugadaManual(nombre: String, n: List<Int>, r: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val c = Combination(System.nanoTime(), nombre.ifBlank { "MI JUGADA" }.uppercase(), n.sorted(), r, "MANUAL")
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
        val j = prefs.getString("historical_stats", null) ?: return listOf(38, 47, 39, 45, 3, 40)
        return try { gson.fromJson(j, object : TypeToken<List<Int>>() {}.type) } catch (e: Exception) { listOf(38, 47, 39, 45, 3, 40) }
    }

    private fun loadSavedData(): List<Combination> {
        val j = prefs.getString("saved_combis", null) ?: return emptyList()
        return try { gson.fromJson(j, object : TypeToken<List<Combination>>() {}.type) } catch (e: Exception) { emptyList() }
    }
}
