package com.sagon.prim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- IDENTIDAD VISUAL EMERALD LUXE ---
val EmeraldMain = Color(0xFF10B981)
val EmeraldSurface = Color(0xFF064E3B)
val GoldLuxe = Color(0xFFFDE047)
val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF022C22))
val LuxeGreenGradient = listOf(Color(0xFF10B981), Color(0xFF059669))
val GlassBorder = Color(0xFF10B981).copy(alpha = 0.2f)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = EmeraldMain,
                    surface = EmeraldSurface,
                    onSurface = Color.White
                )
            ) {
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showManualDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Configuración de Confetti Premium
    val party = remember {
        Party(
            speed = 5f, 
            maxSpeed = 30f, 
            damping = 0.9f, 
            spread = 360,
            colors = listOf(0xFF10B981.toInt(), 0xFFFDE047.toInt()),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(100)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(DeepGreenGradient))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "PRIMITIVA PRIM",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    fontSize = 22.sp
                                ),
                                color = EmeraldMain
                            )
                            
                            if (uiState is MainUiState.Success) {
                                val state = uiState as MainUiState.Success
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    val statusColor by animateColorAsState(
                                        if (state.data.isLive) EmeraldMain else Color.Gray,
                                        label = "statusColor"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                            .shadow(if (state.data.isLive) 8.dp else 0.dp, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.data.isLive) "CONEXIÓN ESTABLE" else "MODO OFFLINE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = statusColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncWithOfficialWeb() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Sincronizar", tint = EmeraldMain)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = EmeraldMain
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // --- CUADRÍCULA DE ACCIONES - 4 BOTONES ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ActionButton(
                                text = "Elegir", 
                                icon = Icons.Rounded.Edit, 
                                color = Color(0xFF60A5FA), 
                                modifier = Modifier.weight(1f)
                            ) { showManualDialog = true }

                            ActionButton(
                                text = "Historial", 
                                icon = Icons.Rounded.List, 
                                color = Color(0xFFFBBF24), 
                                modifier = Modifier.weight(1f)
                            ) {
                                scope.launch {
                                    listState.animateScrollToItem(index = 3)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ActionButton(
                                text = "Ajustes", 
                                icon = Icons.Rounded.Settings, 
                                color = Color(0xFFA78BFA), 
                                modifier = Modifier.weight(1f)
                            ) { showSettingsDialog = true }

                            ActionButton(
                                text = "Enviar", 
                                icon = Icons.Rounded.Share, 
                                color = Color(0xFF34D399), 
                                modifier = Modifier.weight(1f)
                            ) {
                                val header = "✨ PRIMITIVA PRIM - MIS JUGADAS ✨\n" +
                                             "--------------------------------\n"
                                val body = savedCombinations.joinToString("\n") { 
                                    "${it.name}: ${it.numbers.joinToString("-")} [R:${it.reintegro}]" 
                                }
                                val footer = "\n--------------------------------\nGenerado con Prim Luxe"
                                
                                if (body.isNotEmpty()) {
                                    val fullText = header + body + footer
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, fullText)
                                        type = "text/plain"
                                    }, "Compartir Jugadas"))
                                }
                            }
                        }
                    }
                }

                // --- PANEL DE RESULTADOS (GLASSMORPHISM) ---
                item {
                    when (val state = uiState) {
                        is MainUiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp), 
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = EmeraldMain, strokeWidth = 3.dp)
                            }
                        }
                        is MainUiState.Success -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(30.dp, RoundedCornerShape(32.dp), ambientColor = EmeraldMain, spotColor = EmeraldMain),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.7f)),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "ÚLTIMA COMBINACIÓN",
                                        color = GoldLuxe,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 3.sp
                                        )
                                    )
                                    Text(
                                        text = state.data.date.uppercase(),
                                        color = Color.White.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        state.data.numbers.forEach { NumberBall(it.toString(), "MAIN") }
                                        NumberBall(state.data.reintegro.toString(), "REINTEGRO")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(32.dp))
                                    
                                    Text(
                                        text = "ESTADÍSTICA DE ORO",
                                        color = EmeraldMain,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.5.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(), 
                                        horizontalArrangement = Arrangement.Center,
                                        maxItemsInEachRow = 6
                                    ) {
                                        state.data.frequent.take(12).forEach { num ->
                                            NumberBall(num.toString(), "SMALL")
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                        is MainUiState.Error -> {
                            Text("Error: ${state.message}", color = Color.Red, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }

                // --- BOTÓN GENERADOR PRINCIPAL ---
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "buttonScale")

                    Button(
                        onClick = { viewModel.generateRandomCombination() },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .scale(scale)
                            .shadow(24.dp, RoundedCornerShape(20.dp), ambientColor = EmeraldMain, spotColor = EmeraldMain),
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
                            Text(
                                text = "COMBINACIÓN ALEATORIA",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF022C22),
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    }
                }

                // --- HISTORIAL RECIENTE ---
                item {
                    Text(
                        text = "HISTORIAL RECIENTE",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(savedCombinations, key = { it.id }) { item ->
                    CombinationCard(item) { viewModel.deleteEntry(item.id) }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // --- DIÁLOGOS ---
        if (showManualDialog) {
            ManualSelectionDialog(
                onDismiss = { showManualDialog = false }, 
                onSave = { n, r -> viewModel.addManualEntry("JUGADA MANUAL", n, r); showManualDialog = false }
            )
        }
        
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }, 
                onClear = { viewModel.clearAll(); showSettingsDialog = false }
            )
        }

        if (confettiTrigger > 0) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party)
            )
        }
    }
}

