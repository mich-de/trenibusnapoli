package com.eavorari.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eavorari.data.model.TrainInfo
import com.eavorari.data.model.UiState
import com.eavorari.ui.theme.*
import com.eavorari.ui.components.RetroIndicator
import androidx.compose.ui.graphics.Brush
import com.eavorari.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    stationName: String,
    initialTipo: String,
    onBack: () -> Unit,
) {
    val scheduleState by viewModel.scheduleState.collectAsState()
    val filteredTrains by viewModel.filteredTrains.collectAsState()
    val destinationFilter by viewModel.destinationFilter.collectAsState()
    val currentTipo by viewModel.tipo.collectAsState()

    BackHandler(enabled = destinationFilter != null) {
        viewModel.setDestinationFilter(null)
        viewModel.applyFilter()
    }

    var currentTime by remember { mutableStateOf("") }
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }

    LaunchedEffect(Unit) {
        viewModel.setTipo(initialTipo)
        viewModel.loadSchedule()
        viewModel.startAutoRefresh()
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopAutoRefresh() }
    }

    val switchTipo: () -> Unit = {
        val next = if (currentTipo == "P") "A" else "P"
        viewModel.setTipo(next)
        viewModel.loadSchedule()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopBar(
                onBack = onBack,
                currentTime = currentTime,
                onRefresh = { viewModel.loadSchedule() },
            )

            when (val state = scheduleState) {
                is UiState.Initial, is UiState.Loading -> {
                    LoadingBoard()
                }
                is UiState.Error -> {
                    ErrorBoard(message = state.message, onRetry = { viewModel.loadSchedule() })
                }
                is UiState.Success -> {
                    BoardContent(
                        stationName = stationName,
                        tipo = currentTipo,
                        trains = filteredTrains,
                        destinationFilter = destinationFilter,
                        onFilterByDestination = { dest ->
                            viewModel.setDestinationFilter(dest)
                            viewModel.applyFilter()
                        },
                        onClearFilter = {
                            viewModel.setDestinationFilter(null)
                            viewModel.applyFilter()
                        },
                        timestamp = state.timestamp,
                        onTipoChange = switchTipo,
                    )
                }
            }
        }

        ScanLineOverlay()
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    currentTime: String,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = Amber,
            )
        }

        Text(
            text = currentTime,
            style = ClockText,
            color = Amber,
            modifier = Modifier.padding(start = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "EAV",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 3.sp,
            ),
            color = TextDim,
        )

        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Aggiorna",
                tint = TextSecondary,
            )
        }
    }
}

@Composable
private fun LoadingBoard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Amber,
                strokeWidth = 2.dp,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "CARICAMENTO ORARIO...",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                ),
                color = TextDim,
            )
        }
    }
}

@Composable
private fun ErrorBoard(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ERRORE",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 4.sp,
                ),
                color = Red,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = TextStyle(
                    fontSize = 13.sp,
                ),
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber,
                    contentColor = Background,
                ),
                shape = RoundedCornerShape(2.dp),
            ) {
                Text(
                    "RIPROVA",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BoardContent(
    stationName: String,
    tipo: String,
    trains: List<TrainInfo>,
    destinationFilter: String?,
    onFilterByDestination: (String) -> Unit,
    onClearFilter: () -> Unit,
    timestamp: Long,
    onTipoChange: () -> Unit,
) {
    val tipoLabel = if (tipo == "P") "PARTENZE" else "ARRIVI"
    val tipoSub = if (tipo == "P") "Departures" else "Arrivals"

    Column(modifier = Modifier.fillMaxSize()) {
        BoardHeader(
            stationName = stationName,
            tipoLabel = tipoLabel,
            tipoSub = tipoSub,
            tipo = tipo,
            onTipoChange = onTipoChange,
        )

        AnimatedVisibility(
            visible = destinationFilter != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            destinationFilter?.let {
                FilterChip(
                    label = it,
                    onClear = onClearFilter,
                )
            }
        }

        BoardColumnHeaders()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            itemsIndexed(trains, key = { _, train -> "${train.trainNumber}_${train.time}" }) { index, train ->
                TrainBoardCard(
                    train = train,
                    index = index,
                    onDestinationClick = { dest ->
                        if (destinationFilter != dest) {
                            onFilterByDestination(dest)
                        } else {
                            onClearFilter()
                        }
                    },
                )
            }

            if (trains.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "NESSUN TRENO DISPONIBILE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = TextDim,
                        )
                    }
                }
            }
        }

        BoardFooter(timestamp = timestamp)
    }
}

