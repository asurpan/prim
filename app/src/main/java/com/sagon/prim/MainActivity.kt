package com.sagon.prim

import android.Manifest
import android.app.Activity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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

// --- CONFIGURACIÓN DE COLOR LUXE PLATINUM ---
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
                            Text("PRIMITIVA",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp
                                ), color = EmeraldMain)
                            Text("SINCRO OFICIAL 2026",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 4.sp,
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
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionButton("LECTOR", Icons.Rounded.QrCodeScanner, PinkLuxe, Modifier.weight(1f)) {
                            showTicketReader = true
                        }
                        ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1.3f), "ESTRATEGIA 2026", true, { showInfoDialog = true }) {
                            viewModel.generarPrediccionMaestra()
                        }
                        ActionButton("NUEVA", Icons.Rounded.Add, EmeraldMain, Modifier.weight(1f)) {
                            initialManualNumbers = emptyList()
                            showManualSelector = true
                        }
                    }
                }

                item {
                    when (val state = uiState) {
                        is MainUiState.Success -> ResultCard(state.data)
                        is MainUiState.Error -> ErrorDisplay(state.message) { viewModel.syncWithOfficialWeb() }
                        MainUiState.Loading -> LoadingDisplay()
                    }
                }

                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TENDENCIAS 2026", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                                TextButton(onClick = {
                                    initialManualNumbers = stats.take(6)
                                    showManualSelector = true
                                }) {
                                    Text("JUGAR TOP 6", color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Icon(Icons.Rounded.ChevronRight, null, tint = GoldLuxe, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 6) {
                                stats.take(12).forEach { num ->
                                    GoldenBall(num.toString()) {
                                        if (initialManualNumbers.size < 6 && num !in initialManualNumbers) {
                                            initialManualNumbers = (initialManualNumbers + num).sorted()
                                            showManualSelector = true
                                        } else if (num in initialManualNumbers) {
                                            showManualSelector = true
                                        } else {
                                            initialManualNumbers = listOf(num)
                                            showManualSelector = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("JUGADAS GUARDADAS", color = Color.White.copy(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp))
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

        if (showInfoDialog) AlgorithmHelpDialog { showInfoDialog = false }

        if (showDeleteDialog && selectedDeleteId != null) {
            DeleteConfirmDialog(onDismiss = { showDeleteDialog = false }, onConfirm = {
                selectedDeleteId?.let { viewModel.deleteEntry(it) }
                showDeleteDialog = false
            })
        }

        if (showTicketReader) {
            TicketReaderDialog(
                onDismiss = { showTicketReader = false },
                onScanManual = {
                    initialManualNumbers = emptyList()
                    showManualSelector = true
                    showTicketReader = false
                },
                onNumbersDetected = { numbers ->
                    initialManualNumbers = numbers
                    showManualSelector = true
                    showTicketReader = false
                    Toast.makeText(context, "Boleto detectado con éxito", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showManualSelector) {
            val frequentList = if (uiState is MainUiState.Success) (uiState as MainUiState.Success).data.frequent else emptyList()
            ManualEntryDialog(
                initialNumbers = initialManualNumbers,
                frequentNumbers = frequentList,
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
        modifier = Modifier.fillMaxWidth().shadow(30.dp, spotColor = EmeraldMain),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
        border = BorderStroke(1.dp, EmeraldMain.copy(0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(data.date, color = EmeraldMain, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                data.numbers.forEach { NumberBall(it.toString(), EmeraldMain) }
                Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(0.1f)))
                NumberBall(data.reintegro.toString(), Color.Red)
            }
        }
    }
}

@Composable
fun CombinationRow(combination: Combination, lastResult: LotteryResult?, onDeleteClick: (Long) -> Unit) {
    val hits = remember(combination, lastResult) { lastResult?.let { res -> combination.numbers.count { it in res.numbers } } ?: 0 }
    val hitReintegro = remember(combination, lastResult) { lastResult?.reintegro == combination.reintegro }
    val isWinner = hits >= 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isWinner) EmeraldMain.copy(0.1f) else Color.White.copy(0.03f)),
        border = BorderStroke(1.dp, if (isWinner) EmeraldMain.copy(0.5f) else Color.White.copy(0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(combination.name, color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    if (combination.date.isNotEmpty()) {
                        Text(" • ${combination.date}", color = Color.White.copy(0.4f), fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    combination.numbers.forEach { num ->
                        val isHit = lastResult?.numbers?.contains(num) == true
                        Text(num.toString().padStart(2, '0'), color = if (isHit) EmeraldMain else Color.White, fontWeight = if (isHit) FontWeight.Black else FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (hitReintegro) Color.Red.copy(0.2f) else Color.Transparent).border(1.dp, if (hitReintegro) Color.Red else EmeraldMain.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Text(combination.reintegro.toString(), color = if (hitReintegro) Color.Red else EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            IconButton(onClick = { onDeleteClick(combination.id) }) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.1f))
            }
        }
    }
}

@Composable
fun TicketReaderDialog(onDismiss: () -> Unit, onScanManual: () -> Unit, onNumbersDetected: (List<Int>) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var isProcessing by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = { granted -> hasCameraPermission = granted })
    LaunchedEffect(Unit) { if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = PinkLuxe, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("LECTOR DE BOLETOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    if (hasCameraPermission) {
                        AndroidView(factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                    if (isProcessing) { imageProxy.close(); return@setAnalyzer }
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        recognizer.process(image).addOnSuccessListener { visionText ->
                                            val foundNumbers = visionText.text.split(Regex("[^0-9]+")).mapNotNull { it.toIntOrNull() }.filter { it in 1..49 }.distinct()
                                            if (foundNumbers.size >= 6) { isProcessing = true; onNumbersDetected(foundNumbers.take(6).sorted()) }
                                        }.addOnCompleteListener { imageProxy.close() }
                                    } else imageProxy.close()
                                }
                                try { cameraProvider.unbindAll(); cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis) } catch (e: Exception) { Log.e("CameraX", "Binding failed", e) }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        }, modifier = Modifier.fillMaxSize())
                    } else Text("Permiso de cámara necesario", color = Color.White)
                }
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.White.copy(0.5f)) }
            }
        }
    }
}

