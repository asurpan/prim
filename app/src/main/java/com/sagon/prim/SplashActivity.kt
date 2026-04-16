package com.sagon.prim

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView // Importar TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 3000 // 3 segundos

    // Lista de frases para la pantalla de bienvenida
    private val splashMessages = listOf(
        "¡El sueño de la Primitiva está a tu alcance!",
        "Tus números, tu suerte, tu futuro.",
        "Un boleto, una esperanza, un gran premio.",
        "Prepárate para la combinación ganadora.",
        "La Primitiva: Donde los sueños se hacen realidad.",
        "Cada número cuenta. ¡La suerte te espera!",
        "Descubre tu próxima gran victoria."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_splash)

        // Seleccionar un mensaje aleatorio de la lista
        val randomMessage = splashMessages.random()

        // Asignar el mensaje al TextView de la frase principal (el de textSize="24sp")
        val mainPhraseTextView: TextView = findViewById(R.id.mainPhraseTextView) // Asegúrate de que este ID exista en activity_splash.xml
        mainPhraseTextView.text = randomMessage

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_TIME_OUT)
    }
}