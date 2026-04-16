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
 * VIEWMODEL SENIOR - ARQUITECTURA EMERALD LUXE
 * Gestión de estado reactivo, persistencia de caché y scraping de triple redundancia.
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

    // Estadísticas históricas para "Estadística de Oro"
    private val goldStats = listOf(38, 47, 39, 45, 3, 40, 30, 1, 11, 22, 17, 5, 23, 28, 32, 42, 14, 21)

    init {
        loadLastResult()
        syncWithOfficialWeb()
    }

    private fun loadLastResult() {
        val json = prefs.getString("last_lottery_result", null)
        if (json != null) {
            try {
                val cached = gson.fromJson(json, LotteryResult::class.java)
                _uiState.value = MainUiState.Success(cached.copy(isLive = false))
            } catch (e: Exception) {
                Log.e("PRIM_LUXE", "Error loading cache: ${e.message}")
            }
        }
    }

    /**
     * Sincronización con bypass de bot-detection y redundancia multi-fuente.
     */
    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            if (_uiState.value !is MainUiState.Success) {
                _uiState.value = MainUiState.Loading
            }
            
            try {
                val data = withContext(Dispatchers.IO) {
                    fetchSource1() ?: fetchSource2() ?: fetchSource3()
                }
                
                if (data != null) {
                    _uiState.value = MainUiState.Success(data)
                    prefs.edit().putString("last_lottery_result", gson.toJson(data)).apply()
                } else {
                    throw Exception("Fuentes no disponibles")
                }
            } catch (e: Exception) {
                Log.e("PRIM_LUXE", "Sync failed: ${e.message}")
                val currentState = _uiState.value
                if (currentState is MainUiState.Success) {
                    // Si ya teníamos datos (de caché o sync anterior), marcamos como offline pero mantenemos los datos
                    _uiState.value = MainUiState.Success(currentState.data.copy(isLive = false))
                } else {
                    // Si no había nada, mostramos el modo offline con datos por defecto
                    _uiState.value = MainUiState.Success(
                        LotteryResult(
                            date = "MODO OFFLINE",
                            numbers = listOf(5, 12, 28, 33, 40, 48),
                            reintegro = 9,
                            complementario = 14,
                            frequent = goldStats,
                            isLive = false
                        )
                    )
                }
            }
        }
    }

    private fun fetchSource1(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.resultadoprimitiva.es/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Connection", "keep-alive")
                .timeout(20000).get()
            
            val nums = doc.select(".bola_peque").mapNotNull { it.text().trim().toIntOrNull() }.take(6)
            if (nums.size < 6) return null
            
            LotteryResult(
                date = doc.select(".fecha_sorteo").firstOrNull()?.text() ?: "RECIENTE",
                numbers = nums.sorted(),
                reintegro = doc.select(".bola_reintegro").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0,
                complementario = doc.select(".bola_complemento").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0,
                frequent = goldStats,
                isLive = true
            )
        } catch (e: Exception) { 
            Log.e("PRIM_LUXE", "Source 1 error: ${e.message}")
            null 
        }
    }

    private fun fetchSource2(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .timeout(20000).get()
            
            val nums = doc.select(".ganadora .bola").mapNotNull { it.text().trim().toIntOrNull() }.take(6)
            if (nums.size < 6) return null
            
            LotteryResult(
                date = doc.select(".fecha").firstOrNull()?.text() ?: "ACTUALIZADO",
                numbers = nums.sorted(),
                reintegro = doc.select(".reintegro .bola").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0,
                complementario = doc.select(".complementario .bola").firstOrNull()?.text()?.trim()?.toIntOrNull() ?: 0,
                frequent = goldStats,
                isLive = true
            )
        } catch (e: Exception) { 
            Log.e("PRIM_LUXE", "Source 2 error: ${e.message}")
            null 
        }
    }

    private fun fetchSource3(): LotteryResult? {
        return try {
            val doc = Jsoup.connect("https://elpais.com/loterias/primitiva/")
                .userAgent("Mozilla/5.0").timeout(10000).get()
            val nums = doc.select(".num").mapNotNull { it.text().trim().toIntOrNull() }.take(6)
            if (nums.size < 6) return null
            LotteryResult("SORTEO EL PAÍS", nums.sorted(), 0, 0, goldStats, true)
        } catch (e: Exception) { null }
    }

    fun generateRandomCombination() {
        val set = mutableSetOf<Int>()
        while (set.size < 6) { set.add(Random.nextInt(1, 50)) }
        val newCombi = Combination(
            id = System.currentTimeMillis(),
            name = "COMBINACIÓN ALEATORIA",
            numbers = set.toList().sorted(),
            reintegro = Random.nextInt(0, 10),
            type = "ALEATORIA"
        )
        saveNewCombination(newCombi)
        _confettiTrigger.value += 1
    }

    fun addManualEntry(name: String, numbers: List<Int>, reintegro: Int) {
        val newCombi = Combination(
            id = System.currentTimeMillis(),
            name = name.ifBlank { "JUGADA MANUAL" },
            numbers = numbers.sorted(),
            reintegro = reintegro,
            type = "MANUAL"
        )
        saveNewCombination(newCombi)
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

    private fun saveNewCombination(combi: Combination) {
        val updated = (listOf(combi) + _savedCombinations.value).take(50)
        _savedCombinations.value = updated
        persistToDisk(updated)
    }

    private fun persistToDisk(list: List<Combination>) {
        prefs.edit().putString("saved_combis", gson.toJson(list)).apply()
    }

    private fun loadSavedData(): List<Combination> {
        val json = prefs.getString("saved_combis", null) ?: return emptyList()
        val type = object : TypeToken<List<Combination>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }
}