@Composable
fun NumberBall(text: String, color: Color) {
    Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun GoldenBall(num: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(0.4f)).border(1.dp, GoldLuxe.copy(0.5f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(num, color = GoldLuxe, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, detail: String? = null, showInfo: Boolean = false, onInfoClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(90.dp), shape = RoundedCornerShape(28.dp), color = Color.White.copy(0.06f), border = BorderStroke(1.dp, color.copy(0.3f))) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
            if (detail != null) Text(detail, color = color.copy(0.6f), fontSize = 8.sp)
        }
    }
}

@Composable
fun LoadingDisplay() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(color = EmeraldMain)
        Text("Sincronizando 2026...", color = Color.White.copy(0.4f), fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun ErrorDisplay(msg: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(msg, color = Color.White.copy(0.6f), textAlign = TextAlign.Center, fontSize = 12.sp)
        TextButton(onClick = onRetry) { Text("REINTENTAR", color = EmeraldMain) }
    }
}

@Composable
fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("ELIMINAR") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }, title = { Text("¿Eliminar jugada?") })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(initialNumbers: List<Int>, frequentNumbers: List<Int>, onDismiss: () -> Unit, onSave: (String, List<Int>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(initialNumbers.toSet()) }
    var reintegro by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
            border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxHeight(0.9f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CONFIGURAR JUGADA", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la jugada", color = Color.White.copy(0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldMain,
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Panel de números seleccionados
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (0..5).forEach { index ->
                        val num = selected.toList().sorted().getOrNull(index)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (num != null) EmeraldMain else Color.White.copy(0.05f))
                                .border(1.dp, if (num != null) GoldLuxe else Color.White.copy(0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num?.toString() ?: "",
                                color = if (num != null) Color.Black else Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (frequentNumbers.isNotEmpty()) {
                    Text("TENDENCIAS (Toca para añadir)", color = EmeraldMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        frequentNumbers.take(12).forEach { n ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selected.contains(n)) EmeraldMain else GoldLuxe.copy(0.1f))
                                    .border(1.dp, if (selected.contains(n)) GoldLuxe else GoldLuxe.copy(0.3f), CircleShape)
                                    .clickable {
                                        if (selected.contains(n)) selected -= n
                                        else if (selected.size < 6) selected += n
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(n.toString(), color = if (selected.contains(n)) Color.Black else GoldLuxe, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Text("SELECCIÓN MANUAL", color = Color.White.copy(0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..49).forEach { n ->
                        val isSelected = selected.contains(n)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EmeraldMain else Color.White.copy(0.05f))
                                .border(1.dp, if (isSelected) GoldLuxe else Color.White.copy(0.1f), CircleShape)
                                .clickable {
                                    if (isSelected) selected -= n
                                    else if (selected.size < 6) selected += n
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                n.toString(),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text("REINTEGRO: $reintegro", color = GoldLuxe, fontWeight = FontWeight.Black)
                Slider(
                    value = reintegro.toFloat(),
                    onValueChange = { reintegro = it.toInt() },
                    valueRange = 0f..9f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldMain,
                        activeTrackColor = EmeraldMain,
                        inactiveTrackColor = Color.White.copy(0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onSave(name, selected.toList().sorted(), reintegro); onDismiss() },
                    enabled = selected.size == 6,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain, disabledContainerColor = Color.White.copy(0.1f))
                ) {
                    Text("GUARDAR COMBINACIÓN", fontWeight = FontWeight.Black)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("CANCELAR", color = Color.White.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun AlgorithmHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onDismiss) { Text("OK") } }, title = { Text("Estrategia 2026") }, text = { Text("Analizamos las tendencias reales de Abril 2026 para sugerir números con mayor probabilidad.") })
}

@Composable
fun LuxeExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onConfirm) { Text("SALIR") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }, title = { Text("¿Salir?") })
}