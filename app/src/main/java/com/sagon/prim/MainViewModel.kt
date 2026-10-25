package com.sagon.prim

import android.app.Application
import android.content.Context
import android.content.Intent
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

    private val prefs = application.getSharedPreferences("prim_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    // Top 20 números históricos reales de la Primitiva (basado en apariciones totales)
    private val defaultStats = listOf(43, 28, 33, 40, 7, 12, 22, 34, 11, 30, 31, 37, 38, 48, 6, 10, 16, 25, 41, 1)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _savedCombinations = MutableStateFlow<List<Combination>>(loadSavedData())
    val savedCombinations: StateFlow<List<Combination>> = _savedCombinations.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()

    private val _dailyLuck = MutableStateFlow(loadInitialLuck()) // Carga si ya se hizo hoy
    val dailyLuck: StateFlow<Int> = _dailyLuck.asStateFlow()

    private val _hasNewBoletos = MutableStateFlow(false)
    val hasNewBoletos: StateFlow<Boolean> = _hasNewBoletos.asStateFlow()

    private fun loadInitialLuck(): Int {
        val today = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("last_luck_date", "")
        return if (lastDate == today) calculateDailyLuck() else 0
    }

    private val _luckyNumber = MutableStateFlow(calculateDailyLuckyNumber())
    val luckyNumber: StateFlow<Int> = _luckyNumber.asStateFlow()

    private var estadisticasReales: List<Int> = loadStatsFromPrefs()

    fun revealLuck() {
        if (_dailyLuck.value == 0) {
            val luck = calculateDailyLuck()
            _dailyLuck.value = luck
            val today = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            prefs.edit().putString("last_luck_date", today).apply()
        }
    }

    fun getGuardianOfDay(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "SELENE (LUNA)"
            Calendar.TUESDAY -> "ARES (MARTE)"
            Calendar.WEDNESDAY -> "HERMES (MERCURIO)"
            Calendar.THURSDAY -> "ZEUS (JÚPITER)"
            Calendar.FRIDAY -> "AFRODITA (VENUS)"
            Calendar.SATURDAY -> "CRONOS (SATURNO)"
            else -> "APOLO (SOL)"
        }
    }

    fun getResonanceMessage(luck: Int, lastResult: LotteryResult?): String? {
        if (lastResult == null) return null
        val drawNumbers = lastResult.numbers
        val matches = drawNumbers.any { it == luck }
        return if (matches) "¡RESONANCIA DETECTADA! Tu energía vibra con el número $luck del sorteo." else null
    }

    private fun calculateDailyLuck(): Int {
        val today = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val deviceId = prefs.getString("unique_luck_id", null) ?: UUID.randomUUID().toString().also { 
            prefs.edit().putString("unique_luck_id", it).apply() 
        }
        val seed = (today.toLongOrNull() ?: 0L) + deviceId.hashCode().toLong()
        return Random(seed).nextInt(75, 100)
    }

    private fun calculateDailyLuckyNumber(): Int {
        val today = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        // Usamos una semilla diferente a la de la suerte diaria para que no coincidan
        val seed = (today.toLongOrNull() ?: 0L) + 98765L 
        return Random(seed).nextInt(1, 50)
    }

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
                Log.e("PRIM", "Error carga")
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
                Log.e("PRIM", "Sync falló: ${e.message}")
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
            val d = doc.select(".fdPrimitiva, h2").firstOrNull { it.text().contains("/") }?.text() ?: "ÚLTIMO SORTEO"
            if (nums.size >= 6) LotteryResult(d.uppercase(), nums.take(6).sorted(), r, 0, emptyList(), true) else null
        } catch (e: Exception) { null }
    }

    private fun fetchLiveFrequencies(): List<Int> {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/estadisticas/").userAgent(agent).timeout(10000).get()
            doc.select(".bola").mapNotNull { it.text().trim().toIntOrNull() }.filter { it in 1..49 }.distinct().take(18)
        } catch (e: Exception) { emptyList() }
    }

    fun getCosmicNumbers(count: Int, exclude: List<Int>): List<Int> {
        val today = java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val guardian = getGuardianOfDay()
        val seed = today.hashCode().toLong() xor guardian.hashCode().toLong()
        val random = Random(seed)
        
        return (1..49)
            .filter { it !in exclude }
            .shuffled(random)
            .take(count)
    }

    fun generarPrediccionMaestra() {
        viewModelScope.launch(Dispatchers.Default) {
            val pool = if (estadisticasReales.size >= 12) estadisticasReales else defaultStats
            val stars = pool.shuffled().take(4)
            val cosmos = getCosmicNumbers(2, stars)
            val nums = (stars + cosmos).sorted()
            
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val c = Combination(System.nanoTime(), "JUGADA MÁGICA", nums, Random.nextInt(0, 10), "COMBINACIÓN", sdf.format(Date()))
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
            _hasNewBoletos.value = true
            withContext(Dispatchers.Main) { _confettiTrigger.value += 1 }
        }
    }

    fun guardarJugadaManual(nombre: String, n: List<Int>, r: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val c = Combination(System.nanoTime(), nombre.ifBlank { "MI JUGADA" }.uppercase(), n.sorted(), r, "MANUAL", sdf.format(Date()))
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
            _hasNewBoletos.value = true
        }
    }

    fun deleteEntry(id: Long) {
        _savedCombinations.update { it.filter { it.id != id } }
        persistToDisk(_savedCombinations.value)
    }

    private fun isCombinationOptimal(numbers: List<Int>): Boolean {
        val evens = numbers.count { it % 2 == 0 }
        val sum = numbers.sum()
        
        val sumValid = sum in 115..185
        val balanceValid = evens in 2..4
        
        var consecutiveCount = 0
        for (i in 0 until numbers.size - 1) {
            if (numbers[i + 1] - numbers[i] == 1) {
                consecutiveCount++
                if (consecutiveCount >= 2) return false
            } else {
                consecutiveCount = 0
            }
        }
        
        val decades = numbers.map { (it - 1) / 10 }.toSet()
        val decadesValid = decades.size >= 3
        
        val endings = numbers.map { it % 10 }
        val maxEndingCount = endings.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
        val endingsValid = maxEndingCount <= 2

        return sumValid && balanceValid && decadesValid && endingsValid
    }

    fun generarCombinacionAvanzada() {
        viewModelScope.launch(Dispatchers.Default) {
            val random = Random.Default
            val pool = if (estadisticasReales.size >= 12) estadisticasReales else defaultStats
            val rest = (1..49).filter { it !in pool }
            
            var bestNumbers: List<Int> = emptyList()
            for (attempt in 1..3000) {
                // Basado 100% en la realidad estadística: 4 números del TOP histórico frecuente y 2 del resto
                val fromPool = pool.shuffled(random).take(4)
                val fromRest = rest.shuffled(random).take(2)
                val candidate = (fromPool + fromRest).sorted()
                
                if (isCombinationOptimal(candidate)) {
                    bestNumbers = candidate
                    break
                }
            }
            if (bestNumbers.isEmpty()) {
                bestNumbers = pool.shuffled(random).take(6).sorted()
            }
            
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val c = Combination(System.nanoTime(), "JUGADA ESTADÍSTICA", bestNumbers, Random.nextInt(0, 10), "AVANZADA", sdf.format(Date()))
            _savedCombinations.update { (listOf(c) + it).take(100) }
            persistToDisk(_savedCombinations.value)
            _hasNewBoletos.value = true
            withContext(Dispatchers.Main) { _confettiTrigger.value += 1 }
        }
    }

    fun clearNewBoletosNotification() {
        _hasNewBoletos.value = false
    }

    fun refreshSavedCombinations() {
        _savedCombinations.value = loadSavedData()
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