@Composable
private fun BoardHeader(
    stationName: String,
    tipoLabel: String,
    tipoSub: String,
    tipo: String,
    onTipoChange: () -> Unit,
) {
    val blink = rememberInfiniteTransition(label = "blink")
    val visible by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )

    val legendText = if (tipo == "P") {
        "Treni in PARTENZA da $stationName — verso le loro destinazioni"
    } else {
        "Treni in ARRIVO a $stationName — provenienti dalla loro origine"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BoardRowDark)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stationName,
            style = StationName,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        if (visible > 0.5f) Green else Green.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp),
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Divider.copy(alpha = 0.3f)),
            ) {
                TipoToggle(
                    label = "P",
                    full = "PARTENZE",
                    sub = "via da",
                    isActive = tipo == "P",
                    onClick = { if (tipo != "P") onTipoChange() },
                )
                TipoToggle(
                    label = "A",
                    full = "ARRIVI",
                    sub = "verso",
                    isActive = tipo == "A",
                    onClick = { if (tipo != "A") onTipoChange() },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = legendText,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            ),
            color = TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            text = tipoSub,
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
            color = TextDim.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun TipoToggle(
    label: String,
    full: String,
    sub: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isActive) Amber.copy(alpha = 0.2f) else Color.Transparent
    val fg = if (isActive) Amber else TextDim

    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
            ),
            color = fg,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = full,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                ),
                color = fg,
            )
            Text(
                text = sub,
                style = TextStyle(
                    fontSize = 8.sp,
                    letterSpacing = 1.sp,
                ),
                color = if (isActive) Amber.copy(alpha = 0.6f) else TextDim.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun BoardColumnHeaders() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BoardHeader.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            "TRENO",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
            color = BoardHeader,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            "DESTINAZIONE",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
            color = BoardHeader,
            modifier = Modifier.weight(2f),
        )
        Text(
            "BIN",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
            color = BoardHeader,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center,
        )
        Text(
            "ORA",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
            color = BoardHeader,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
        )
        Text(
            "RIT",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
            ),
            color = BoardHeader,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun TrainBoardCard(
    train: TrainInfo,
    index: Int,
    onDestinationClick: (String) -> Unit,
) {
    val hasDelay = train.delay.isNotBlank()
    val delayColor = if (hasDelay) Red else TextDim

    val bgColor = if (index % 2 == 0) BoardRowDark else BoardRowLight

    val infiniteTransition = rememberInfiniteTransition(label = "blinker")
    val blinkVisible by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinker",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Divider.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RetroIndicator(
            isActive = blinkVisible > 0.5f,
            color = if (hasDelay) Red else Green
        )
        Spacer(Modifier.width(8.dp))

        Text(
            train.trainNumber,
            style = TrainNumber,
            color = Amber,
            modifier = Modifier.weight(1.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            train.destination,
            style = TrainDestination,
            color = TextPrimary,
            modifier = Modifier
                .weight(2f)
                .clickable { onDestinationClick(train.destination) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            train.platform.ifBlank { "-" },
            style = TrainNumber.copy(fontSize = 18.sp),
            color = TextDim,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Center,
        )

        Text(
            train.time,
            style = TrainTime,
            color = Amber,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.Center,
        )

        Text(
            train.delay.ifBlank { "" },
            style = TrainDelay.copy(color = delayColor),
            color = delayColor,
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Cyan.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "FILTRO: $label",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            ),
            color = Cyan,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "× tocca destinazione per rimuovere",
            style = TextStyle(
                fontSize = 10.sp,
            ),
            color = TextDim,
            modifier = Modifier.clickable(onClick = onClear),
        )
    }
}

@Composable
private fun BoardFooter(timestamp: Long) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.ITALY) }
    val timeStr = remember(timestamp) { sdf.format(Date(timestamp)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "⟳ AGGIORNATO $timeStr",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
            color = TextDim,
        )
        Text(
            "EAV · TELECOMANDO",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
            color = TextDim,
        )
    }
}

@Composable
private fun ScanLineOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanlineY",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineH = 1.dp.toPx()
        val spacing = 4.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = Color.Black.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = lineH,
            )
            y += spacing + lineH
        }

        val beamY = yOffset * size.height
        val beamHeight = 45.dp.toPx()
        
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0f),
                    Color.White.copy(alpha = 0.015f),
                    Color.White.copy(alpha = 0f),
                ),
                startY = beamY - beamHeight / 2,
                endY = beamY + beamHeight / 2
            ),
            topLeft = Offset(0f, beamY - beamHeight / 2),
            size = androidx.compose.ui.geometry.Size(size.width, beamHeight)
        )
    }
}


