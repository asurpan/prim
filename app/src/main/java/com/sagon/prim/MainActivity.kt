package com.sagon.prim

import android.Manifest
import android.app.Activityimport
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// --- PALETA LUXE PLATINUM 2026 ---
private val EmeraldMain = Color(0xFF10B981)
private val EmeraldSurface = Color(0xFF064E3B)
private val GoldLuxe = Color(0xFFFDE047)
private val DeepGreenGradient = listOf(Color(0xFF064E3B), Color(0xFF022C22))
private val PinkLuxe = Color(0xFFF472B6)

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
    var showTicketReader by remember { mutableStateOf(false) }
    var initialManualNumbers by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
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
                            Text("PRIMITIVA PRIM",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp
                                ), color = EmeraldMain)
                            Text("¡QUE LA SUERTE TE ACOMPAÑE!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                ), color = Color.White.copy(0.4f))
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
                contentPadding = PaddingValues(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- BOTONES DE ACCIÓN SUPERIORES ---
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton("LECTOR", Icons.Rounded.QrCodeScanner, PinkLuxe, Modifier.weight(1f), "ESCANEO IA") {
                            showTicketReader = true
                        }
                        ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1.3f), "TOP FREQ. + 2 ALEAT.", true) {
                            viewModel.generarPrediccionMaestra()
                        }
                        ActionButton("AJUSTES", Icons.Rounded.Settings, Color(0xFFA78BFA), Modifier.weight(1f)) {
                            // Lógica de ajustes si existiera
                        }
                    }
                }

                // --- ÚLTIMO SORTEO ---
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("ÚLTIMO SORTEO: PRIM", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        when (val state = uiState) {
                            is MainUiState.Success -> ResultCard(state.data)
                            is MainUiState.Error -> ErrorDisplay(state.message) { viewModel.syncWithOfficialWeb() }
                            MainUiState.Loading -> LoadingDisplay()
                        }
                    }
                }

                // --- TENDENCIAS HISTÓRICAS (2 FILAS) ---
                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("NÚMEROS CON MÁS APARICIONES (HISTÓRICO)", color = GoldLuxe.copy(0.7f), fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)

                            // Fila 1 (Top 1-6)
                            TrendRow(stats.take(6), context) {
                                initialManualNumbers = stats.take(6)
                                showManualSelector = true
                            }

                            // Fila 2 (Top 7-12)
                            TrendRow(stats.drop(6).take(6), context) {
                                initialManualNumbers = stats.drop(6).take(6)
                                showManualSelector = true
                            }
                        }
                    }
                }

                // --- HISTORIAL Y ESCRUTINIO ---
                item {
                    Text("HISTORIAL Y ESCRUTINIO", color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp)
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

        // --- DIÁLOGOS ---
        if (confettiTrigger > 0) KonfettiView(modifier = Modifier.fillMaxSize(), parties = listOf(party))
        if (showDeleteDialog && selectedDeleteId != null) {
            DeleteConfirmDialog(onDismiss = { showDeleteDialog = false }, onConfirm = {
                selectedDeleteId?.let { viewModel.deleteEntry(it) }
                showDeleteDialog = false
            })
        }
        if (showTicketReader) {
            TicketReaderDialog(
                onDismiss = { showTicketReader = false },
                onScanManual = { showManualSelector = true; showTicketReader = false },
                onNumbersDetected = { numbers -> initialManualNumbers = numbers; showManualSelector = true; showTicketReader = false }
            )
        }
        if (showManualSelector) {
            ManualEntryDialog(
                initialNumbers = initialManualNumbers,
                frequentNumbers = if (uiState is MainUiState.Success) (uiState as MainUiState.Success).data.frequent else emptyList(),
                onDismiss = { showManualSelector = false },
                onSave = { nombre, n, r -> viewModel.guardarJugadaManual(nombre, n, r) }
            )
        }
        if (showExitDialog) LuxeExitDialog({ showExitDialog = false }, { (context as? Activity)?.finish() })
    }
}

@Composable
fun ResultCard(data: LotteryResult) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(20.dp, spotColor = EmeraldMain),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.1f)))
            NumberBall(data.reintegro.toString(), Color.Red)
        }
    }
}

