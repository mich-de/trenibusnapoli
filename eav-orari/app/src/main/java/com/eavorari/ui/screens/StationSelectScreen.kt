package com.eavorari.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.eavorari.data.model.UiState
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eavorari.data.EavRepository
import com.eavorari.data.model.BusArea
import com.eavorari.data.model.BusLine
import com.eavorari.data.model.ExtraBusRoute
import com.eavorari.data.model.MetroLine
import com.eavorari.data.model.MetroStop
import com.eavorari.data.model.Station
import com.eavorari.data.model.StationsState
import com.eavorari.data.model.StrikeInfo
import com.eavorari.data.model.TransportMode
import com.eavorari.ui.theme.*
import com.eavorari.viewmodel.ScheduleViewModel

private val peninsulaBusAreas = setOf("Costiera Sorrentina")
private val peninsulaNightAreas = setOf("Notturni Vesuviani", "Info Notturni")

@Composable
fun StationSelectScreen(
    viewModel: ScheduleViewModel,
    onNavigate: (Station, String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val stationsState by viewModel.stationsState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(TransportMode.TRAIN) }

    val stations = when (val state = stationsState) {
        is StationsState.Success -> state.stations
        else -> emptyList()
    }

    val filteredStations = remember(stations, searchQuery) {
        if (searchQuery.isBlank()) stations
        else stations.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val repository = remember { EavRepository() }
    val metroLines = remember { repository.getMetroLines() }

    val prefs = com.eavorari.data.SecurePrefs.get(context)
    val savedStationName = prefs.getString("station_name", "") ?: ""
    val peninsulaEnabled = prefs.getBoolean("peninsula_enabled", savedStationName.isNotBlank())
    val isPeninsulaMode = savedStationName.isNotBlank() && peninsulaEnabled

    val allBusAreas = remember { repository.getBusAreas() }
    val allNightBusAreas = remember { repository.getNightBusAreas() }
    val allSitaAreas = remember { repository.getSitaAreas() }
    val allExtraRoutes = remember { repository.getExtraBusRoutes() }

    val busAreas = remember(allBusAreas, isPeninsulaMode) {
        if (isPeninsulaMode) allBusAreas.filter { it.name in peninsulaBusAreas }
        else allBusAreas
    }
    val sitaAreas = remember(allSitaAreas, isPeninsulaMode) {
        if (isPeninsulaMode) allSitaAreas
        else allSitaAreas
    }
    val extraBusRoutes = remember(allExtraRoutes, isPeninsulaMode) {
        if (isPeninsulaMode) allExtraRoutes
        else allExtraRoutes
    }
    val nightBusAreas = remember(allNightBusAreas, isPeninsulaMode) {
        if (isPeninsulaMode) allNightBusAreas.filter { it.name in peninsulaNightAreas }
        else allNightBusAreas
    }

    val strikes by viewModel.strikes.collectAsState()
    val strikesLoading by viewModel.strikesLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Spacer(Modifier.height(8.dp))

            HeaderSection(onNavigateToSettings = onNavigateToSettings)

            Spacer(Modifier.height(16.dp))

            ModeTabs(
                selectedMode = selectedMode,
                onModeSelected = { selectedMode = it },
            )

            Spacer(Modifier.height(16.dp))

            when (selectedMode) {
                TransportMode.TRAIN -> TrainSection(
                    stationsState = stationsState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    filteredStations = filteredStations,
                    onNavigate = onNavigate,
                    onRetry = { viewModel.loadStations() },
                )
                TransportMode.BUS -> BusSection(
                    busAreas = busAreas + sitaAreas,
                    extraRoutes = extraBusRoutes,
                )
                TransportMode.NIGHT_BUS -> BusSection(busAreas = nightBusAreas)
                TransportMode.STRIKE -> StrikeSection(
                    strikes = strikes,
                    isLoading = strikesLoading,
                    onRefresh = { viewModel.fetchStrikes() },
                )
                TransportMode.METRO -> MetroSection(metroLines = metroLines)
            }

            Spacer(Modifier.height(8.dp))
            DataFreshnessFooter(
                strikeTimestamp = viewModel.strikeTimestamp.collectAsState().value,
                selectedMode = selectedMode,
                scheduleState = viewModel.scheduleState.collectAsState().value,
            )
        }
    }
}

