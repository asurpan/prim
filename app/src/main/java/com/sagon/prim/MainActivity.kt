package com.sagon.prim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

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
                colorScheme = darkColorScheme(primary = EmeraldMain, surface = EmeraldSurface)
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
                            Text("PRIMITIVA PRIM", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp), color = EmeraldMain)
                            if (uiState is MainUiState.Success) {
                                val state = uiState as MainUiState.Success
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (state.data.isLive) EmeraldMain else Color.Gray))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (state.data.isLive) "CONEXIÓN LIVE" else "MODO LOCAL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncWithOfficialWeb() }) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, tint = EmeraldMain)
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BOTONERA DE ACCIÓN
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton("ELEGIR", Icons.Rounded.Edit, Color(0xFF60A5FA), Modifier.weight(1f)) { }
                            ActionButton("SMART-LUCK", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1f)) {
                                viewModel.generateSmartCombination()
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ActionButton("HISTORIAL", Icons.Rounded.History, Color.White, Modifier.weight(1f)) {
                                scope.launch { if (savedCombinations.isNotEmpty()) listState.animateScrollToItem(4) }
                            }
                            ActionButton("LIMPIAR", Icons.Rounded.DeleteSweep, Color(0xFFF87171), Modifier.weight(1f)) {
                                viewModel.clearAll()
                            }
                        }
                    }
                }

                // RESULTADOS DEL ÚLTIMO SORTEO
                item {
                    when (val state = uiState) {
                        is MainUiState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth().shadow(15.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("SORTEO REAL MÁS RECIENTE", color = EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        state.data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.1f)))
                                        NumberBall(state.data.reintegro.toString(), Color.Red)
                                    }
                                }
                            }
                        }
                        else -> { LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldMain) }
                    }
                }

                // NÚMEROS DE ORO (DINÁMICOS)
                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = GoldLuxe.copy(0.05f)),
                            border = BorderStroke(1.dp, GoldLuxe.copy(0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.TipsAndUpdates, null, tint = GoldLuxe, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FRECUENCIA HISTÓRICA ACTUALIZADA", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    stats.take(12).forEach { num ->
                                        Box(
                                            modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(0.2f)).border(1.dp, GoldLuxe.copy(0.3f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(num.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(savedCombinations) { combination ->
                    CombinationRow(combination) { viewModel.deleteEntry(it) }
                }
            }
        }
        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(16.dp), color = Color.White.copy(0.05f),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun NumberBall(number: String, color: Color) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
    }
}

@Composable
fun CombinationRow(combination: Combination, onDelete: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.02f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(combination.name, color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    combination.numbers.forEach { num -> Text(num.toString(), color = Color.White, fontSize = 12.sp) }
                    Text("| R: ${combination.reintegro}", color = EmeraldMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = { onDelete(combination.id) }) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(20.dp))
            }
        }
    }
}