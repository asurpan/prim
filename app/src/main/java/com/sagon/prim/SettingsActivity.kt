package com.sagon.prim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.random.Random

private object SettingsPalette {
    val EmeraldMain = Color(0xFF10B981)
    val EmeraldSurface = Color(0xFF064E3B)
    val GoldLuxe = Color(0xFFFDE047)
    val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF022C22))
}

class SettingsActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(primary = SettingsPalette.EmeraldMain, surface = SettingsPalette.EmeraldSurface, onSurface = Color.White)
            ) {
                SettingsScreen(onBack = { finish() }, onRequestPermission = { checkAndRequestNotificationPermission() })
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
    val gson = Gson()

    var numCombinations by remember { mutableStateOf(sharedPrefs.getInt("num_extra_gen", 0).toFloat()) }
    var notificationsEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("notifications_enabled", true)) }
    var notifyMonday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_monday", true)) }
    var notifyThursday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_thursday", true)) }
    var notifySaturday by remember { mutableStateOf(sharedPrefs.getBoolean("notify_saturday", true)) }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(SettingsPalette.DeepGreenGradient))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CONFIGURACIÓN", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp), color = SettingsPalette.EmeraldMain)
                            Text("SISTEMA DE PREDICCIÓN V17", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp), color = Color.White.copy(0.4f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = SettingsPalette.EmeraldMain) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                SettingsSection(title = "INTELIGENCIA DE CÁLCULO", icon = Icons.Rounded.AutoAwesome) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Jugadas extra inmediatas", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(numCombinations.toInt().toString(), color = SettingsPalette.GoldLuxe, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Slider(value = numCombinations, onValueChange = { numCombinations = it }, valueRange = 0f..10f, steps = 9, colors = SliderDefaults.colors(thumbColor = SettingsPalette.EmeraldMain, activeTrackColor = SettingsPalette.EmeraldMain))
                        Text("Al guardar, se generarán estas jugadas y aparecerán en la pantalla principal al instante.", color = Color.White.copy(0.5f), fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }

                SettingsSection(title = "NOTIFICACIONES PUSH", icon = Icons.Rounded.NotificationsActive) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Alertas de Sorteo", color = Color.White, fontWeight = FontWeight.Bold)
                            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it; if (it) onRequestPermission() })
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
                        val newCount = numCombinations.toInt()
                        sharedPrefs.edit().apply {
                            putInt("num_extra_gen", newCount)
                            putBoolean("notifications_enabled", notificationsEnabled)
                            putBoolean("notify_monday", notifyMonday)
                            putBoolean("notify_thursday", notifyThursday)
                            putBoolean("notify_saturday", notifySaturday)
                            apply()
                        }

                        if (newCount > 0) {
                            val statsJson = sharedPrefs.getString("historical_stats", null)
                            val pool: List<Int> = try { gson.fromJson(statsJson, object : TypeToken<List<Int>>() {}.type) ?: listOf(38, 47, 39, 45, 3, 40) } catch (e: Exception) { listOf(38, 47, 39, 45, 3, 40) }

                            val existingJson = sharedPrefs.getString("saved_combis", null)
                            val list: MutableList<Combination> = try { gson.fromJson(existingJson, object : TypeToken<MutableList<Combination>>() {}.type) ?: mutableListOf() } catch (e: Exception) { mutableListOf() }

                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            val currentDate = sdf.format(Date())
                            repeat(newCount) { i ->
                                val nums = (pool.shuffled().take(4) + generateSequence { Random.nextInt(1, 50) }.filter { it !in pool }.take(2).toList()).sorted()
                                list.add(0, Combination(System.nanoTime() + i, "EXTRA (Ajustes)", nums, Random.nextInt(0, 10), "COMBINACIÓN", currentDate))
                            }
                            sharedPrefs.edit().putString("saved_combis", gson.toJson(list.take(100))).apply()
                        }
                        Toast.makeText(context, "Sincronización Completa", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsPalette.EmeraldMain)
                ) {
                    Icon(Icons.Rounded.Save, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("GUARDAR Y APLICAR", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)), border = BorderStroke(1.dp, Color.White.copy(0.08f))) {
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