package com.sagon.prim

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- IDENTIDAD VISUAL EMERALD LUXE V17 (ESTRICTO) ---
val EmeraldMain = Color(0xFF10B981)
val EmeraldSurface = Color(0xFF064E3B)
val GoldLuxe = Color(0xFFFDE047)
val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF022C22))

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
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var showManualSelector by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // BLINDAJE BOTÓN ATRÁS
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    val party = remember {
        Party(
            speed = 10f, maxSpeed = 30f, damping = 0.9f, spread = 360,
            colors = listOf(0xFF10B981.toInt(), 0xFFFDE047.toInt()),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(100)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(DeepGreenGradient))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "PRIMITIVA PRIM",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp
                                ),
                                color = EmeraldMain
                            )
                            Text(
                                "SINCRO LUXE ACTIVA",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                                color = Color.White.copy(0.4f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncWithOfficialWeb() }) {
                            Icon(Icons.Rounded.Sync, null, tint = EmeraldMain)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // PANEL DE ACCIONES LUXE: FILA ÚNICA (OPTIMIZADA)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton("ELEGIR", Icons.Rounded.Edit, Color(0xFF60A5FA), Modifier.weight(1f)) {
                            showManualSelector = true
                        }
                        ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1f)) {
                            viewModel.generarPrediccionMaestra()
                        }
                        ActionButton("AJUSTES", Icons.Rounded.Settings, Color(0xFFA78BFA), Modifier.weight(1f)) {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    }
                }

                // RESULTADO EN VIVO (ULTIMO SORTEO)
                item {
                    when (val state = uiState) {
                        is MainUiState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(30.dp, spotColor = EmeraldMain, ambientColor = EmeraldMain),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                                border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("ÚLTIMO SORTEO OFICIAL", color = EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        state.data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(0.1f)))
                                        NumberBall(state.data.reintegro.toString(), Color.Red)
                                    }
                                }
                            }
                        }
                        else -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape), color = EmeraldMain)
                    }
                }

                // NÚMEROS DE ORO (MÁS APARICIONES)
                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Column {
                            Text(
                                "NÚMEROS CON MÁS APARICIONES HISTÓRICAS",
                                color = GoldLuxe,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                stats.take(12).forEach { num -> GoldenBall(num.toString()) }
                            }
                        }
                    }
                }

                // SECCIÓN HISTORIAL
                item {
                    Text(
                        "JUGADAS GUARDADAS",
                        color = Color.White.copy(0.5f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }

                items(savedCombinations) { combination ->
                    CombinationRow(combination) { viewModel.deleteEntry(it) }
                }
            }
        }

        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))

        // DIÁLOGOS
        if (showManualSelector) {
            ManualEntryDialog(
                onDismiss = { showManualSelector = false },
                onSave = { n, r -> viewModel.guardarJugadaManual(n, r) }
            )
        }

        if (showExitDialog) {
            LuxeExitDialog(
                onDismiss = { showExitDialog = false },
                onConfirm = { (context as? Activity)?.finish() }
            )
        }
    }
}

// --- COMPONENTES ATÓMICOS DE DISEÑO LUXE ---

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(0.06f),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun LuxeExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
            border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Logout, null, tint = EmeraldMain, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿DESEA SALIR?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-1).sp)
                Text("El Oráculo dejará de sincronizar.", color = Color.White.copy(0.6f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("CANCELAR", color = Color.White)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("SALIR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoldenBall(num: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "gold")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = Modifier.size(42.dp).scale(glowScale).clip(CircleShape)
            .background(Color.Black.copy(0.4f)).border(1.dp, GoldLuxe.copy(0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(num, color = GoldLuxe, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun NumberBall(number: String, color: Color) {
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun CombinationRow(combination: Combination, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(combination.type, color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 2.sp)
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    combination.numbers.forEach { num ->
                        Text(num.toString(), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(EmeraldMain.copy(0.1f)).border(1.dp, EmeraldMain.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(combination.reintegro.toString(), color = EmeraldMain, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
            IconButton(onClick = { onDelete(combination.id) }) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(onDismiss: () -> Unit, onSave: (List<Int>, Int) -> Unit) {
    var selectedNums by remember { mutableStateOf(setOf<Int>()) }
    var selectedR by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
            border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ELEGIR JUGADA", color = EmeraldMain, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (1..49).forEach { n ->
                        val isSel = selectedNums.contains(n)
                        Surface(
                            onClick = { if(isSel) selectedNums -= n else if(selectedNums.size < 6) selectedNums += n },
                            modifier = Modifier.padding(4.dp).size(36.dp),
                            shape = CircleShape,
                            color = if(isSel) EmeraldMain else Color.Black.copy(0.3f),
                            border = BorderStroke(1.dp, if(isSel) Color.White else EmeraldMain.copy(0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(n.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("REINTEGRO", color = EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..9).forEach { r ->
                        val isSel = selectedR == r
                        Surface(
                            onClick = { selectedR = r },
                            modifier = Modifier.size(30.dp),
                            shape = CircleShape,
                            color = if(isSel) Color.Red else Color.Black.copy(0.3f),
                            border = BorderStroke(1.dp, if(isSel) Color.White else Color.Red.copy(0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(r.toString(), color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Button(
                    onClick = { onSave(selectedNums.toList(), selectedR); onDismiss() },
                    enabled = selectedNums.size == 6,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
                ) {
                    Text("GUARDAR MAESTRA", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}