@Composable
private fun HeaderSection(onNavigateToSettings: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                tint = Amber.copy(alpha = pulseAlpha),
                modifier = Modifier.size(42.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "TRASPORTI",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 6.sp,
                ),
                color = Amber,
            )
            Text(
                text = "penisola sorrentina e oltre",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                ),
                color = TextDim,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Card)
                .clickable(onClick = onNavigateToSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Impostazioni",
                tint = TextDim,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ModeTabs(
    selectedMode: TransportMode,
    onModeSelected: (TransportMode) -> Unit,
) {
    val tabHeight = 50.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(tabHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceVariant),
    ) {
        TransportMode.entries.forEach { mode ->
            val isSelected = mode == selectedMode
            val accent = when (mode) {
                TransportMode.TRAIN -> Amber
                TransportMode.BUS -> Cyan
                TransportMode.NIGHT_BUS -> Color(0xFF7C3AED)
                TransportMode.STRIKE -> Red
                TransportMode.METRO -> Color(0xFFFFD700)
            }
            val targetBgColor = if (isSelected) accent else Color.Transparent
            val targetTextColor = if (isSelected) Background else TextDim

            val animatedBgColor by animateColorAsState(
                targetValue = targetBgColor,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "tabBg"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = targetTextColor,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(animatedBgColor)
                    .clickable(onClick = { onModeSelected(mode) }),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mode.label,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = animatedTextColor,
                    )
                    if (isSelected) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(animatedTextColor, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainSection(
    stationsState: StationsState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredStations: List<Station>,
    onNavigate: (Station, String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        SearchField(query = searchQuery, onQueryChange = onSearchQueryChange)
        Spacer(Modifier.height(12.dp))

        when (stationsState) {
            is StationsState.Loading -> LoadingIndicator()
            is StationsState.Error -> ErrorSection(onRetry = onRetry)
            is StationsState.Success -> {
                StationsList(
                    stations = filteredStations,
                    onNavigateDepartures = { onNavigate(it, "P") },
                    onNavigateArrivals = { onNavigate(it, "A") },
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val borderColor = if (query.isNotBlank()) Amber.copy(alpha = 0.6f) else Divider

    Column {
        Text(
            text = "DOVE SEI?",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
            ),
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Cerca stazione...",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    color = TextDim,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Amber,
                unfocusedTextColor = TextPrimary,
                cursorColor = Amber,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Card,
                unfocusedContainerColor = Card,
            ),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, letterSpacing = 1.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(),
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StationsList(
    stations: List<Station>,
    onNavigateDepartures: (Station) -> Unit,
    onNavigateArrivals: (Station) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        items(stations, key = { it.id }) { station ->
            Box(modifier = Modifier.animateItemPlacement()) {
                StationItem(
                    station = station,
                    onDepartures = { onNavigateDepartures(station) },
                    onArrivals = { onNavigateArrivals(station) },
                )
            }
        }
    }
}

@Composable
private fun StationItem(
    station: Station,
    onDepartures: () -> Unit,
    onArrivals: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable(onClick = onDepartures)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(Amber.copy(alpha = 0.4f), RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp),
                color = TextPrimary,
            )
            if (station.lineName.isNotBlank()) {
                Text(
                    text = station.lineName,
                    style = TextStyle(fontSize = 10.sp, letterSpacing = 0.5.sp),
                    color = TextDim,
                )
            }
        }

        ActionButton("P", Amber) { onDepartures() }
        Spacer(Modifier.width(4.dp))
        ActionButton("A", Cyan) { onArrivals() }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun ActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = color,
        )
    }
}

@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Amber, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                "CARICAMENTO...",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 3.sp),
                color = TextDim,
            )
        }
    }
}

@Composable
private fun ErrorSection(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ERRORE CONNESSIONE",
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.sp),
                color = Red,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Background),
                shape = RoundedCornerShape(2.dp),
            ) {
                Text("RIPROVA", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp))
            }
        }
    }
}

