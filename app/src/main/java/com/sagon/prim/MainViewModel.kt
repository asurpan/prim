import android.util.Log
import androidx.compose.ui.graphics.colorspace.connect
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.filter
import androidx.slice.builders.header
import androidx.wear.compose.foundation.size

/**
 * FUENTE 1: WEB OFICIAL (Loterías y Apuestas del Estado)
 * Implementación con Doble Petición (Session & Cookie) para Bypass de Akamai.
 */
private fun fetchFromOfficialWeb(): LotteryResult? {
    return try {
        val url = "https://www.loteriasyapuestas.es/es/resultados/primitiva"
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        // Paso 1: Obtener cookies de sesión iniciales
        val initialResponse = Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(10000)
            .execute()

        val cookies = initialResponse.cookies()

        // Paso 2: Petición con cookies y cabeceras de navegación real
        val doc = Jsoup.connect(url)
            .userAgent(userAgent)
            .cookies(cookies)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Referer", "https://www.loteriasyapuestas.es/es/resultados")
            .timeout(20000)
            .get()

        // Selectores precisos para la estructura oficial de SELAE
        val bolas = doc.select(".cuerpoRegionDerecha .bola, .regionJuego .bola, .combinacionGanadora .bola")
        val nums = bolas.take(6).mapNotNull { it.text().trim().toIntOrNull() }

        if (nums.size == 6) {
            val reintegro = doc.select(".reintegro .bola, .bola.especial").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val complementario = doc.select(".complementario .bola, .bola.c").firstOrNull()?.text()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val fecha = doc.select(".fechaSorteo, .breadcrumb .active").firstOrNull()?.text() ?: "ÚLTIMO SORTEO"

            return LotteryResult(
                date = "OFICIAL: $fecha",
                numbers = nums.sorted(),
                reintegro = reintegro,
                complementario = complementario,
                frequent = goldStats,
                isLive = true
            )
        }
        null
    } catch (e: Exception) {
        Log.e("PRIM_LUXE", "Official Cookie Bypass Error: ${e.message}")
        null
    }
}