@Composable
fun TrendRow(numbers: List<Int>, context: android.content.Context, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.04f)),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                numbers.forEach { num ->
                    GoldenBall(num.toString(), onClick = onEditClick)
                }
            }

            IconButton(modifier = Modifier.size(30.dp), onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "🔥 Top números Primitiva: ${numbers.joinToString("-")}")
                }
                context.startActivity(Intent.createChooser(intent, "Compartir"))
            }) {
                Icon(Icons.Rounded.Share, null, tint = EmeraldMain.copy(0.7f), modifier = Modifier.size(16.dp))
            }

            IconButton(modifier = Modifier.size(30.dp), onClick = onEditClick) {
                Icon(Icons.Rounded.ChevronRight, null, tint = GoldLuxe, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CombinationRow(combination: Combination, lastResult: LotteryResult?, onDeleteClick: (Long) -> Unit) {
    val context = LocalContext.current
    val hits = remember(combination, lastResult) { lastResult?.let { res -> combination.numbers.count { it in res.numbers } } ?: 0 }
    val hitReintegro = remember(combination, lastResult) { lastResult?.reintegro == combination.reintegro }
    val isWinner = hits >= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isWinner) EmeraldMain.copy(0.08f) else Color.White.copy(0.02f)),
        border = BorderStroke(1.dp, if (isWinner) EmeraldMain.copy(0.4f) else Color.White.copy(0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(combination.name.uppercase(), color = GoldLuxe, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (combination.date.isNotEmpty()) {
                        Text(" • ${combination.date.split(" ")[0]}", color = Color.White.copy(0.3f), fontSize = 8.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    combination.numbers.forEach { num ->
                        val isHit = lastResult?.numbers?.contains(num) == true
                        Text(num.toString().padStart(2, '0'), color = if (isHit) EmeraldMain else Color.White, fontWeight = if (isHit) FontWeight.Black else FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(if (hitReintegro) Color.Red.copy(0.2f) else Color.Transparent).border(1.dp, if (hitReintegro) Color.Red else EmeraldMain.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Text(combination.reintegro.toString(), color = if (hitReintegro) Color.Red else EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            IconButton(modifier = Modifier.size(30.dp), onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "🍀 Mi jugada: ${combination.numbers.joinToString("-")} [R:${combination.reintegro}]")
                }
                context.startActivity(Intent.createChooser(intent, "Compartir"))
            }) {
                Icon(Icons.Rounded.Share, null, tint = EmeraldMain.copy(0.8f), modifier = Modifier.size(16.dp))
            }

            IconButton(modifier = Modifier.size(30.dp), onClick = { onDeleteClick(combination.id) }) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.15f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TicketReaderDialog(onDismiss: () -> Unit, onScanManual: () -> Unit, onNumbersDetected: (List<Int>) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LECTOR IA 2026", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black)) {
                    if (hasPermission) {
                        AndroidView(factory = { ctx ->
                            PreviewView(ctx).apply {
                                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                                providerFuture.addListener({
                                    val provider = providerFuture.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                                    val analysis = ImageAnalysis.Builder().build().also {
                                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                            proxy.image?.let { img ->
                                                recognizer.process(InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees))
                                                    .addOnSuccessListener { visionText ->
                                                        val nums = visionText.text.split(Regex("[^0-9]+")).mapNotNull { s -> s.toIntOrNull() }.filter { n -> n in 1..49 }.distinct()
                                                        if (nums.size >= 6) onNumbersDetected(nums.take(6).sorted())
                                                    }.addOnCompleteListener { proxy.close() }
                                            } ?: proxy.close()
                                        }
                                    }
                                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                                }, ContextCompat.getMainExecutor(ctx))
                            }
                        }, modifier = Modifier.fillMaxSize())
                    }
                }
                TextButton(onClick = onDismiss) { Text("CERRAR", color = Color.White.copy(0.4f)) }
            }
        }
    }
}

@Composable
fun NumberBall(text: String, color: Color) {
    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

@Composable
fun GoldenBall(num: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(0.3f)).border(1.dp, GoldLuxe.copy(0.4f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(num, color = GoldLuxe, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, detail: String? = null, showInfo: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(85.dp), shape = RoundedCornerShape(24.dp), color = Color.White.copy(0.06f), border = BorderStroke(1.dp, color.copy(0.2f))) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            if (detail != null) Text(detail, color = color.copy(0.5f), fontSize = 7.sp)
        }
    }
}

@Composable
fun LoadingDisplay() {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = EmeraldMain, strokeWidth = 2.dp)
    }
}

@Composable
fun ErrorDisplay(msg: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(msg, color = Color.White.copy(0.5f), textAlign = TextAlign.Center, fontSize = 11.sp)
        TextButton(onClick = onRetry) { Text("REINTENTAR", color = EmeraldMain, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }, title = { Text("¿Eliminar jugada?") }, containerColor = EmeraldSurface, titleContentColor = Color.White, textContentColor = Color.White.copy(0.7f))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(initialNumbers: List<Int>, frequentNumbers: List<Int>, onDismiss: () -> Unit, onSave: (String, List<Int>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialNumbers.toSet()) }
    var reintegro by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface), border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))) {
            Column(modifier = Modifier.padding(24.dp).fillMaxHeight(0.9f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NUEVA JUGADA", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre (Ej: Mi Suerte)", color = Color.White.copy(0.4f)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldMain, unfocusedBorderColor = Color.White.copy(0.1f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (0..5).forEach { i ->
                        val n = selected.toList().sorted().getOrNull(i)
                        Box(modifier = Modifier.padding(4.dp).size(38.dp).clip(CircleShape).background(if (n != null) EmeraldMain else Color.White.copy(0.05f)).border(1.dp, if (n != null) GoldLuxe else Color.White.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(n?.toString() ?: "", color = if (n != null) Color.Black else Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    (1..49).forEach { n ->
                        val isS = selected.contains(n)
                        Box(modifier = Modifier.padding(4.dp).size(36.dp).clip(CircleShape).background(if (isS) EmeraldMain else Color.White.copy(0.05f)).border(1.dp, if (isS) GoldLuxe else Color.White.copy(0.1f), CircleShape).clickable { if (isS) selected -= n else if (selected.size < 6) selected += n }, contentAlignment = Alignment.Center) {
                            Text(n.toString(), color = if (isS) Color.Black else Color.White, fontSize = 12.sp, fontWeight = if (isS) FontWeight.Black else FontWeight.Normal)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text("REINTEGRO: $reintegro", color = GoldLuxe, fontWeight = FontWeight.Bold)
                Slider(value = reintegro.toFloat(), onValueChange = { reintegro = it.toInt() }, valueRange = 0f..9f, steps = 8, colors = SliderDefaults.colors(thumbColor = EmeraldMain, activeTrackColor = EmeraldMain))
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { onSave(name, selected.toList().sorted(), reintegro); onDismiss() }, enabled = selected.size == 6, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain, disabledContainerColor = Color.White.copy(0.1f))) {
                    Text("GUARDAR JUGADA", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun LuxeExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm) { Text("SALIR") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("VOLVER") } }, title = { Text("¿Cerrar App?") }, containerColor = EmeraldSurface, titleContentColor = Color.White)
}