package com.sagon.prim

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
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

    // ESCUCHADOR DE CAMBIOS EN DISCO
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "saved_combis") {
            _savedCombinations.value = loadSavedData()
        }
    }

    init {
        loadLastResult()
        syncWithOfficialWeb()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun loadLastResult() {
        val json = prefs.getString("last_lottery_result", null)
        if (json != null) {
            try {
                val cached = gson.fromJson(json, LotteryResult::class.java)
                _uiState.value = MainUiState.Success(cached.copy(isLive = true))
                estadisticasReales = cached.frequent
            } catch (e: Exception) {
                Log.e("PRIM_DEBUG", "Error caché: ${e.message}")
            }
        }
    }

    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val ultimoSorteo = fetchLastDraw()
                    val frecuenciasVivas = fetchLiveFrequencies()
                    ultimoSorteo?.copy(frequent = frecuenciasVivas.ifEmpty { estadisticasReales })
                }
                if (result != null) {
                    estadisticasReales = result.frequent
                    _uiState.value = MainUiState.Success(result.copy(isLive = true))
                    persistResult(result)
                    saveStatsToPrefs(result.frequent)
                }
            } catch (e: Exception) {
                Log.e("PRIM_DEBUG", "Error sync: ${e.message}")
            }
        }
    }

    private fun fetchLiveFrequencies(): List<Int> {
        return try {
            val doc = Jsoup.connect("https://www.loteriadehoy.com/primitiva/frecuencia-de-numeros/")
                .userAgent("Mozilla/5.0").timeout(15000).get()
            doc.select(".bola, .num, .numero").mapNotNull { it.text().toIntOrNull() }
                .filter { it in 1..49 }.distinct().take(18)
        } catch (e: Exception) { emptyList() }
    }

    private fun fetchLastDraw(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.loteriadehoy.com/primitiva/")
                .userAgent("Mozilla/5.0").timeout(15000).get()
            val nums = doc.select(".bola, .num").mapNotNull { it.text().toIntOrNull() }.filter { it in 1..49 }.distinct().take(6)
            if (nums.size == 6) {
                val r = doc.select(".reintegro").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                val c = doc.select(".complementario").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                LotteryResult("OFICIAL", nums.sorted(), r, c, emptyList(), true)
            } else null
        } catch (e: Exception) { null }
    }

    fun generarPrediccionMaestra() {
        viewModelScope.launch(Dispatchers.Default) {
            val numExtra = prefs.getInt("num_extra_gen", 0)
            val totalGeneraciones = 1 + numExtra
            val poolHistorico = if (estadisticasReales.size >= 12) estadisticasReales else estadisticasReales + listOf(38, 47, 39, 45, 3, 40)

            val nuevas = mutableListOf<Combination>()
            repeat(totalGeneraciones) { i ->
                val nums = (poolHistorico.shuffled().take(4) + generateSequence { Random.nextInt(1, 50) }.filter { it !in poolHistorico }.take(2).toList()).sorted()
                nuevas.add(Combination(id = System.nanoTime() + i, name = if (i == 0) "PREDICCIÓN MAESTRA" else "EXTRA LUXE", numbers = nums, reintegro = Random.nextInt(0, 10), type = "COMBINACIÓN"))
            }

            _savedCombinations.update { current ->
                val updated = (nuevas + current).take(100)
                persistToDisk(updated)
                updated
            }
            withContext(Dispatchers.Main) { _confettiTrigger.value += 1 }
        }
    }

    fun guardarJugadaManual(numeros: List<Int>, reintegro: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val newCombination = Combination(
                id = System.nanoTime(),
                name = "JUGADA PERSONAL",
                numbers = numeros.sorted(),
                reintegro = reintegro,
                type = "MANUAL"
            )
            _savedCombinations.update { current ->
                val updated = (listOf(newCombination) + current).take(100)
                persistToDisk(updated)
                updated
            }
        }
    }

    fun deleteEntry(id: Long) {
        _savedCombinations.update { current ->
            val updated = current.filter { it.id != id }
            persistToDisk(updated)
            updated
        }
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
        val json = prefs.getString("historical_stats", null) ?: return listOf(38, 47, 39, 45, 3, 40)
        return try { gson.fromJson(json, object : TypeToken<List<Int>>() {}.type) } catch (e: Exception) { listOf(38, 47, 39, 45, 3, 40) }
    }

    private fun loadSavedData(): List<Combination> {
        val json = prefs.getString("saved_combis", null) ?: return emptyList()
        return try { gson.fromJson(json, object : TypeToken<List<Combination>>() {}.type) } catch (e: Exception) { emptyList() }
    }
}
