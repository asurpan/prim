package com.sagon.prim

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
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
                            Text("PRIMITIVA PRIM", 
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black, 
                                    letterSpacing = (-1.5).sp
                                ), color = EmeraldMain)
                            Text("SINCRO LUXE PLATINUM V17", 
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
                        ActionButton("PREDECIR", Icons.Rounded.AutoAwesome, GoldLuxe, Modifier.weight(1.3f), "4 ÉXITOS + 2 AZAR", true, { showInfoDialog = true }) {
                            viewModel.generarPrediccionMaestra()
                        }
                        ActionButton("AJUSTES", Icons.Rounded.Settings, Color(0xFFA78BFA), Modifier.weight(1f)) {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        }
                    }
                }

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
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
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

                item {
                    if (uiState is MainUiState.Success) {
                        val stats = (uiState as MainUiState.Success).data.frequent
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NÚMEROS CON MÁS APARICIONES", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                                TextButton(onClick = {
                                    initialManualNumbers = stats.take(6)
                                    showManualSelector = true
                                }) {
                                    Text("JUGAR TOP 6", color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Icon(Icons.Rounded.ChevronRight, null, tint = GoldLuxe, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                stats.take(12).forEach { num ->
                                    GoldenBall(num.toString()) {
                                        initialManualNumbers = listOf(num)
                                        showManualSelector = true
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text("JUGADAS GUARDADAS Y ESCRUTINIO", color = Color.White.copy(0.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp))
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
fun CombinationRow(combination: Combination, lastResult: LotteryResult?, onDeleteClick: (Long) -> Unit) {
    val context = LocalContext.current
    
    val hits = remember(combination, lastResult) {
        lastResult?.let { res ->
            combination.numbers.count { it in res.numbers }
        } ?: 0
    }
    val hitReintegro = remember(combination, lastResult) {
        lastResult?.reintegro == combination.reintegro
    }
    
    val isWinner = hits >= 3

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isWinner) 12.dp else 0.dp, spotColor = EmeraldMain),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) EmeraldMain.copy(0.08f) else Color.White.copy(0.03f)
        ),
        border = BorderStroke(
            1.dp, 
            if (isWinner) EmeraldMain.copy(0.6f) else Color.White.copy(0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(combination.name, color = GoldLuxe, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
                    
                    if (lastResult != null) {
                        Surface(
                            color = if (isWinner) EmeraldMain else Color.White.copy(0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isWinner) "★ PREMIO ★" else "$hits ACIERTOS",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    combination.numbers.forEach { num ->
                        val isHit = lastResult?.numbers?.contains(num) == true
                        Text(
                            num.toString().padStart(2, '0'), 
                            color = if (isHit) EmeraldMain else Color.White, 
                            fontSize = 17.sp, 
                            fontWeight = if (isHit) FontWeight.Black else FontWeight.Bold,
                            modifier = if (isHit) Modifier.scale(1.1f) else Modifier
                        )
                    }
                }
                
                if (isWinner) {
                    Text(
                        text = "¡FELICIDADES! HAS ACERTADO $hits NÚMEROS",
                        color = EmeraldMain,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Reintegro Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R", color = Color.White.copy(0.3f), fontSize = 8.sp, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (hitReintegro) Color.Red.copy(0.2f) else EmeraldMain.copy(0.05f))
                        .border(1.dp, if (hitReintegro) Color.Red else EmeraldMain.copy(0.2f), CircleShape), 
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        combination.reintegro.toString(), 
                        color = if (hitReintegro) Color.Red else EmeraldMain, 
                        fontWeight = FontWeight.Black, 
                        fontSize = 14.sp
                    )
                }
            }

            // Actions
            Column {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, "¡Mi jugada Primitiva!: ${combination.numbers.joinToString("-")} | R: ${combination.reintegro}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Share, null, tint = EmeraldMain, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onDeleteClick(combination.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.White.copy(0.2f), modifier = Modifier.size(16.dp))
                }
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

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldSurface),
            border = BorderStroke(1.dp, PinkLuxe.copy(0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.QrCodeScanner, null, tint = PinkLuxe, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("LECTOR OPTIQUE V17", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Escanee el área de los números del boleto", 
                    color = Color.White.copy(0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(2.dp, PinkLuxe.copy(0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                        if (isProcessing) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                            recognizer.process(image)
                                                .addOnSuccessListener { visionText ->
                                                    val foundNumbers = visionText.text
                                                        .split(Regex("[^0-9]+"))
                                                        .mapNotNull { it.toIntOrNull() }
                                                        .filter { it in 1..49 }
                                                        .distinct()

                                                    if (foundNumbers.size >= 6) {
                                                        isProcessing = true
                                                        onNumbersDetected(foundNumbers.take(6).sorted())
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                    
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (e: Exception) {
                                        Log.e("CameraX", "Binding failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Overlay de escaneo
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 2.dp.toPx()
                            val color = PinkLuxe
                            val pad = 40.dp.toPx()
                            
                            drawLine(color, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Offset(pad + 50f, pad), strokeWidth * 2)
                            drawLine(color, androidx.compose.ui.geometry.Offset(pad, pad), androidx.compose.ui.geometry.Offset(pad, pad + 50f), strokeWidth * 2)
                            
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width - pad, pad), androidx.compose.ui.geometry.Offset(size.width - pad - 50f, pad), strokeWidth * 2)
                            drawLine(color, androidx.compose.ui.geometry.Offset(size.width - pad, pad), androidx.compose.ui.geometry.Offset(size.width - pad, pad + 50f), strokeWidth * 2)
                        }
                        
                        val infiniteTransition = rememberInfiniteTransition()
                        val scanLineY by infiniteTransition.animateFloat(
                            initialValue = 0.1f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 280.dp * scanLineY)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, PinkLuxe, Color.Transparent)))
                        )
                        
                        if (isProcessing) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EmeraldMain)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PinkLuxe)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Esperando permisos...", color = Color.White.copy(0.5f), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = onScanManual,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ENTRADA MANUAL", fontWeight = FontWeight.Bold)
                }
                
                TextButton(onClick = onDismiss) {
                    Text("CANCELAR", color = Color.White.copy(0.5f))
                }
            }
        }
    }
}

