package com.sagon.prim

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
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
 * PRIMITIVA LUXE - MainActivity
 * Diseño Editorial Premium 2026.
 */

private val EmeraldMain = Color(0xFF10B981)
private val EmeraldSurface = Color(0xFF064E3B)
private val GoldLuxe = Color(0xFFFDE047)
private val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF02100E))
private val PinkLuxe = Color(0xFFF472B6)
private val GlassEffect = Color.White.copy(0.05f)

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
    var showQRReader by remember { mutableStateOf(false) }
    var showScanResult by remember { mutableStateOf(false) }

    var initialManualNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var initialReintegro by remember { mutableIntStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PRIMITIVA LUXE",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp
                                ), color = EmeraldMain)
                            Text("SISTEMA DE ESCRUTINIO REAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 4.sp,
                                    fontWeight = FontWeight.Bold
                                ), color = Color.White.copy(0.4f))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.syncWithOfficialWeb() }) {
                            Icon(Icons.Rounded.Sync, "Sincronizar", tint = EmeraldMain)
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
                contentPadding = PaddingValues(bottom = 32.dp, start = 24.dp, end = 24.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActionButton("ESCANEAR", Icons.Rounded.QrCodeScanner, PinkLuxe, Modifier.weight(1f), "ESCRUTAR QR") { showQRReader = true }
                        ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1f), "ESTADÍSTICA") { viewModel.generarPrediccionMaestra() }
                        ActionButton("AJUSTES", Icons.Rounded.Settings, Color(0xFFA78BFA), Modifier.weight(1f), "PERFIL") { /* Ajustes */ }
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("ÚLTIMO RESULTADO OFICIAL",
                            color = EmeraldMain.copy(0.6f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 4.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        when (val state = uiState) {
                            is MainUiState.Success -> ResultCard(state.data)
                            is MainUiState.Error -> ErrorDisplay(state.message) { viewModel.syncWithOfficialWeb() }
                            MainUiState.Loading -> LoadingDisplay()
                        }
                    }
                }

                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("NÚMEROS CALIENTES (HISTÓRICO)",
                                color = GoldLuxe.copy(0.8f),
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                letterSpacing = 2.sp)

                            TrendRow(stats.take(6), context) {
                                initialManualNumbers = stats.take(6)
                                showManualSelector = true
                            }
                        }
                    }
                }

                item {
                    Text("HISTORIAL DE JUGADAS",
                        color = Color.White.copy(0.3f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp)
                }

                items(savedCombinations, key = { it.id }) { combination ->
                    val lastRes = (uiState as? MainUiState.Success)?.data
                    CombinationRow(combination, lastRes) { id ->
                        selectedDeleteId = id
                        showDeleteDialog = true
                    }
                }
            }
        }

        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))

        if (showDeleteDialog && selectedDeleteId != null) {
            DeleteConfirmDialog(onDismiss = { showDeleteDialog = false }, onConfirm = {
                selectedDeleteId?.let { viewModel.deleteEntry(it) }
                showDeleteDialog = false
            })
        }

        if (showQRReader) {
            QRReaderDialog(
                onDismiss = { showQRReader = false },
                onNumbersDetected = { numbers, r ->
                    initialManualNumbers = numbers
                    initialReintegro = r ?: 0
                    showScanResult = true
                    showQRReader = false
                }
            )
        }

        if (showScanResult) {
            ScanResultLuxeDialog(
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
                initialNumbers = initialManualNumbers,
                initialReintegro = initialReintegro,
                onDismiss = { showManualSelector = false },
                onSave = { nombre, n, r -> viewModel.guardarJugadaManual(nombre, n, r) }
            )
        }

        if (showExitDialog) LuxeExitDialog({ showExitDialog = false }, { (context as? Activity)?.finish() })
    }
}