@Composable
private fun BusSection(
    busAreas: List<BusArea>,
    extraRoutes: List<ExtraBusRoute> = emptyList(),
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(busAreas) { area ->
            BusAreaCard(
                area = area,
                onLineClick = { line ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(line.pdfUrl))
                    context.startActivity(intent)
                },
            )
        }

        if (extraRoutes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "AUTOLINEE PRIVATE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                    ),
                    color = TextDim.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            items(extraRoutes) { route ->
                ExtraBusCard(
                    route = route,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route.websiteUrl))
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun BusAreaCard(
    area: BusArea,
    onLineClick: (BusLine) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(12.dp),
    ) {
        Text(
            text = area.name.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            ),
            color = Amber,
        )
        Spacer(Modifier.height(8.dp))

        area.lines.forEach { line ->
            BusLineRow(line = line, onClick = { onLineClick(line) })
            if (line != area.lines.last()) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Divider.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
private fun BusLineRow(line: BusLine, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.code,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = Cyan,
            modifier = Modifier.width(52.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.name,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                color = TextPrimary,
            )
        }
        Text(
            text = "PDF →",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp),
            color = TextDim,
        )
    }
}

@Composable
private fun StrikeSection(
    strikes: List<StrikeInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "SCIOPERI",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                ),
                color = Red,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "AGGIORNA",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = Red.copy(alpha = 0.7f),
                )
            }
        }

        if (isLoading) {
            LoadingIndicator()
            return
        }

        if (strikes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NESSUNO SCIOPERO IN CORSO",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = Green,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "linee vesuviane — tratta Napoli↔Sorrento",
                        style = TextStyle(fontSize = 11.sp),
                        color = TextDim,
                    )
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(strikes) { strike ->
                StrikeCard(
                    strike = strike,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(strike.link))
                        context.startActivity(intent)
                    },
                )
            }
        }
    }
}

