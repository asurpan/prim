package com.sagon.prim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.random.Random
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * PALETA DE COLORES LUXE V17
 * Encapsulada para evitar conflictos de declaración global
 */
private object SettingsPalette {
    val EmeraldMain = Color(0xFF10B981)
    val EmeraldSurface = Color(0xFF064E3B)
    val GoldLuxe = Color(0xFFFDE047)
    val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF022C22))
}

class SettingsActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notificaciones limitadas por el sistema", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SettingsPalette.EmeraldMain,
                    surface = SettingsPalette.EmeraldSurface,
                    onSurface = Color.White
                )
            ) {
                SettingsScreen(
                    onBack = { finish() },
                    onRequestPermission = { checkAndRequestNotificationPermission() }
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("prim_luxe_prefs", Context.MODE_PRIVATE) }

    var numCombinations by remember {
        mutableStateOf(sharedPrefs.getInt("num_extra_gen", 0).toFloat())
    }
    var notificationsEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true))
    }
    var notifyMonday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_monday", true)) }
    var notifyThursday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_thursday", true)) }
    var notifySaturday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_saturday", true)) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(SettingsPalette.DeepGreenGradient))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CONFIGURACIÓN LUXE", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp), color = SettingsPalette.EmeraldMain)
                            Text("SISTEMA DE PREDICCIÓN V17", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp), color = Color.White.copy(0.4f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = SettingsPalette.EmeraldMain)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsSection(title = "INTELIGENCIA DE CÁLCULO", icon = Icons.Rounded.AutoAwesome) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Combinaciones adicionales", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(numCombinations.toInt().toString(), color = SettingsPalette.GoldLuxe, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Slider(
                            value = numCombinations,
                            onValueChange = { numCombinations = it },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = SettingsPalette.EmeraldMain, activeTrackColor = SettingsPalette.EmeraldMain)
                        )
                        Text("Define cuántas opciones extra generará el sistema en cada predicción. Se guardarán todas en el historial.", color = Color.White.copy(0.5f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }

                SettingsSection(title = "NOTIFICACIONES PUSH", icon = Icons.Rounded.NotificationsActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Alertas de Sorteo", color = Color.White, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = {
                                    notificationsEnabled = it
                                    if (it) onRequestPermission()
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = SettingsPalette.EmeraldMain, checkedTrackColor = SettingsPalette.EmeraldMain.copy(0.4f))
                            )
                        }
                        if (notificationsEnabled) {
                            HorizontalDivider(color = Color.White.copy(0.05f))
                            LuxeDayRow("Lunes", notifyMonday) { notifyMonday = it }
                            LuxeDayRow("Jueves", notifyThursday) { notifyThursday = it }
                            LuxeDayRow("Sábado", notifySaturday) { notifySaturday = it }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val gson = Gson()
                        val currentNumExtra = sharedPrefs.getInt("num_extra_gen", 0)
                        val newNumExtra = numCombinations.toInt()

                        // 1. Guardar preferencias básicas
                        sharedPrefs.edit().apply {
                            putInt("num_extra_gen", newNumExtra)
                            putBoolean("notifications_enabled", notificationsEnabled)
                            putBoolean("notify_monday", notifyMonday)
                            putBoolean("notify_thursday", notifyThursday)
                            putBoolean("notify_saturday", notifySaturday)
                            apply()
                        }

                        // 2. Generar las combinaciones adicionales inmediatamente si el usuario lo solicita
                        // Cargamos estadísticas para la generación
                        val statsJson = sharedPrefs.getString("historical_stats", null)
                        val poolHistorico: List<Int> = try {
                            gson.fromJson(statsJson, object : TypeToken<List<Int>>() {}.type)
                                ?: listOf(38, 47, 39, 45, 3, 40)
                        } catch (e: Exception) {
                            listOf(38, 47, 39, 45, 3, 40)
                        }

                        val poolFinal = if (poolHistorico.size >= 12) poolHistorico 
                                       else poolHistorico + listOf(38, 47, 39, 45, 3, 40)

                        // Generamos solo si el número es mayor a 0
                        if (newNumExtra > 0) {
                            val existingJson = sharedPrefs.getString("saved_combis", null)
                            val currentList: MutableList<Combination> = try {
                                gson.fromJson(existingJson, object : TypeToken<MutableList<Combination>>() {}.type) 
                                    ?: mutableListOf()
                            } catch (e: Exception) {
                                mutableListOf()
                            }

                            val nuevas = mutableListOf<Combination>()
                            repeat(newNumExtra) { i ->
                                val nums = (poolFinal.shuffled().take(4) + 
                                           generateSequence { Random.nextInt(1, 50) }
                                           .filter { it !in poolFinal }.take(2).toList()).sorted()
                                
                                nuevas.add(Combination(
                                    id = System.nanoTime() + i,
                                    name = "EXTRA LUXE (Ajustes)",
                                    numbers = nums,
                                    reintegro = Random.nextInt(0, 10),
                                    type = "COMBINACIÓN"
                                ))
                            }

                            val updatedList = (nuevas + currentList).take(100)
                            sharedPrefs.edit().putString("saved_combis", gson.toJson(updatedList)).apply()
                        }

                        Toast.makeText(context, "Ajustes Sincronizados y Combinaciones Generadas", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsPalette.EmeraldMain)
                ) {
                    Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("GUARDAR Y APLICAR", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = SettingsPalette.EmeraldMain, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = SettingsPalette.EmeraldMain, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun LuxeDayRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(0.8f), fontSize = 14.sp)
        Checkbox(checked = checked, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = SettingsPalette.EmeraldMain))
    }
}