@Composable
fun ScanResultLuxeDialog(
    numbers: List<Int>,
    reintegro: Int,
    winningResult: LotteryResult?,
    onDismiss: () -> Unit,
    onContinue: (String) -> Unit
) {
    val hits = winningResult?.let { res -> numbers.count { it in res.numbers } } ?: 0
    val reintHit = winningResult?.reintegro == reintegro
    val isWinner = hits >= 3 || (hits >= 0 && reintHit)

    var ticketName by remember { mutableStateOf("SCAN ${System.currentTimeMillis().toString().takeLast(4)}") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = EmeraldSurface,
            border = BorderStroke(1.dp, if (isWinner) GoldLuxe.copy(0.5f) else EmeraldMain.copy(0.2f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (isWinner) Icons.Rounded.EmojiEvents else Icons.Rounded.ReceiptLong,
                    null,
                    tint = if (isWinner) GoldLuxe else Color.White.copy(0.3f),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (isWinner) "¡TICKET PREMIADO!" else "TICKET ESCANEADO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    "ESCRUTINIO: $hits ACIERTOS ${if (reintHit) "+ R" else ""}",
                    color = GoldLuxe,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    numbers.forEach { num ->
                        val isHit = winningResult?.numbers?.contains(num) == true
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isHit) EmeraldMain else Color.White.copy(0.1f))
                                .border(1.dp, if (isHit) GoldLuxe else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(num.toString(), color = if (isHit) Color.Black else Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
                    label = { Text("ETIQUETA DEL TICKET", color = Color.White.copy(0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onContinue(ticketName) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)
                ) {
                    Text("GUARDAR EN HISTORIAL", fontWeight = FontWeight.Black)
                }

                TextButton(onClick = onDismiss) {
                    Text("CERRAR", color = Color.White.copy(0.4f))
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, detail: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(0.06f),
        border = BorderStroke(1.dp, color.copy(0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            Text(detail ?: " ", color = color.copy(0.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
            Text(data.date, color = GoldLuxe.copy(0.9f), fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                Box(modifier = Modifier.width(1.5.dp).height(30.dp).background(Color.White.copy(0.1f)))
                NumberBall(data.reintegro.toString(), Color(0xFFEF4444))
            }
        }
    }
}

@Composable
fun QRReaderDialog(onDismiss: () -> Unit, onNumbersDetected: (List<Int>, Int?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()) }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "laser"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = EmeraldSurface, border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ESCÁNER LUXE", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 2.sp)
                Text("DETECCIÓN AUTOMÁTICA DE PREMIOS", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.size(260.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black).border(2.dp, EmeraldMain.copy(0.3f), RoundedCornerShape(24.dp))) {
                    AndroidView(factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build().also {
                                        it.setAnalyzer(executor) { proxy ->
                                            proxy.image?.let { img ->
                                                val inputImage = InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                                                scanner.process(inputImage).addOnSuccessListener { barcodes ->
                                                    for (barcode in barcodes) {
                                                        val raw = barcode.rawValue ?: ""

                                                        // --- LUXE EXTRACTION ENGINE (REFINED) ---
                                                        var detectedNums = emptyList<Int>()
                                                        var detectedReint: Int? = null

                                                        // 1. Prioridad: Parámetro 'n' (combinación) y 'r' (reintegro)
                                                        val nMatch = Regex("[?&]n=([0-9,.;-]+)").find(raw)
                                                        val rMatch = Regex("[?&]r=([0-9])").find(raw)

                                                        if (nMatch != null) {
                                                            val content = nMatch.groupValues[1]
                                                            detectedNums = if (content.length >= 12 && !content.contains(",")) {
                                                                (0..5).mapNotNull { i -> content.substring(i*2, i*2+2).toIntOrNull() }
                                                            } else {
                                                                content.split(Regex("[,.;-]")).mapNotNull { it.toIntOrNull() }
                                                            }
                                                            detectedReint = rMatch?.groupValues?.get(1)?.toIntOrNull()
                                                        } else {
                                                            // 2. Fallback: Cascada de detección de secuencias
                                                            val allDigits = Regex("\\d+").findAll(raw).map { it.value }.toList()
                                                            for (s in allDigits) {
                                                                if (s.length >= 12) {
                                                                    val candidateNums = mutableListOf<Int>()
                                                                    for (i in 0 until 12 step 2) {
                                                                        if (i+2 <= s.length) {
                                                                            val pair = s.substring(i, i+2).toIntOrNull()
                                                                            if (pair != null && pair in 1..49) candidateNums.add(pair)
                                                                        }
                                                                    }
                                                                    if (candidateNums.size >= 6) {
                                                                        detectedNums = candidateNums.take(6)
                                                                        if (s.length >= 13 && detectedReint == null) {
                                                                            detectedReint = s.substring(12, 13).toIntOrNull()
                                                                        }
                                                                        break
                                                                    }
                                                                }
                                                            }
                                                        }

                                                        // Limpieza final y validación de rango (Primitiva 1-49)
                                                        val finalNums = detectedNums.filter { it in 1..49 }.distinct().sorted()

                                                        if (finalNums.size >= 6) {
                                                            onNumbersDetected(finalNums.take(6), detectedReint)
                                                            return@addOnSuccessListener
                                                        }
                                                    }
                                                }.addOnCompleteListener { proxy.close() }
                                            } ?: proxy.close()
                                        }
                                    }
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    }, modifier = Modifier.fillMaxSize())

                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter).offset(y = 260.dp * laserOffset).background(EmeraldMain).shadow(8.dp, spotColor = EmeraldMain))
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.White.copy(0.4f)) }
            }
        }
    }
}

