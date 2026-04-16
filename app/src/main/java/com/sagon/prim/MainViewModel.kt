package com.sagon.prim

import android.app.Application
import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * VIEWMODEL LUXE EDITION - LÓGICA DE ESTRATEGIA BASADA EN FRECUENCIAS
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("prim_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _savedCombinations = MutableStateFlow<List<Combination>>(loadSavedData())
    val savedCombinations: StateFlow<List<Combination>> = _savedCombinations.asStateFlow()

    private val _confettiTrigger = MutableStateFlow(0)
    val confettiTrigger: StateFlow<Int> = _confettiTrigger.asStateFlow()

    // --- FRECUENCIA HISTÓRICA MAESTRA (OFICIAL 1985 - 2024) ---
    private val masterFrequency = listOf(38, 47, 39, 45, 3, 40, 30, 1, 11, 22, 17, 5, 23, 28, 32, 42, 14, 21)

    init {
        syncWithOfficialWeb()
    }

    fun syncWithOfficialWeb() {
        viewModelScope.launch {
            _uiState.value = MainUiState.Loading
            try {
                val data = withContext(Dispatchers.IO) {
                    val doc = Jsoup.connect("https://www.resultadoprimitiva.es/").get()
                    val numbersList = doc.select(".bola_peque").mapNotNull { it.text().toIntOrNull() }.take(6)
                    val r = doc.select(".bola_reintegro").firstOrNull()?.text()?.toIntOrNull() ?: 0
                    val c = doc.select(".bola_complemento").firstOrNull()?.text()?.toIntOrNull() ?: 0
                    val dateText = doc.select(".fecha_sorteo").firstOrNull()?.text() ?: "Último Sorteo"

                    LotteryResult(
                        date = dateText,
                        numbers = if (numbersList.size == 6) numbersList else masterFrequency.take(6),
                        reintegro = r,
                        complementario = c,
                        frequent = masterFrequency,
                        isLive = true
                    )
                }
                _uiState.value = MainUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = MainUiState.Success(
                    LotteryResult(
                        date = "Histórico (Modo Offline)",
                        numbers = listOf(3, 15, 22, 38, 41, 47),
                        reintegro = 7,
                        complementario = 12,
                        frequent = masterFrequency,
                        isLive = false
                    )
                )
            }
        }
    }

    /**
     * GENERAR JUGADA MAESTRA:
     * Usa 4 números del "Club del Oro" (más frecuentes) + 2 números al azar para cubrir sorpresas.
     */
    fun generateStrategyCombination() {
        val currentState = _uiState.value
        if (currentState is MainUiState.Success) {
            val strategySet = mutableSetOf<Int>()
            
            // Tomamos 4 números de la estadística de mayor aparición (Histórico + Hoy)
            val hotPool = (currentState.data.frequent + currentState.data.numbers).shuffled()
            strategySet.addAll(hotPool.take(4))
            
            // Completamos con 2 números de azar puro (1-49)
            while (strategySet.size < 6) {
                strategySet.add(Random.nextInt(1, 50))
            }
            
            val newCombi = Combination(
                id = System.currentTimeMillis(),
                name = "ESTRATEGIA ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                numbers = strategySet.toList().sorted(),
                reintegro = Random.nextInt(0, 10),
                type = "ESTRATEGIA"
            )
            saveNewCombination(newCombi)
            _confettiTrigger.value += 1
        }
    }

    fun addFullManualEntry(name: String, numbers: List<Int>, reintegro: Int) {
        val manual = Combination(
            id = System.currentTimeMillis(),
            name = name,
            numbers = numbers.sorted(),
            reintegro = reintegro,
            type = "MANUAL"
        )
        saveNewCombination(manual)
    }

    fun clearAllCombinations() {
        _savedCombinations.value = emptyList()
        persistToDisk(emptyList())
    }

    fun deleteEntry(id: Long) {
        val updated = _savedCombinations.value.filter { it.id != id }
        _savedCombinations.value = updated
        persistToDisk(updated)
    }

    private fun saveNewCombination(combi: Combination) {
        val updated = listOf(combi) + _savedCombinations.value
        _savedCombinations.value = updated
        persistToDisk(updated)
    }

    private fun persistToDisk(list: List<Combination>) {
        prefs.edit().putString("saved_combis", gson.toJson(list)).apply()
    }

    private fun loadSavedData(): List<Combination> {
        val json = prefs.getString("saved_combis", null) ?: return emptyList()
        val type = object : TypeToken<List<Combination>>() {}.type
        return gson.fromJson(json, type)
    }
}
