package com.sagon.prim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    // Referencia al ViewModel Senior
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

    // Configuración de Confetti Premium para celebraciones
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
                // ITEM 0: GRID DE ACCIONES (Elegir, Historial, Ajustes, Enviar)
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
                                    // Scroll automático al encabezado del historial (índice 3)
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
                                val body = savedCombinations.joinToString("\n") { 
                                    "${it.name}: ${it.numbers.joinToString("-")} [R:${it.reintegro}]" 
                                }
                                if (body.isNotEmpty()) {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "MIS JUGADAS PRIMITIVA:\n$body")
                                        type = "text/plain"
                                    }, "Compartir"))
                                }
                            }
                        }
                    }
                }

                // ITEM 1: PANEL DE RESULTADOS (Glassmorphism Luxe)
                item {
                    when (val state = uiState) {
                        is MainUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EmeraldMain)
                            }
                        }
                        is MainUiState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(30.dp, RoundedCornerShape(32.dp), spotColor = EmeraldMain),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldSurface.copy(alpha = 0.7f)),
                                border = BorderStroke(1.dp, GlassBorder)
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ÚLTIMA COMBINACIÓN", color = GoldLuxe, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 3.sp))
                                    Text(state.data.date.uppercase(), color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        state.data.numbers.forEach { NumberBall(it.toString(), "MAIN") }
                                        NumberBall(state.data.reintegro.toString(), "REINTEGRO")
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text("ESTADÍSTICA DE ORO", color = EmeraldMain, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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

                // ITEM 2: BOTÓN GENERADOR PRINCIPAL
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "buttonScale")

                    Button(
                        onClick = { viewModel.generateRandomCombination() },
                        interactionSource = interactionSource,
                        modifier = Modifier.fillMaxWidth().height(64.dp).scale(scale).shadow(24.dp, RoundedCornerShape(20.dp), spotColor = EmeraldMain),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("GENERAR JUGADA MAESTRA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }

                // ITEM 3: CABECERA HISTORIAL (Punto de Anclaje de Scroll)
                item {
                    Text(
                        text = "HISTORIAL DE JUGADAS",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 4.sp),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // ITEM 4+: LISTA DE JUGADAS GUARDADAS
                items(savedCombinations, key = { it.id }) { combination ->
                    CombinationItem(combination, onDelete = { viewModel.deleteEntry(it) })
                }
            }
        }

        if (confettiTrigger > 0) {
            KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))
        }

        if (showManualDialog) {
            ManualEntryDialog(
                onDismiss = { showManualDialog = false },
                onConfirm = { name, nums, r -> viewModel.addManualEntry(name, nums, r) }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onClearAll = { viewModel.clearAll() }
            )
        }
    }
}

// --- COMPONENTES UI LUXE ---

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = EmeraldSurface.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun NumberBall(number: String, type: String) {
    val size = if (type == "MAIN" || type == "REINTEGRO") 44.dp else 30.dp
    val fontSize = if (type == "MAIN" || type == "REINTEGRO") 18.sp else 12.sp
    val bgColor = if (type == "REINTEGRO" || type == "SMALL_R") Color(0xFFEF4444) else EmeraldMain
    
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(bgColor.copy(alpha = 0.8f), bgColor))).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

@Composable
fun CombinationItem(combination: Combination, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(combination.name, color = EmeraldMain, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    combination.numbers.forEach { NumberBall(it.toString(), "SMALL") }
                    NumberBall(combination.reintegro.toString(), "SMALL_R")
                }
            }
            IconButton(onClick = { onDelete(combination.id) }) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.White.copy(alpha = 0.2f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryDialog(onDismiss: () -> Unit, onConfirm: (String, List<Int>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var numsText by remember { mutableStateOf("") }
    var rText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ELEGIR JUGADA", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Apodo de la jugada") })
                OutlinedTextField(value = numsText, onValueChange = { numsText = it }, label = { Text("6 números (sep. comas)") })
                OutlinedTextField(value = rText, onValueChange = { rText = it }, label = { Text("Reintegro") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val nums = numsText.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (nums.size == 6) {
                    onConfirm(name, nums, rText.toIntOrNull() ?: 0)
                    onDismiss()
                }
            }) { Text("GUARDAR") }
        }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit, onClearAll: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AJUSTES", fontWeight = FontWeight.Black) },
        text = { Text("¿Deseas borrar todo el historial de jugadas?") },
        confirmButton = {
            Button(onClick = { onClearAll(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("BORRAR TODO")
            }
        }
    )
}
