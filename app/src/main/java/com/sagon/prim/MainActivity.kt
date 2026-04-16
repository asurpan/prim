package com.sagon.prim

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- IDENTIDAD VISUAL EMERALD LUXE V17 ---
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showManualSelector by remember { mutableStateOf(false) }

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
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp
                                ),
                                color = EmeraldMain
                            )
                            if (uiState is MainUiState.Success) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldMain)
                                            .shadow(4.dp, CircleShape, ambientColor = EmeraldMain, spotColor = EmeraldMain)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "ORÁCULO ACTUALIZADO",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                                        color = Color.White.copy(0.6f)
                                    )
                                }
                            }
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
                // PANEL DE ACCIONES LUXE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionButton("ELEGIR", Icons.Rounded.Edit, Color(0xFF60A5FA), Modifier.weight(1f)) {
                                showManualSelector = true
                            }
                            ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1f)) {
                                viewModel.generarPrediccionMaestra()
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionButton("HISTORIAL", Icons.Rounded.History, Color.White, Modifier.weight(1f)) {
                                scope.launch { if (savedCombinations.isNotEmpty()) listState.animateScrollToItem(4) }
                            }
                            ActionButton("LIMPIAR", Icons.Rounded.DeleteSweep, Color(0xFFF87171), Modifier.weight(1f)) {
                                viewModel.clearAll()
                            }
                        }
                    }
                }

                // RESULTADO EN VIVO
                item {
                    when (val state = uiState) {
                        is MainUiState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(32.dp)),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                                border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SORTEO REAL MÁS RECIENTE", color = EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        state.data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.1f)))
                                        NumberBall(state.data.reintegro.toString(), Color.Red)
                                    }
                                }
                            }
                        }
                        else -> {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape), color = EmeraldMain)
                        }
                    }
                }

                // NÚMEROS DE ORO (ANIMACIÓN INFINITA)
                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldLuxe.copy(0.05f)),
                            border = BorderStroke(1.dp, GoldLuxe.copy(0.15f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.TipsAndUpdates, null, tint = GoldLuxe, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("NÚMEROS DE ORO (FRECUENCIA)", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    stats.take(12).forEach { num -> GoldenBall(num.toString()) }
                                }
                            }
                        }
                    }
                }

                // JUGADAS GUARDADAS
                items(savedCombinations) { combination ->
                    CombinationRow(combination) { viewModel.deleteEntry(it) }
                }
            }
        }

        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))

        if (showManualSelector) {
            ManualEntryDialog(
                onDismiss = { showManualSelector = false },
                onSave = { n, r -> viewModel.guardarJugadaManual(n, r) }
            )
        }
    }
}

@Composable
fun GoldenBall(num: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "gold")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(glowScale)
            .clip(CircleShape)
            .background(Color.Black.copy(0.3f))
            .border(1.dp, GoldLuxe.copy(0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(num, color = GoldLuxe, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(0.07f),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun NumberBall(number: String, color: Color) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
fun CombinationRow(combination: Combination, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(combination.name, color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    combination.numbers.forEach { num ->
                        Text(num.toString(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("| R: ${combination.reintegro}", color = EmeraldMain, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
            IconButton(onClick = { onDelete(combination.id) }) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(22.dp))
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
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ELIGE TU JUGADA", color = EmeraldMain, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = (-1).sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text("NÚMEROS (${selectedNums.size}/6)", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (1..49).forEach { n ->
                        val isSel = selectedNums.contains(n)
                        Surface(
                            onClick = {
                                if(isSel) selectedNums -= n
                                else if(selectedNums.size < 6) selectedNums += n
                            },
                            modifier = Modifier.padding(vertical = 4.dp).size(38.dp),
                            shape = CircleShape,
                            color = if(isSel) EmeraldMain else Color.Black.copy(0.3f),
                            border = BorderStroke(1.dp, if(isSel) Color.White else EmeraldMain.copy(0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(n.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp), color = Color.White.copy(0.1f))

                Text("REINTEGRO", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..9).forEach { r ->
                        val isSel = selectedR == r
                        Surface(
                            onClick = { selectedR = r },
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = if(isSel) Color.Red else Color.Black.copy(0.3f),
                            border = BorderStroke(1.dp, if(isSel) Color.White else Color.Transparent)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(r.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { onSave(selectedNums.toList(), selectedR); onDismiss() },
                    enabled = selectedNums.size == 6,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
                ) {
                    Text("GUARDAR JUGADA MAESTRA", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}