@Composable
fun NumberBall(number: String, color: Color) {
    Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
        Text(number, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun GoldenBall(num: String, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "gold")
    val glowScale by infiniteTransition.animateFloat(1f, 1.12f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(glowScale)
            .clip(CircleShape)
            .background(Color.Black.copy(0.4f))
            .border(1.dp, GoldLuxe.copy(0.5f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(num, color = GoldLuxe, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ActionButton(text: String, icon: ImageVector, color: Color, modifier: Modifier, detail: String? = null, showInfo: Boolean = false, onInfoClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(105.dp), shape = RoundedCornerShape(28.dp), color = Color.White.copy(0.06f), border = BorderStroke(1.dp, color.copy(0.3f))) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showInfo && onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = color.copy(0.5f), modifier = Modifier.size(16.dp))
                }
            }
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp)
                if (detail != null) Text(detail, color = color.copy(0.8f), fontWeight = FontWeight.Bold, fontSize = 7.5.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface), border = BorderStroke(1.dp, Color.Red.copy(0.3f))) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.DeleteForever, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿ELIMINAR JUGADA?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Esta acción no se puede deshacer.", color = Color.White.copy(0.6f), fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("CANCELAR", color = Color.White) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Red), shape = RoundedCornerShape(16.dp)) {
                        Text("ELIMINAR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualEntryDialog(initialNumbers: List<Int>, frequentNumbers: List<Int>, onDismiss: () -> Unit, onSave: (String, List<Int>, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedNums by remember { mutableStateOf(initialNumbers.toSet()) }
    var selectedR by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface), border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NUEVA JUGADA LUXE", color = EmeraldMain, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 15) name = it },
                    placeholder = { Text("Nombre de la jugada...", color = Color.White.copy(0.3f), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldMain,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        cursorColor = EmeraldMain
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (frequentNumbers.isNotEmpty()) {
                    Button(
                        onClick = {
                            val needed = 6 - selectedNums.size
                            if (needed > 0) {
                                val toAdd = frequentNumbers.filter { it !in selectedNums }.take(needed)
                                selectedNums = selectedNums + toAdd
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldLuxe.copy(0.1f)),
                        border = BorderStroke(1.dp, GoldLuxe.copy(0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = GoldLuxe, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUTO-COMPLETAR ORO", color = GoldLuxe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (1..49).forEach { n ->
                        val isSel = selectedNums.contains(n)
                        Surface(onClick = { if (isSel) selectedNums -= n else if (selectedNums.size < 6) selectedNums += n }, modifier = Modifier.padding(4.dp).size(36.dp), shape = CircleShape, color = if (isSel) EmeraldMain else Color.Black.copy(0.3f), border = BorderStroke(1.dp, if (isSel) Color.White else EmeraldMain.copy(0.2f))) { Box(contentAlignment = Alignment.Center) { Text(n.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("REINTEGRO", color = EmeraldMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..9).forEach { r ->
                        val isSel = selectedR == r
                        Surface(onClick = { selectedR = r }, modifier = Modifier.size(30.dp), shape = CircleShape, color = if (isSel) Color.Red else Color.Black.copy(0.3f), border = BorderStroke(1.dp, if (isSel) Color.White else Color.Red.copy(0.2f))) { Box(contentAlignment = Alignment.Center) { Text(r.toString(), color = Color.White, fontSize = 10.sp) } }
                    }
                }
                Button(onClick = { onSave(name, selectedNums.toList(), selectedR); onDismiss() }, enabled = selectedNums.size == 6, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain)) {
                    Text("GUARDAR EN HISTORIAL", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun AlgorithmHelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface), border = BorderStroke(1.dp, GoldLuxe.copy(0.3f))) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = GoldLuxe, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("FÓRMULA DE ÉXITO", color = GoldLuxe, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 2.sp)
                Text("Combinamos los 4 números con más apariciones históricas (ORO) con 2 números al azar.", color = Color.White.copy(0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("ENTENDIDO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LuxeExitDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(containerColor = EmeraldSurface), border = BorderStroke(1.dp, EmeraldMain.copy(0.3f))) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Logout, null, tint = EmeraldMain, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿DESEA SALIR?", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("La sincronización se detendrá.", color = Color.White.copy(0.6f), fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("CANCELAR", color = Color.White) }
                    Button(onClick = onConfirm, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = EmeraldMain), shape = RoundedCornerShape(16.dp)) {
                        Text("SALIR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