@Composable
private fun StrikeCard(strike: StrikeInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(1.dp, Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Red, RoundedCornerShape(1.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = strike.title,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp,
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = strike.announcement,
            style = TextStyle(fontSize = 11.sp),
            color = TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        if (!strike.guaranteeHours.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Green, RoundedCornerShape(1.dp)),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = strike.guaranteeHours,
                    style = TextStyle(fontSize = 11.sp),
                    color = Green,
                )
            }
        }

        val hasDepartures = strike.lastNapoliSorrento != null || strike.lastSorrentoNapoli != null ||
            strike.firstNapoliSorrento != null || strike.firstSorrentoNapoli != null

        if (hasDepartures) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BoardRowDark, RoundedCornerShape(2.dp))
                    .padding(10.dp),
            ) {
                Column {
                    Text(
                        text = "SORRENTO ↔ NAPOLI",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = Amber,
                    )
                    Spacer(Modifier.height(4.dp))

                    if (strike.lastNapoliSorrento != null) {
                        DepartureRow("Ultima Napoli→Sorrento", strike.lastNapoliSorrento)
                    }
                    if (strike.lastSorrentoNapoli != null) {
                        DepartureRow("Ultima Sorrento→Napoli", strike.lastSorrentoNapoli)
                    }
                    if (strike.firstNapoliSorrento != null) {
                        DepartureRow("Prima Napoli→Sorrento", strike.firstNapoliSorrento)
                    }
                    if (strike.firstSorrentoNapoli != null) {
                        DepartureRow("Prima Sorrento→Napoli", strike.firstSorrentoNapoli)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Apri avviso →",
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = Red.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun MetroSection(metroLines: List<MetroLine>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(metroLines) { line ->
            val accent = Color(line.colorHex)
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card)
                    .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(accent, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = line.name,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 2.sp),
                        color = accent,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = line.colorName,
                        style = TextStyle(fontSize = 10.sp),
                        color = accent.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = line.operator,
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 1.sp),
                        color = TextDim,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().background(BoardRowDark, RoundedCornerShape(2.dp)).padding(10.dp),
                ) {
                    Text(
                        text = "FERMATE",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp),
                        color = accent,
                    )
                    Spacer(Modifier.height(4.dp))
                    line.stops.forEachIndexed { idx, stop ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                            val dotColor = when {
                                idx == 0 -> accent
                                idx == line.stops.lastIndex -> accent.copy(alpha = 0.5f)
                                else -> accent.copy(alpha = 0.2f)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(14.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(dotColor, RoundedCornerShape(1.dp)))
                                if (idx < line.stops.lastIndex) {
                                    Box(modifier = Modifier.width(1.dp).height(12.dp).background(accent.copy(alpha = 0.15f)))
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = stop.name,
                                    style = TextStyle(fontWeight = if (idx == 0 || idx == line.stops.lastIndex) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp),
                                    color = if (idx == 0 || idx == line.stops.lastIndex) TextPrimary else TextSecondary,
                                )
                                if (stop.connections.isNotBlank()) {
                                    Text(
                                        text = stop.connections,
                                        style = TextStyle(fontSize = 9.sp),
                                        color = accent.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "FREQUENZA", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, letterSpacing = 1.sp), color = TextDim)
                        Text(text = line.frequency, style = TextStyle(fontSize = 10.sp), color = TextSecondary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "PRIMA / ULTIMA", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.sp, letterSpacing = 1.sp), color = TextDim)
                        Text(text = line.firstLast, style = TextStyle(fontSize = 10.sp), color = TextSecondary)
                    }
                }

                if (line.info.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(text = line.info, style = TextStyle(fontSize = 10.sp), color = TextDim, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun StopList(stops: List<String>, accent: Color) {
    stops.forEachIndexed { idx, stop ->
        Row(
            modifier = Modifier.padding(vertical = 3.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(6.dp)
                    .background(
                        when {
                            idx == 0 -> accent
                            idx == stops.lastIndex -> accent.copy(alpha = 0.5f)
                            else -> TextDim.copy(alpha = 0.3f)
                        },
                        RoundedCornerShape(1.dp),
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (stop.contains("(")) stop.substringBefore("(").trim() else stop,
                    style = TextStyle(
                        fontWeight = if (idx == 0 || idx == stops.lastIndex) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                    color = if (idx == 0 || idx == stops.lastIndex) TextPrimary else TextSecondary,
                )
                if (stop.contains("(")) {
                    Text(
                        text = stop.substringAfter("(").removeSuffix(")").trim(),
                        style = TextStyle(fontSize = 9.sp, letterSpacing = 0.3.sp),
                        color = accent.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtraBusCard(route: ExtraBusRoute, onClick: () -> Unit) {
    val accent = Color(route.color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Card)
            .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, RoundedCornerShape(1.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = route.operator,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                ),
                color = accent,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = route.routeName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
            color = TextPrimary,
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = route.info,
            style = TextStyle(fontSize = 11.sp),
            color = TextSecondary,
        )

        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BoardRowDark, RoundedCornerShape(2.dp))
                .border(1.dp, accent.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                .padding(10.dp),
        ) {
            Text(
                text = "FERMATE — ANDATA",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                ),
                color = accent,
            )
            Spacer(Modifier.height(6.dp))
            StopList(stops = route.stops, accent = accent)

            if (route.returnStops.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "FERMATE — RITORNO",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = accent,
                )
                Spacer(Modifier.height(6.dp))
                StopList(stops = route.returnStops, accent = accent)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "PARTENZE DA SORRENTO",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                ),
                color = TextDim,
            )
            Text(
                text = route.departures.joinToString(" · "),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                color = TextPrimary,
            )

            if (route.returns.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "RITORNI DA DESTINAZIONE",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                    ),
                    color = TextDim,
                )
                Text(
                    text = route.returns.joinToString(" · "),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                    color = TextPrimary,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Apri sito →",
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            color = accent.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun DataFreshnessFooter(
    strikeTimestamp: Long,
    selectedMode: TransportMode,
    scheduleState: UiState,
) {
    val sdf = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.ITALY) }

    val strikeStr = remember(strikeTimestamp) {
        if (strikeTimestamp > 0) "scioperi: ${sdf.format(java.util.Date(strikeTimestamp))}" else ""
    }
    val trainStr = remember(scheduleState) {
        if (scheduleState is UiState.Success) "treni: ${sdf.format(java.util.Date(scheduleState.timestamp))}" else ""
    }

    val footerText = buildString {
        if (selectedMode == TransportMode.STRIKE && strikeStr.isNotEmpty()) append(strikeStr)
        else if (selectedMode == TransportMode.TRAIN && trainStr.isNotEmpty()) append(trainStr)
        else append("orari 2026 · ANM · EAV · SITA · CURRERI · MAROZZI · ITABUS")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (strikeStr.isNotEmpty() || trainStr.isNotEmpty()) "⟳" else "",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            ),
            color = TextDim.copy(alpha = 0.5f),
        )
        Text(
            text = " $footerText",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            ),
            color = TextDim.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun DepartureRow(label: String, time: String) {
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 0.3.sp),
            color = TextDim,
            modifier = Modifier.width(140.dp),
        )
        Text(
            text = time,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
            color = TextPrimary,
        )
    }
}
