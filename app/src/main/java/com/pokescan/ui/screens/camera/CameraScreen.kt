package com.pokescan.ui.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pokescan.data.api.ApiCard
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onCardFound: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val manualQuery by viewModel.manualQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(uiState) {
        if (uiState is CameraUiState.CardAdded) {
            onCardFound((uiState as CameraUiState.CardAdded).cardId)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Camera preview always in background when scanning
        if (hasCameraPermission && uiState is CameraUiState.Scanning) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onTextDetected = viewModel::onTextDetected,
            )
            ScanningOverlay(modifier = Modifier.fillMaxSize())
        } else if (!hasCameraPermission) {
            NoCameraPermissionContent(
                modifier = Modifier.fillMaxSize(),
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Scan Card",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        // Bottom sheet for search / results
        AnimatedVisibility(
            visible = uiState !is CameraUiState.Scanning,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            BottomResultsSheet(
                uiState = uiState,
                manualQuery = manualQuery,
                onQueryChange = viewModel::setManualQuery,
                onSearch = viewModel::searchCards,
                onAddCard = viewModel::addCard,
                onReset = viewModel::resetToScanning,
            )
        }

        // Manual search FAB when scanning
        if (uiState is CameraUiState.Scanning) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.searchCards(manualQuery.ifBlank { "" }) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("Search Manually") },
                containerColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onTextDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var lastProcessedTime by remember { mutableLongStateOf(0L) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(executor) { imageProxy ->
                            val now = System.currentTimeMillis()
                            // Only process every 2 seconds to avoid spamming
                            if (now - lastProcessedTime > 2000) {
                                lastProcessedTime = now
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees,
                                    )
                                    textRecognizer.process(image)
                                        .addOnSuccessListener { visionText ->
                                            // Use the topmost text block — on a Pokemon card that's
                                            // almost always the card name. Passing the full flat
                                            // visionText.text includes set numbers, flavor text,
                                            // HP values, etc., which confuses cleanOcrText.
                                            val topBlockText = visionText.textBlocks
                                                .filter { it.boundingBox != null }
                                                .minByOrNull { it.boundingBox!!.top }
                                                ?.text
                                                ?: visionText.text
                                            if (topBlockText.isNotBlank()) {
                                                onTextDetected(topBlockText)
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier,
    )
}

@Composable
private fun ScanningOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Semi-transparent borders
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp, 200.dp)
                .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Point camera at a Pokemon card",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                "The card name will be detected automatically",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomResultsSheet(
    uiState: CameraUiState,
    manualQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onAddCard: (ApiCard, Int, String, Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(16.dp))

            when (uiState) {
                is CameraUiState.Processing -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Searching for cards...")
                        }
                    }
                }

                is CameraUiState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(uiState.message, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        ManualSearchRow(
                            query = manualQuery,
                            onQueryChange = onQueryChange,
                            onSearch = onSearch,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onReset) { Text("Scan Again") }
                    }
                }

                is CameraUiState.SearchResults -> {
                    Text(
                        "Found ${uiState.results.size} cards for \"${uiState.query}\"",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    ManualSearchRow(
                        query = manualQuery,
                        onQueryChange = onQueryChange,
                        onSearch = onSearch,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(uiState.results, key = { it.id }) { card ->
                            SearchResultItem(
                                card = card,
                                onAdd = { qty, cond, foil -> onAddCard(card, qty, cond, foil) },
                            )
                            HorizontalDivider()
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Scan Again")
                    }
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun ManualSearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search card name...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = { onSearch(query) },
            modifier = Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SearchResultItem(
    card: ApiCard,
    onAdd: (Int, String, Boolean) -> Unit,
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    var showAddDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showAddDialog = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = card.images.small,
            contentDescription = card.name,
            modifier = Modifier
                .size(56.dp, 78.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(card.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "${card.set.name} · #${card.number}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (card.rarity != null) {
                Text(
                    card.rarity,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            val price = card.tcgplayer?.prices?.normal?.market
                ?: card.tcgplayer?.prices?.holofoil?.market
            if (price != null) {
                Text(
                    currencyFormat.format(price),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        IconButton(onClick = { showAddDialog = true }) {
            Icon(
                Icons.Default.AddCircle,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            cardName = card.name,
            onDismiss = { showAddDialog = false },
            onConfirm = { qty, cond, foil ->
                onAdd(qty, cond, foil)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddCardDialog(
    cardName: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Boolean) -> Unit,
) {
    var quantity by remember { mutableIntStateOf(1) }
    var condition by remember { mutableStateOf("NM") }
    var isFoil by remember { mutableStateOf(false) }
    val conditions = listOf("NM", "LP", "MP", "HP", "DMG")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Card", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                Text(cardName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                // Quantity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity:", modifier = Modifier.width(90.dp))
                    IconButton(onClick = { if (quantity > 1) quantity-- }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    Text(quantity.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { quantity++ }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Condition
                Text("Condition:")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    conditions.forEach { cond ->
                        FilterChip(
                            selected = condition == cond,
                            onClick = { condition = cond },
                            label = { Text(cond, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Foil
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFoil, onCheckedChange = { isFoil = it })
                    Text("Foil / Holo")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(quantity, condition, isFoil) }) { Text("Add to Collection") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun NoCameraPermissionContent(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = Color.White.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Camera permission required",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Grant camera access to scan Pokemon cards",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("Grant Permission") }
    }
}
