package com.sagon.prim

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object LotteryNotificationHelper { // Usamos 'object' para que sea un singleton y no necesitemos instanciarlo

    private const val CHANNEL_ID = "lottery_check_channel"
    private const val CHANNEL_NAME = "Resultados de la Primitiva"
    private const val NOTIFICATION_ID = 101

    fun showNotification(context: Context, message: String) {
        createNotificationChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener un icono en res/drawable
            .setContentTitle("¡Resultados de la Primitiva!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Para mostrar el mensaje completo
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Notificación importante

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context) {
        // Solo es necesario para Android 8.0 (API nivel 26) y superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones sobre los resultados de la Primitiva y tus aciertos."
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}