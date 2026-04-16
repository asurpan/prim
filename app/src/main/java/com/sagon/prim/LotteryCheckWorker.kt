package com.sagon.prim

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.*

class LotteryCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val CHANNEL_ID = "LotteryNotificationChannel"
    private val NOTIFICATION_ID = 1

    override suspend fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("prim_luxe_prefs", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Sorteos: Lunes (2), Jueves (5), Sábado (7)
        val isDrawDay = currentDayOfWeek == Calendar.MONDAY || 
                        currentDayOfWeek == Calendar.THURSDAY || 
                        currentDayOfWeek == Calendar.SATURDAY

        if (!isDrawDay) return Result.success()

        try {
            val url = "https://www.combinacionganadora.com/primitiva/"
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .get()

            val result = extractResult(doc)

            if (result == null || result.numbers.isEmpty()) {
                return Result.failure()
            }

            val frequent = fetchLiveFrequencies()
            val finalResult = if (frequent.isNotEmpty()) result.copy(frequent = frequent) else result

            sharedPreferences.edit().putString("last_lottery_result", Gson().toJson(finalResult)).apply()

            val savedCombinations = loadCombinations(applicationContext)
            val aciertosMap = mutableMapOf<String, Int>()

            savedCombinations.forEach { combination ->
                val hits = combination.numbers.intersect(result.numbers.toSet()).size
                if (hits >= 3) {
                    aciertosMap[combination.name] = hits
                }
            }

            if (aciertosMap.isNotEmpty()) {
                sendNotification(aciertosMap)
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("PRIM_WORKER", "Error: ${e.message}")
            return Result.failure()
        }
    }

    private fun extractResult(doc: Document): LotteryResult? {
        // Buscamos específicamente el bloque del último sorteo
        val summary = doc.select(".gameSummaryBlock").firstOrNull() ?: doc
        
        var dateText = summary.select("span.fdPrimitiva").firstOrNull()?.text() 
            ?: summary.select("h2").firstOrNull { it.text().contains("sorteo", true) || it.text().contains("premios", true) }?.text()
            ?: "ÚLTIMO SORTEO"
        
        // Limpieza de prefijos para evitar fechas de "archivo"
        if (dateText.contains("premios de", ignoreCase = true)) {
            dateText = dateText.split(Regex("premios de", RegexOption.IGNORE_CASE)).last().trim()
        }
        
        val allBalls = summary.select(".numbers.sexy li")
        val mainNums = allBalls.filter { ball -> !ball.hasAttr("data-extra") }
            .mapNotNull { ball -> 
                ball.text().trim().filter { char -> char.isDigit() }.toIntOrNull() 
            }
        
        val complementario = allBalls.firstOrNull { ball -> 
            ball.attr("data-extra") == "C" || ball.text().contains("C") 
        }?.let { ball ->
            ball.text().trim().filter { char -> char.isDigit() }.toIntOrNull() 
        } ?: 0
        
        val reintegro = allBalls.firstOrNull { ball -> 
            ball.attr("data-extra") == "R" || ball.text().contains("R") 
        }?.let { ball ->
            ball.text().trim().filter { char -> char.isDigit() }.toIntOrNull()
        } ?: 0

        return if (mainNums.size >= 6) {
            LotteryResult(dateText.uppercase(), mainNums.take(6).sorted(), reintegro, complementario, emptyList(), true)
        } else null
    }

    private fun fetchLiveFrequencies(): List<Int> {
        return try {
            val doc = Jsoup.connect("https://www.combinacionganadora.com/primitiva/estadisticas/")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000).get()
            doc.select(".table-stats td:first-child, .stat-number, .bola")
                .mapNotNull { it.text().trim().toIntOrNull() }
                .filter { it in 1..49 }.distinct().take(18)
        } catch (e: Exception) { emptyList() }
    }

    private fun loadCombinations(context: Context): List<Combination> {
        val prefs = context.getSharedPreferences("prim_luxe_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("saved_combis", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Combination>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sendNotification(aciertosMap: Map<String, Int>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Aciertos Primitiva", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val content = aciertosMap.entries.joinToString("\n") { "${it.key}: ¡${it.value} aciertos!" }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Resultados Primitiva!")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
