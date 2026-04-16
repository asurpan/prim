package com.sagon.prim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagon.prim.ui.theme.*
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * MAIN ACTIVITY - EMERALD LUXE EDITION
 * Refactorizada para eliminar términos técnicos y maximizar la elegancia visual.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PRIMTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val savedCombinations by viewModel.savedCombinations.collectAsState()
    val confettiTrigger by viewModel.confettiTrigger.collectAsState()
    val context = LocalContext.current

    var showManualDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val party = remember {
        Party(
            speed = 5f,
            maxSpeed = 40f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xFF10B981.toInt(), 0xFF34D399.toInt(), 0xFFFDE047.toInt()),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(100)
        )
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(DeepGreenGradient))) {
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "PRIMITIVA PRIM",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp
                                ),
                                color = EmeraldMain
                            )
                            if (uiState is MainUiState.Success) {
                                val state = uiState as MainUiState.Success
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (state.data.isLive) Color.Green else Color.Gray))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (state.data.isLive) "DATOS ACTUALIZADOS" else "MODO HISTÓRICO",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                                        color = if (state.data.isLive) EmeraldVibrant else TextGray
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncWithOfficialWeb() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Sincronizar", tint = EmeraldMain)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp), // Margen editorial Premium
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PANEL DE CONTROL SUPERIOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(text = "Manual", icon = Icons.Default.Edit, color = ActionBlue, modifier = Modifier.weight(1f)) { 
                        showManualDialog = true
                    }
                    ActionButton(text = "Compartir", icon = Icons.Default.Share, color = ActionGreen, modifier = Modifier.weight(1f)) {
                        val shareText = savedCombinations.joinToString("\n") { "${it.name}: ${it.numbers.joinToString("-")} R:${it.reintegro}" }
                        val intent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "Jugadas Maestras PRIM:\n$shareText"); type = "text/plain" }
                        context.startActivity(Intent.createChooser(intent, "Compartir Jugada"))
                    }
                    ActionButton(text = "Análisis", icon = Icons.AutoMirrored.Filled.List, color = ActionOrange, modifier = Modifier.weight(1f)) { 
                        showHistoryDialog = true
                    }
                    ActionButton(text = "Ajustes", icon = Icons.Default.Settings, color = ActionPurple, modifier = Modifier.weight(1f)) { 
                        showSettingsDialog = true
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SECCIÓN DE RESULTADOS (GLASSMORPHISM)
                when (val state = uiState) {
                    is MainUiState.Loading -> CircularProgressIndicator(color = EmeraldMain, modifier = Modifier.padding(32.dp))
                    is MainUiState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
                            border = BorderStroke(1.dp, BorderGlass)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ÚLTIMO SORTEO OFICIAL", color = GoldSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                                Text(state.data.date, color = TextGray, fontSize = 10.sp)
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    state.data.numbers.forEach { num ->
                                        NumberBall(number = num.toString(), type = "MAIN")
                                    }
                                    NumberBall(number = state.data.reintegro.toString(), type = "REINTEGRO")
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text("ESTADÍSTICA DE ORO (MÁS FRECUENTES)", color = EmeraldLight, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    state.data.frequent.take(8).forEach { num ->
                                        NumberBall(number = num.toString(), type = "SMALL")
                                    }
                                }
                            }
                        }
                    }
                    is MainUiState.Error -> Text(state.message, color = ErrorRed, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // BOTÓN DE ACCIÓN PRINCIPAL: ESTRATEGIA MAESTRA
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { viewModel.generateStrategyCombination() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(16.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(LuxeGreenGradient)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GENERAR ESTRATEGIA MAESTRA", fontWeight = FontWeight.Black, color = EmeraldBlack, fontSize = 16.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Usamos los números que más aparecen + azar para completar tu suerte.",
                        color = EmeraldLight.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // LISTADO DE JUGADAS GUARDADAS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(savedCombinations) { item ->
                        CombinationCard(item, onDelete = { viewModel.deleteEntry(item.id) })
                    }
                }
            }
        }

        // --- DIÁLOGOS ---
        if (showManualDialog) {
            ManualSelectionDialog(
                onDismiss = { showManualDialog = false },
                onSave = { numbers, reintegro ->
                    viewModel.addFullManualEntry("JUGADA PROPIA", numbers, reintegro)
                    showManualDialog = false
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onClearAll = { 
                    viewModel.clearAllCombinations()
                    showSettingsDialog = false
                }
            )
        }

        if (showHistoryDialog) {
            HistoryDialog(onDismiss = { showHistoryDialog = false })
        }

        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSelectionDialog(onDismiss: () -> Unit, onSave: (List<Int>, Int) -> Unit) {
    var selectedNumbers by remember { mutableStateOf(setOf<Int>()) }
    var selectedReintegro by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        tonalElevation = 8.dp,
        title = { Text("TU COMBINACIÓN", color = GoldSoft, fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Elige 6 números:", color = MintWhite, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 7
                ) {
                    for (i in 1..49) {
                        val isSelected = selectedNumbers.contains(i)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EmeraldMain else Color.Transparent)
                                .border(1.dp, EmeraldMain.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    if (isSelected) selectedNumbers -= i
                                    else if (selectedNumbers.size < 6) selectedNumbers += i
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(i.toString(), color = if (isSelected) EmeraldBlack else MintWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Reintegro (0-9):", color = MintWhite, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..9) {
                        val isSelected = selectedReintegro == i
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFEF4444) else Color.Transparent)
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape)
                                .clickable { selectedReintegro = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(i.toString(), color = if (isSelected) Color.White else Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedNumbers.size == 6) onSave(selectedNumbers.toList().sorted(), selectedReintegro) },
                enabled = selectedNumbers.size == 6,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
            ) {
                Text("GUARDAR JUGADA", color = EmeraldBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = TextGray)
            }
        }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onClearAll: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        title = { Text("AJUSTES DE LUJO", color = GoldSoft, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Versión: 3.0 Emerald Luxe", color = TextGray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onClearAll,
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("BORRAR TODA LA LISTA", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR", color = EmeraldMain)
            }
        }
    )
}

@Composable
fun HistoryDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        title = { Text("ESTADÍSTICAS", color = GoldSoft, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("Próximamente: Gráficas de tendencias basadas en sorteos desde 1985.", color = MintWhite, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ENTENDIDO", color = EmeraldMain)
            }
        }
    )
}

@Composable
fun NumberBall(number: String, type: String = "MAIN") {
    val size = when(type) { "MAIN" -> 42.dp; "SMALL" -> 30.dp; else -> 42.dp }
    val bgColor = when(type) { 
        "REINTEGRO" -> Color(0xFFEF4444)
        "SMALL" -> Color.Transparent 
        else -> EmeraldMain 
    }
    val textColor = if (type == "SMALL") EmeraldMain else EmeraldBlack
    val border = if (type == "SMALL") BorderStroke(1.dp, EmeraldMain.copy(alpha = 0.4f)) else null

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = if (type == "SMALL") 11.sp else 16.sp
        )
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun CombinationCard(item: Combination, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, BorderGlass)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = GoldSoft, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.numbers.forEach { num ->
                        Text(num.toString().padStart(2, '0') + " ", color = MintWhite, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("R:${item.reintegro}", color = Color(0xFFF87171), fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            IconButton(onClick = onDelete) { 
                Icon(Icons.Default.Delete, contentDescription = null, tint = TextGray.copy(alpha = 0.4f)) 
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier, 
        horizontalArrangement = horizontalArrangement,
        maxItemsInEachRow = 8
    ) { content() }
}
