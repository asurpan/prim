package com.sagon.prim

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LotteryCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val CHANNEL_ID = "LotteryNotificationChannel"
    private val NOTIFICATION_ID = 1

    override suspend fun doWork(): Result {
        val sharedPreferences = applicationContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)

        // Obtener preferencias de días de notificación
        val notifyMonday = sharedPreferences.getBoolean("notify_monday", true)
        val notifyThursday = sharedPreferences.getBoolean("notify_thursday", true)
        val notifySaturday = sharedPreferences.getBoolean("notify_saturday", true)

        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Domingo=1, Lunes=2, ..., Sábado=7

        // Determinar si hoy es un día de sorteo seleccionado para notificar
        val shouldNotifyToday = when (currentDayOfWeek) {
            Calendar.MONDAY -> notifyMonday
            Calendar.THURSDAY -> notifyThursday
            Calendar.SATURDAY -> notifySaturday
            else -> false // No es un día de sorteo de Primitiva
        }

        // Solo continuar si las notificaciones están habilitadas Y es un día de sorteo seleccionado
        if (!notificationsEnabled || !shouldNotifyToday) {
            return Result.success()
        }

        try {
            val html = fetchHtml("https://www.elmundo.es/loterias/primitiva.html")
            val winningNumbers = extractWinningNumbers(html)

            if (winningNumbers.isEmpty()) {
                // Si no hay números ganadores, no se puede verificar.
                return Result.failure()
            }

            val savedCombinations = loadCombinations(applicationContext)
            val aciertosMap = mutableMapOf<String, Int>()

            savedCombinations.forEach { combination ->
                val userNumbers = combination.numbers.split(", ").mapNotNull { it.trim().toIntOrNull() }.toSet()
                val hits = userNumbers.intersect(winningNumbers.toSet()).size
                aciertosMap[combination.name] = hits
            }

            sendNotification(aciertosMap)

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun fetchHtml(url: String): String {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        return response.body?.string() ?: ""
    }

    private fun extractWinningNumbers(html: String): List<Int> {
        val doc = Jsoup.parse(html)
        val nums = doc.select(".ue-c-lottery-raffle__prices-number--highlighted")
            .mapNotNull { it.text().trim().toIntOrNull() }
        
        return if (nums.size >= 6) {
            nums.take(6).sorted()
        } else {
            emptyList()
        }
    }

    private fun loadCombinations(context: Context): List<LotteryCombination> {
        val sharedPreferences = context.getSharedPreferences("LotteryCombinations", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("saved_combinations", null)
        return if (json != null) {
            val type = object : TypeToken<List<LotteryCombination>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun sendNotification(aciertosMap: Map<String, Int>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificaciones de Aciertos de Primitiva",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre aciertos en tus combinaciones de la Primitiva."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationContent = StringBuilder()
        var hasRelevantHits = false

        for ((combinationName, hits) in aciertosMap) {
            if (hits >= 3) {
                notificationContent.append("$combinationName: ¡$hits aciertos!\n")
                hasRelevantHits = true
            }
        }

        if (hasRelevantHits) {
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("¡Resultados de la Primitiva!")
                .setContentText(notificationContent.toString().trim())
                .setStyle(NotificationCompat.BigTextStyle().bigText(notificationContent.toString().trim()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}