package com.sagon.prim

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.shadow
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * PRIMITIVA PRIM - MainActivity
 * Diseño Editorial Premium 2026.
 */

private val EmeraldMain = Color(0xFF10B981)
private val EmeraldSurface = Color(0xFF064E3B)
private val GoldPrim = Color(0xFFFDE047)
private val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF02100E))
private val PinkPrim = Color(0xFFF472B6)
private val GlassEffect = Color.White.copy(0.05f)

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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

    override fun onResume() {
        super.onResume()
        viewModel.refreshSavedCombinations()
    }
}

enum class PrimScreen(val title: String, val icon: ImageVector) {
    Inicio("INICIO", Icons.Rounded.Home),
    Historial("BOLETOS", Icons.Rounded.ReceiptLong),
    Estadisticas("TOP", Icons.Rounded.BarChart)
}

@ExperimentalGetImage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val savedCombinations by viewModel.savedCombinations.collectAsState()
    val confettiTrigger by viewModel.confettiTrigger.collectAsState()
    val dailyLuck by viewModel.dailyLuck.collectAsState()
    val hasNewBoletos by viewModel.hasNewBoletos.collectAsState()
    val context = LocalContext.current
    
    var currentScreen by remember { mutableStateOf(PrimScreen.Inicio) }

    LaunchedEffect(currentScreen) {
        if (currentScreen == PrimScreen.Historial) {
            viewModel.clearNewBoletosNotification()
        }
    }

    var showManualSelector by remember { mutableStateOf(false) }
    var showQRReader by remember { mutableStateOf(false) }
    var showScanResult by remember { mutableStateOf(false) }
    var showPredictionExplainer by remember { mutableStateOf(false) }
    var showMagicReveal by remember { mutableStateOf(false) }
    var showLuckExplainer by remember { mutableStateOf(false) }

    var initialManualNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var initialManualName by remember { mutableStateOf("") }
    var initialReintegro by remember { mutableIntStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val prefs = remember { context.getSharedPreferences("prim_prefs", Context.MODE_PRIVATE) }
    var showInfoDialog by remember { 
        mutableStateOf(prefs.getBoolean("first_run_info", true)) 
    }
    
    LaunchedEffect(showInfoDialog) {
        if (!showInfoDialog && prefs.getBoolean("first_run_info", true)) {
            prefs.edit().putBoolean("first_run_info", false).apply()
        }
    }

    var selectedDeleteId by remember { mutableStateOf<Long?>(null) }

    BackHandler { showExitDialog = true }

    val party = remember {
        Party(speed = 10f, maxSpeed = 30f, damping = 0.9f, spread = 360,
            colors = listOf(0xFF10B981.toInt(), 0xFFFDE047.toInt()),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(100))
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(DeepGreenGradient))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("PRIMITIVA PRIM",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1.5).sp
                            ), color = EmeraldMain)
                    },
                    actions = {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Rounded.Info, null, tint = Color.White.copy(0.5f))
                        }
                        IconButton(onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }) {
                            Icon(Icons.Rounded.Settings, null, tint = Color.White.copy(0.5f))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = EmeraldMain
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = Color.Black.copy(0.7f),
                    modifier = Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.9f))
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Botones de Acción Fijos (Solo en Inicio)
                        if (currentScreen == PrimScreen.Inicio) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                ActionButton("CREAR APUESTA MANUAL", Icons.Rounded.Edit, EmeraldMain, Modifier.fillMaxWidth(), "ELIGE TUS NÚMEROS A MANO", onClick = {
                                    initialManualNumbers = emptyList()
                                    initialManualName = ""
                                    initialReintegro = 0
                                    showManualSelector = true
                                })
                            }
                        }

                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            PrimScreen.entries.forEach { screen ->
                                val isHistorialNotify = screen == PrimScreen.Historial && hasNewBoletos
                                
                                val infiniteTransition = rememberInfiniteTransition(label = "nav_glow")
                                val navGlow by if (isHistorialNotify) {
                                    infiniteTransition.animateColor(
                                        initialValue = Color.White.copy(0.4f),
                                        targetValue = GoldPrim,
                                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "glow"
                                    )
                                } else {
                                    remember { mutableStateOf(Color.White.copy(0.4f)) }
                                }
                                
                                val navScale by if (isHistorialNotify) {
                                    infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.2f,
                                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "scale"
                                    )
                                } else {
                                    remember { mutableStateOf(1f) }
                                }

                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { 
                                        Icon(
                                            screen.icon, 
                                            null, 
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer(scaleX = navScale, scaleY = navScale),
                                            tint = if (currentScreen == screen) GoldPrim else navGlow
                                        ) 
                                    },
                                    label = { 
                                        Text(
                                            screen.title, 
                                            fontSize = 9.sp, 
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (currentScreen == screen) GoldPrim else navGlow
                                        ) 
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = GoldPrim,
                                        selectedTextColor = GoldPrim,
                                        unselectedIconColor = Color.White.copy(0.4f),
                                        unselectedTextColor = Color.White.copy(0.4f),
                                        indicatorColor = EmeraldMain.copy(0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }.using(SizeTransform(clip = false))
                    }, label = "screen_nav"
                ) { screen ->
                    when (screen) {
                        PrimScreen.Inicio -> HomeScreen(
                            uiState = uiState,
                            luckLevel = dailyLuck,
                            onStatsClick = { currentScreen = PrimScreen.Estadisticas },
                            onHistoryClick = { currentScreen = PrimScreen.Historial },
                            onTrendClick = { nums ->
                                initialManualNumbers = nums
                                initialManualName = "LOS QUE MÁS SALEN (ESTADÍSTICAS)"
                                initialReintegro = 0
                                showManualSelector = true
                            },
                            onRevealLuck = { viewModel.revealLuck() },
                            viewModel = viewModel,
                            onLuckInfoClick = { showLuckExplainer = true }
                        )
                        PrimScreen.Historial -> HistoryScreen(
                            savedCombinations = savedCombinations,
                            uiState = uiState,
                            onDeleteClick = { id ->
                                selectedDeleteId = id
                                showDeleteDialog = true
                            }
                        )
                        PrimScreen.Estadisticas -> StatsScreen(uiState = uiState)
                    }
                }
            }
        }

        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))

        if (showInfoDialog) {
            InfoPrimDialog(onDismiss = { showInfoDialog = false })
        }

        if (showLuckExplainer) {
            LuckExplainerDialog(onDismiss = { showLuckExplainer = false })
        }

        if (showDeleteDialog && selectedDeleteId != null) {
            DeleteConfirmDialog(onDismiss = { showDeleteDialog = false }, onConfirm = {
                selectedDeleteId?.let { viewModel.deleteEntry(it) }
                showDeleteDialog = false
            })
        }



        if (showScanResult) {
            ScanResultPrimDialog(
                numbers = initialManualNumbers,
                reintegro = initialReintegro,
                winningResult = (uiState as? MainUiState.Success)?.data,
                onDismiss = { showScanResult = false },
                onContinue = { nombre ->
                    viewModel.guardarJugadaManual(nombre, initialManualNumbers, initialReintegro)
                    showScanResult = false
                }
            )
        }

        if (showManualSelector) {
            ManualEntryDialog(
                initialName = initialManualName,
                initialNumbers = initialManualNumbers,
                initialReintegro = initialReintegro,
                onDismiss = { showManualSelector = false },
                onSave = { nombre, n, r -> viewModel.guardarJugadaManual(nombre, n, r) }
            )
        }

        if (showPredictionExplainer) {
            PredictionExplainerDialog(
                onDismiss = { showPredictionExplainer = false },
                onConfirm = {
                    showPredictionExplainer = false
                    // Iniciamos la magia
                    showMagicReveal = true
                }
            )
        }

        if (showMagicReveal) {
            MagicRevealDialog(
                onDismiss = { showMagicReveal = false },
                onFinished = { name, nums, r ->
                    viewModel.guardarJugadaManual(name, nums, r)
                    showMagicReveal = false
                },
                estadisticasReales = (uiState as? MainUiState.Success)?.data?.frequent ?: emptyList(),
                viewModel = viewModel
            )
        }

        if (showExitDialog) PrimExitDialog({ showExitDialog = false }, { (context as? Activity)?.finish() })
    }
}