@Composable
fun NumberBall(text: String, color: Color) {
    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun TrendRow(numbers: List<Int>, context: android.content.Context, onEditClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onEditClick() },
        shape = RoundedCornerShape(24.dp),
        color = GlassEffect,
        border = BorderStroke(1.dp, Color.White.copy(0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                numbers.forEach { num ->
                    Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.Black.copy(0.3f)).border(1.dp, GoldLuxe.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(num.toString(), color = GoldLuxe, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = GoldLuxe)
        }
    }
}

@Composable
fun CombinationRow(combination: Combination, lastResult: LotteryResult?, onDeleteClick: (Long) -> Unit) {
    val hits = lastResult?.let { res -> combination.numbers.count { it in res.numbers } } ?: 0
    val hitReint = lastResult?.reintegro == combination.reintegro
    val winner = hits >= 3 || (hits >= 0 && hitReint)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (winner) EmeraldMain.copy(0.1f) else GlassEffect,
        border = BorderStroke(1.dp, if (winner) EmeraldMain.copy(0.4f) else Color.White.copy(0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(combination.name.uppercase(), color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    combination.numbers.forEach { num ->
                        val isHit = lastResult?.numbers?.contains(num) == true
                        Text(num.toString().padStart(2, '0'), color = if (isHit) EmeraldMain else Color.White, fontWeight = if (isHit) FontWeight.Black else FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).border(1.dp, if (hitReint) Color.Red else EmeraldMain.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Text(combination.reintegro.toString(), color = if (hitReint) Color.Red else EmeraldMain, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onDeleteClick(combination.id) }) { Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f)) }
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
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("ELIMINAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("VOLVER") } },
        title = { Text("¿Eliminar jugada?", fontWeight = FontWeight.Black) },
        containerColor = EmeraldSurface,
        shape = RoundedCornerShape(32.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(initialNumbers: List<Int>, initialReintegro: Int = 0, onDismiss: () -> Unit, onSave: (String, List<Int>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialNumbers.toSet()) }
    var reintegro by remember { mutableIntStateOf(initialReintegro) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(32.dp), color = EmeraldSurface, border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NUEVA JUGADA", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("NOMBRE") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                FlowRow(horizontalArrangement = Arrangement.Center) {
                    (1..49).forEach { n ->
                        val isS = selected.contains(n)
                        Box(modifier = Modifier.padding(4.dp).size(38.dp).clip(CircleShape).background(if (isS) EmeraldMain else Color.White.copy(0.05f)).clickable { if (isS) selected -= n else if (selected.size < 6) selected += n }, contentAlignment = Alignment.Center) {
                            Text(n.toString(), color = if (isS) Color.Black else Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("REINTEGRO: $reintegro", color = GoldLuxe)
                Slider(value = reintegro.toFloat(), onValueChange = { reintegro = it.toInt() }, valueRange = 0f..9f, steps = 8)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { onSave(name, selected.toList().sorted(), reintegro); onDismiss() }, enabled = selected.size == 6, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    Text("GUARDAR JUGADA", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun LuxeExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm) { Text("SALIR") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("VOLVER") } }, title = { Text("¿Cerrar App?", fontWeight = FontWeight.Black) }, containerColor = EmeraldSurface, shape = RoundedCornerShape(32.dp))
}