@Composable
fun NumberBall(text: String, type: String) {
    val size = if (type == "SMALL") 34.dp else 44.dp
    val bgColor = when(type) { 
        "REINTEGRO" -> Color(0xFFEF4444)
        "SMALL" -> Color.Transparent 
        else -> EmeraldMain 
    }
    val textColor = if (type == "SMALL") EmeraldMain else Color(0xFF022C22)
    val borderStroke = if (type == "SMALL") BorderStroke(1.dp, GlassBorder) else null
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = if (type == "SMALL") 11.sp else 18.sp
            )
        )
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "actionScale")

    Card(
        modifier = modifier
            .height(68.dp)
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }, 
        shape = RoundedCornerShape(20.dp), 
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)), 
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), 
            horizontalAlignment = Alignment.CenterHorizontally, 
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    letterSpacing = 1.2.sp
                )
            )
        }
    }
}

@Composable
fun CombinationCard(item: Combination, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(), 
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.4f)), 
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name.uppercase(), 
                    color = GoldLuxe, 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.numbers.forEach { 
                        Text(
                            text = "${it.toString().padStart(2, '0')} ", 
                            color = Color.White, 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        ) 
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "R:${item.reintegro}", 
                        color = Color(0xFFF87171), 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
            IconButton(onClick = onDelete) { 
                Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = Color.White.copy(alpha = 0.25f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSelectionDialog(onDismiss: () -> Unit, onSave: (List<Int>, Int) -> Unit) {
    var selN by remember { mutableStateOf(setOf<Int>()) }
    var selR by remember { mutableStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldSurface,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 8.dp,
        title = {
            Text(
                text = "JUGADA MANUAL",
                color = GoldLuxe,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "NÚMEROS (6)", 
                    color = Color.White.copy(0.5f), 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(), 
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..49) {
                        val isSelected = selN.contains(i)
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EmeraldMain else Color.Transparent)
                                .border(1.dp, EmeraldMain.copy(0.3f), CircleShape)
                                .clickable { 
                                    if (isSelected) selN -= i 
                                    else if (selN.size < 6) selN += i 
                                }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = i.toString(), 
                                color = if (isSelected) Color(0xFF022C22) else Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "REINTEGRO", 
                    color = Color.White.copy(0.5f), 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..9) {
                        val isSelected = selR == i
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFEF4444) else Color.Transparent)
                                .border(1.dp, Color(0xFFEF4444).copy(0.3f), CircleShape)
                                .clickable { selR = i }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = i.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = { 
            Button(
                onClick = { onSave(selN.toList(), selR) }, 
                enabled = selN.size == 6,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain, contentColor = Color(0xFF022C22)),
                shape = RoundedCornerShape(12.dp)
            ) { 
                Text("GUARDAR JUGADA", fontWeight = FontWeight.Black) 
            } 
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = EmeraldMain.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onClear: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, 
        containerColor = EmeraldSurface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = "AJUSTES",
                color = GoldLuxe,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
            )
        },
        text = { 
            Column {
                Text(
                    text = "¿Deseas eliminar permanentemente todo el historial de jugadas?", 
                    color = Color.White.copy(0.8f), 
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onClear, 
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.Red.copy(0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) { 
                    Text("BORRAR TODO EL HISTORIAL", color = Color.Red, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) 
                } 
            }
        },
        confirmButton = { 
            TextButton(onClick = onDismiss) { 
                Text("CERRAR", color = EmeraldMain, fontWeight = FontWeight.Bold) 
            }
        }
    )
}