@Composable
fun SmartGeneratorCard(
    onGenerateAdvanced: () -> Unit,
    onViewHistory: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF001A14),
        border = BorderStroke(1.dp, GoldPrim.copy(0.6f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Analytics, null, tint = GoldPrim, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "GENERADOR ESTADÍSTICO INTELIGENTE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Crea combinaciones equilibradas basadas en los patrones que más se repiten en los sorteos ganadores (pares, impares y sumas óptimas).",
                color = Color.White.copy(0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onGenerateAdvanced()
                    onViewHistory()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldMain,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREAR JUGADA INTELIGENTE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MagicRevealDialog(
    onDismiss: () -> Unit,
    onFinished: (String, List<Int>, Int) -> Unit,
    estadisticasReales: List<Int>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val pool = if (estadisticasReales.size >= 12) estadisticasReales else listOf(43, 28, 33, 40, 7, 12, 22, 34, 11, 30, 31, 37, 38, 48, 6, 10, 16, 25, 41, 1)
    
    // Invocamos la suerte del Cosmos para los 2 números adicionales
    val finalNums = remember { 
        val stars = pool.shuffled().take(4)
        val cosmos = viewModel.getCosmicNumbers(2, stars)
        (stars + cosmos).sorted() 
    }
    val finalReint = remember { (0..9).random() }
    
    var revealedCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        for (i in 1..6) {
            kotlinx.coroutines.delay(800)
            revealedCount = i
            PrimSound.playBeep(ToneGenerator.TONE_DTMF_1, 100)
            PrimHaptic.vibrate(context, 30)
        }
        kotlinx.coroutines.delay(1000)
        revealedCount = 7 // Para el reintegro
        PrimSound.playBeep(ToneGenerator.TONE_PROP_ACK, 200)
        PrimHaptic.success(context)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(40.dp),
            color = Color(0xFF011612),
            border = BorderStroke(2.dp, Brush.sweepGradient(listOf(EmeraldMain, GoldPrim, EmeraldMain)))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "INVOCANDO TU SUERTE",
                    color = GoldPrim,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // La Nebulosa Mágica
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f, targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "rot"
                    )
                    
                    Canvas(modifier = Modifier.size(180.dp).graphicsLayer(rotationZ = rotation)) {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(EmeraldMain.copy(0.2f), Color.Transparent)),
                            radius = size.width / 2
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(EmeraldMain, GoldPrim, Color.Transparent)),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    
                    if (revealedCount < 7) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ANALIZANDO", color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${(revealedCount * 16.6).toInt()}%", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = GoldPrim, modifier = Modifier.size(60.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
                
                // Números apareciendo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    finalNums.forEachIndexed { index, num ->
                        AnimatedVisibility(
                            visible = revealedCount > index,
                            enter = scaleIn() + fadeIn()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(EmeraldMain, EmeraldSurface)))
                                    .border(1.dp, GoldPrim.copy(0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(num.toString(), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AnimatedVisibility(
                    visible = revealedCount == 7,
                    enter = slideInVertically { it } + fadeIn()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("REINTEGRO ESPECIAL: ", color = Color.White.copy(0.6f), fontSize = 12.sp)
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Red).border(1.dp, GoldPrim, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(finalReint.toString(), color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                if (revealedCount == 7) {
                    Button(
                        onClick = { 
                            onFinished("JUGADA MÁGICA", finalNums, finalReint)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldMain,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "¡ESTA ES LA MÍA!", 
                            color = Color.White, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Text("ESPERA A QUE EL DESTINO HABLE...", color = Color.White.copy(0.3f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun LuckExplainerDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF011612),
            border = BorderStroke(1.dp, GoldPrim.copy(0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Analytics, null, tint = GoldPrim, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "ANÁLISIS ESTADÍSTICO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "¿CÓMO FUNCIONA EL GENERADOR?",
                    color = GoldPrim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "Las recomendaciones se calculan combinando la estadística histórica de los sorteos y la fecha actual:",
                    color = Color.White.copy(0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LuckFactorItem("Frecuencia", "Basado en los números que más veces han salido en los sorteos.")
                LuckFactorItem("Ciclo Diario", "La fecha de hoy ayuda a combinar las series numéricas.")
                LuckFactorItem("Equilibrio", "Mezcla de números pares, impares y sumas óptimas.")

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMain,
                        contentColor = Color.White
                    )
                ) {
                    Text("¡ENTENDIDO!", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun LuckFactorItem(title: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Rounded.Star, null, tint = GoldPrim, modifier = Modifier.size(14.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, color = GoldPrim, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = Color.White.copy(0.6f), fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
fun HomeScreen(
    uiState: MainUiState,
    luckLevel: Int,
    onStatsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTrendClick: (List<Int>) -> Unit,
    onRevealLuck: () -> Unit,
    viewModel: MainViewModel,
    onLuckInfoClick: () -> Unit
) {
    val context = LocalContext.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            SmartGeneratorCard(
                onGenerateAdvanced = { viewModel.generarCombinacionAvanzada() },
                onViewHistory = onHistoryClick
            )
        }

        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val rawDate = (uiState as? MainUiState.Success)?.data?.date ?: ""
                val displayDate = if (rawDate.trim().uppercase() == "ÚLTIMO SORTEO" || rawDate.trim().uppercase() == "PRIMITIVA") "" else rawDate
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ÚLTIMO SORTEO",
                        color = EmeraldMain.copy(0.6f),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 4.sp)
                    if (displayDate.isNotEmpty()) {
                        Text(
                            text = displayDate, 
                            color = GoldPrim.copy(0.5f), 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 8.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (val state = uiState) {
                    is MainUiState.Success -> ResultCard(state.data)
                    is MainUiState.Error -> ErrorDisplay(state.message) { /* viewModel.syncWithOfficialWeb() */ }
                    MainUiState.Loading -> LoadingDisplay()
                }
            }
        }

        item {
            if (uiState is MainUiState.Success) {
                val stats = uiState.data.frequent
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LOS QUE MÁS SALEN",
                            color = GoldPrim.copy(0.8f),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp)
                        Text("VER TODO", 
                            color = EmeraldMain, 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onStatsClick() })
                    }

                    TrendRow(stats.take(6)) {
                        onTrendClick(stats.take(6))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    savedCombinations: List<Combination>,
    uiState: MainUiState,
    onDeleteClick: (Long) -> Unit
) {
    if (savedCombinations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.ReceiptLong, null, tint = Color.White.copy(0.1f), modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("NO HAY BOLETOS GUARDADOS", color = Color.White.copy(0.3f), fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text("Escanea o crea uno para empezar", color = Color.White.copy(0.2f), fontSize = 10.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val lastDate = (uiState as? MainUiState.Success)?.data?.date ?: "Sorteo Actual"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldMain.copy(0.05f),
                    border = BorderStroke(1.dp, EmeraldMain.copy(0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = EmeraldMain, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tus jugadas se comparan con el último sorteo oficial ($lastDate). Los aciertos indican qué habría pasado si hubieras jugado ese día.",
                            color = Color.White.copy(0.7f),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
                
                Text("TUS JUGADAS ACTIVAS", color = GoldPrim, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(savedCombinations, key = { it.id }) { combination ->
                val lastRes = (uiState as? MainUiState.Success)?.data
                CombinationRow(combination, lastRes, onDeleteClick)
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(0.3f),
                    border = BorderStroke(1.dp, GoldPrim.copy(0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp) 
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = GoldPrim, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "¡QUE NO SE TE OLVIDE!",
                            color = GoldPrim,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Esta app es informativa. No olvides validar tus apuestas favoritas en tu administración oficial más cercana.",
                            color = Color.White.copy(0.6f),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(uiState: MainUiState) {
    if (uiState is MainUiState.Success) {
        val stats = uiState.data.frequent
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("RANKING DE APARICIONES", color = GoldPrim, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
                Text("Números con mayor probabilidad histórica", color = Color.White.copy(0.4f), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(stats.chunked(3)) { rowNums ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowNums.forEach { num ->
                        Surface(
                            modifier = Modifier.weight(1f).height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = GlassEffect,
                            border = BorderStroke(1.dp, GoldPrim.copy(0.1f))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(EmeraldMain),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(num.toString(), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("TOP AFORTUNADO", color = GoldPrim.copy(0.6f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    // Si la fila no está llena, añadir espacios vacíos para mantener el ancho
                    if (rowNums.size < 3) {
                        repeat(3 - rowNums.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldMain.copy(0.05f)),
                    border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = EmeraldMain, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Estos números han salido más veces en los bombos reales. Usarlos aumenta tu probabilidad estadística.",
                            color = Color.White.copy(0.7f),
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    } else {
        LoadingDisplay()
    }
}

@Composable
fun PredictionExplainerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = EmeraldSurface,
            border = BorderStroke(1.dp, GoldPrim.copy(0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = GoldPrim, modifier = Modifier.size(50.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "EL CEREBRO DE LA APP",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    "¿CÓMO CALCULO TU JUGADA?",
                    color = GoldPrim,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    "No elijo números al azar. Analizo los que más veces han salido en la historia de la Primitiva.",
                    color = Color.White.copy(0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Elegiré 4 de esos números 'estrella' y 2 más que el Destino y el Cosmos elijan para ti hoy.",
                    color = Color.White.copy(0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMain,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "¡GENERAR JUGADA GANADORA!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
                
                TextButton(onClick = onDismiss) {
                    Text("AHORA NO", color = Color.White.copy(0.3f))
                }
            }
        }
    }
}

@Composable
fun ScanResultPrimDialog(
    numbers: List<Int>,
    reintegro: Int,
    winningResult: LotteryResult?,
    onDismiss: () -> Unit,
    onContinue: (String) -> Unit
) {
    val context = LocalContext.current
    val hits = winningResult?.let { res -> numbers.count { it in res.numbers } } ?: 0
    val reintHit = winningResult?.reintegro == reintegro
    val isWinner = hits >= 3 || (hits >= 0 && reintHit)

    var ticketName by remember { mutableStateOf("MI JUGADA") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = EmeraldSurface,
            border = BorderStroke(1.dp, if (isWinner) GoldPrim.copy(0.5f) else EmeraldMain.copy(0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (isWinner) Icons.Rounded.EmojiEvents else Icons.Rounded.ReceiptLong,
                    null,
                    tint = if (isWinner) GoldPrim else Color.White.copy(0.3f),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (isWinner) "¡BOLETO PREMIADO!" else "BOLETO COMPROBADO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    "RESULTADO: $hits ACIERTOS ${if (reintHit) "+ REINTEGRO" else ""}",
                    color = GoldPrim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    numbers.forEach { num ->
                        val isHit = winningResult?.numbers?.contains(num) == true
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isHit) EmeraldMain else Color.White.copy(0.1f))
                                .border(1.dp, if (isHit) GoldPrim else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(num.toString(), color = if (isHit) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("REINTEGRO: $reintegro ${if (reintHit) "✓" else ""}",
                    color = if (reintHit) Color.Red else Color.White.copy(0.6f),
                    fontWeight = FontWeight.Black)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = ticketName,
                    onValueChange = { ticketName = it },
                    label = { Text("NOMBRE DE TU JUGADA", color = Color.White.copy(0.4f), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = { 
                            PrimSound.playBeep(ToneGenerator.TONE_PROP_ACK)
                            PrimHaptic.success(context)
                            onContinue(ticketName) 
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldMain,
                            contentColor = Color.White
                        )
                    ) {
                        Text("GUARDAR", color = Color.White, fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareText = "¡He escaneado mi jugada de la Primitiva!\n" +
                                    "🔮 Números: ${numbers.joinToString(" - ")}\n" +
                                    "🎯 Reintegro: $reintegro\n\n" +
                                    "Escaneado con Primitiva Prim: https://play.google.com/store/apps/details?id=com.sagon.prim"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir"))
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, EmeraldMain.copy(0.4f))
                    ) {
                        Icon(Icons.Rounded.Share, null, tint = EmeraldMain)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("CERRAR", color = Color.White.copy(0.4f))
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    detail: String? = null,
    isPulsing: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val context = LocalContext.current
    val scale by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val glowOpacity by if (isPulsing) {
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Surface(
        onClick = {
            PrimSound.playBeep()
            PrimHaptic.vibrate(context, 20)
            onClick()
        },
        modifier = modifier
            .height(70.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(0.08f),
        border = BorderStroke(1.dp, if (isPulsing) color.copy(glowOpacity) else color.copy(0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = color.copy(0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ResultCard(data: LotteryResult) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(30.dp, spotColor = EmeraldMain, ambientColor = EmeraldMain),
        shape = RoundedCornerShape(32.dp),
        color = GlassEffect,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(EmeraldMain.copy(0.4f), Color.Transparent)))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                Box(modifier = Modifier.width(1.5.dp).height(30.dp).background(Color.White.copy(0.1f)))
                NumberBall(data.reintegro.toString(), Color(0xFFEF4444))
            }
        }
    }
}



@Composable
fun NumberBall(text: String, color: Color) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
fun TrendRow(numbers: List<Int>, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = GlassEffect,
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                numbers.forEach { num ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.3f))
                            .border(1.dp, GoldPrim.copy(0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(num.toString(), color = GoldPrim, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CombinationRow(combination: Combination, lastResult: LotteryResult?, onDeleteClick: (Long) -> Unit) {
    val context = LocalContext.current
    val hits = lastResult?.let { res -> combination.numbers.count { it in res.numbers } } ?: 0
    val hitReint = lastResult?.reintegro == combination.reintegro
    val winner = hits >= 3 || (hits >= 0 && hitReint)
    
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "glow"
    )

    val hitScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "hit_scale"
    )
    val dopamineColor by infiniteTransition.animateColor(
        initialValue = EmeraldMain,
        targetValue = GoldPrim,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "dopamine_color"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            .shadow(if (winner) 20.dp else 0.dp, spotColor = EmeraldMain),
        shape = RoundedCornerShape(28.dp),
        color = if (winner) EmeraldMain.copy(0.15f) else Color.White.copy(glowIntensity),
        border = BorderStroke(
            1.dp, 
            if (winner) Brush.sweepGradient(listOf(EmeraldMain, GoldPrim, EmeraldMain))
            else SolidColor(Color.White.copy(0.1f))
        )
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val displayName = when(combination.name) {
                        "PREDICCIÓN MAESTRA", "COMBINACIÓN PROBABLE", "JUGADA MÁS PROBABLE" -> "✨ JUGADA MÁGICA"
                        else -> combination.name
                    }
                    Text(
                        text = displayName.uppercase(), 
                        color = GoldPrim, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 8.sp, 
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val hasHits = hits > 0 || hitReint
                    if (hasHits) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .graphicsLayer(scaleX = hitScale, scaleY = hitScale)
                                .clip(RoundedCornerShape(4.dp))
                                .background(dopamineColor)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            val text = when {
                                hits > 0 && hitReint -> "$hits ${if (hits == 1) "ACIERTO" else "ACIERTOS"} + R"
                                hits > 0 -> "$hits ${if (hits == 1) "ACIERTO" else "ACIERTOS"}"
                                hitReint -> "REINTEGRO"
                                else -> ""
                            }
                            Text(text, color = if (winner) Color.Black else Color.White, fontSize = 7.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    combination.numbers.forEach { num ->
                        val isHit = lastResult?.numbers?.contains(num) == true
                        Text(
                            text = num.toString().padStart(2, '0'), 
                            color = if (isHit) dopamineColor else Color.White, 
                            fontWeight = if (isHit) FontWeight.Black else FontWeight.Bold, 
                            fontSize = 15.sp,
                            modifier = if (isHit) Modifier.graphicsLayer(
                                scaleX = hitScale, 
                                scaleY = hitScale
                            ) else Modifier.graphicsLayer(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R", color = if (hitReint) Color.Red else EmeraldMain.copy(0.5f), fontSize = 7.sp, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (hitReint) Color.Red else EmeraldMain.copy(0.1f))
                        .border(1.dp, if (hitReint) GoldPrim else EmeraldMain.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        combination.reintegro.toString(), 
                        color = if (hitReint) Color.White else EmeraldMain, 
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            IconButton(
                onClick = {
                    val shareText = "✨ ¡Mira mi JUGADA MÁGICA de la Primitiva Prim! ✨\n" +
                            "🔮 Números: ${combination.numbers.joinToString(" - ")}\n" +
                            "🎯 Reintegro: ${combination.reintegro}\n\n" +
                            "Descarga la app: https://play.google.com/store/apps/details?id=com.sagon.prim\n" +
                            "¿Será hoy el gran día? 🤞💰 #PrimitivaPrim"
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartir Suerte"))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.Share, null, tint = GoldPrim.copy(0.7f), modifier = Modifier.size(16.dp))
            }
            
            IconButton(
                onClick = { onDeleteClick(combination.id) },
                modifier = Modifier.size(32.dp)
            ) { 
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(16.dp)) 
            }
        }
    }
}

@Composable
fun LoadingDisplay() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EmeraldMain, strokeWidth = 3.dp)
    }
}

@Composable
fun ErrorDisplay(msg: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(msg, color = Color.White.copy(0.5f), fontSize = 12.sp)
        TextButton(onClick = onRetry) { Text("REINTENTAR", color = EmeraldMain) }
    }
}

@Composable
fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { 
            Button(
                onClick = onConfirm, 
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                )
            ) { 
                Text("ELIMINAR", color = Color.White, fontWeight = FontWeight.Black) 
            } 
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("VOLVER", color = GoldPrim, fontWeight = FontWeight.Bold) } },
        title = { Text("¿Eliminar jugada?", color = Color.White, fontWeight = FontWeight.Black) },
        containerColor = EmeraldSurface,
        shape = RoundedCornerShape(32.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(
    initialName: String = "",
    initialNumbers: List<Int>,
    initialReintegro: Int = 0,
    onDismiss: () -> Unit,
    onSave: (String, List<Int>, Int) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var selected by remember { mutableStateOf(initialNumbers.toSet()) }
    var reintegro by remember { mutableIntStateOf(initialReintegro) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = EmeraldSurface,
            border = BorderStroke(1.dp, GoldPrim.copy(0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AÑADIR JUGADA",
                    color = GoldPrim,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Text(
                    text = "Escribe un nombre y elige tus 6 números",
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ej: Mi jugada del jueves", color = Color.White.copy(0.3f), fontSize = 12.sp) },
                    label = { Text("NOMBRE DE LA JUGADA", color = GoldPrim.copy(0.7f), fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldMain,
                        unfocusedBorderColor = Color.White.copy(0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ELIGE 6 NÚMEROS:", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text("${selected.size} / 6", color = if (selected.size == 6) EmeraldMain else GoldPrim, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 7
                ) {
                    (1..49).forEach { n ->
                        val isS = selected.contains(n)
                        Box(
                            modifier = Modifier
                                .padding(3.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isS) EmeraldMain else Color.White.copy(0.05f))
                                .border(1.dp, if (isS) GoldPrim.copy(0.5f) else Color.Transparent, CircleShape)
                                .clickable {
                                    if (isS) {
                                        PrimSound.playBeep(ToneGenerator.TONE_DTMF_1, 50)
                                        PrimHaptic.vibrate(context, 15)
                                        selected -= n
                                    } else if (selected.size < 6) {
                                        PrimSound.playBeep(ToneGenerator.TONE_DTMF_1, 50)
                                        PrimHaptic.vibrate(context, 15)
                                        selected += n
                                    } else {
                                        // Feedback de error: ya hay 6 seleccionados
                                        PrimSound.playBeep(ToneGenerator.TONE_SUP_ERROR, 200)
                                        PrimHaptic.vibrate(context, 80)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = n.toString(),
                                color = if (isS) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isS) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "REINTEGRO:",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(0.2f))
                            .border(1.dp, Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(reintegro.toString(), color = Color.White, fontWeight = FontWeight.Black)
                    }
                }

                Slider(
                    value = reintegro.toFloat(),
                    onValueChange = { 
                        if (it.toInt() != reintegro) {
                            PrimHaptic.vibrate(context, 10)
                        }
                        reintegro = it.toInt() 
                    },
                    valueRange = 0f..9f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red,
                        inactiveTrackColor = Color.White.copy(0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        PrimSound.playBeep(ToneGenerator.TONE_PROP_ACK)
                        PrimHaptic.success(context)
                        onSave(name, selected.toList().sorted(), reintegro)
                        onDismiss()
                    },
                    enabled = selected.size == 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMain,
                        disabledContainerColor = Color.White.copy(0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (selected.size < 6) "FALTAN ${6 - selected.size} NÚMEROS" else "GUARDAR JUGADA",
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
                
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color.White.copy(0.3f))
                }
            }
        }
    }
}



@Composable
fun PromoPrimCard() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = EmeraldMain.copy(0.08f),
        border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(EmeraldMain, GoldPrim))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.RocketLaunch, null, tint = Color.Black, modifier = Modifier.size(24.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "APP PRIM - 2026",
                    color = GoldPrim,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = "La herramienta definitiva para ganar.",
                    color = Color.White.copy(0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "¡Descarga ya la APP PRIM - Primitiva Prim! La mejor app para ver resultados y generar jugadas con estadística real.\n\nDescarga aquí: https://play.google.com/store/apps/details?id=com.sagon.prim")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Compartir App"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("PROBAR", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun InfoPrimDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = EmeraldSurface,
            border = BorderStroke(1.dp, GoldPrim.copy(0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Analytics, null, tint = GoldPrim, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("GUÍA DE USO", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("FÁCIL Y CLARO", color = GoldPrim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                InfoSection("📊 Estadística Real y Rigurosa", 
                    "El generador aplica el máximo nivel de análisis estadístico posible: se basa en la frecuencia histórica real de los sorteos oficiales y en las leyes de probabilidad matemática.")
                
                InfoSection("⚡ Jugada Inteligente", 
                    "Crea combinaciones equilibradas basadas en matemáticas y estadísticas de premios anteriores.")

                InfoSection("📁 Tus Boletos", 
                    "Organiza tus apuestas y comprueba al instante si has ganado algún premio en el último sorteo.")

                InfoSection("🔒 Privacidad Total", 
                    "Tus jugadas son 100% privadas y se guardan únicamente en tu teléfono móvil.")

                InfoSection("⚠️ Aviso Legal", 
                    "Herramienta informativa y estadística. Sin vinculación con SELAE. Juega con responsabilidad.")

                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
                ) {
                    Text("¡ENTENDIDO!", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun InfoSection(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, color = GoldPrim, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, color = Color.White.copy(0.7f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
fun PrimExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("SALIR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("VOLVER")
            }
        },
        title = {
            Text("¿Cerrar App?", fontWeight = FontWeight.Black)
        },
        containerColor = EmeraldSurface,
        shape = RoundedCornerShape(32.dp)
    )
}

object PrimSound {
    private var toneGenerator: ToneGenerator? = null

    fun playBeep(tone: Int = ToneGenerator.TONE_PROP_BEEP, duration: Int = 150) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 60)
            }
            toneGenerator?.startTone(tone, duration)
        } catch (e: Exception) {
            // Ignorar errores de sonido
        }
    }
}

object PrimHaptic {
    fun vibrate(context: Context, duration: Long = 50) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }
    
    fun success(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 80, 40), -1))
        } else {
            vibrator.vibrate(150)
        }
    }
}
