package com.eavorari.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eavorari.data.EavRepository
import com.eavorari.data.model.AppSettings
import com.eavorari.data.model.Station
import com.eavorari.data.model.StationsState
import com.eavorari.ui.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onStationClicked: (Station, String) -> Unit = { _: Station, _: String -> },
) {
    val context = LocalContext.current
    val prefs = remember { com.eavorari.data.SecurePrefs.get(context) }

    var savedId by remember { mutableStateOf(prefs.getString("station_id", "") ?: "") }
    var savedName by remember { mutableStateOf(prefs.getString("station_name", "") ?: "") }
    var savedTipo by remember { mutableStateOf(prefs.getString("tipo", "P") ?: "P") }
    var peninsulaEnabled by remember { mutableStateOf(prefs.getBoolean("peninsula_enabled", savedName.isNotBlank())) }

    val repository = remember { EavRepository() }
    var stationsState by remember { mutableStateOf<StationsState>(StationsState.Loading) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        stationsState = try {
            StationsState.Success(repository.fetchStations())
        } catch (e: Exception) {
            StationsState.Error(e.message ?: "")
        }
    }

    val stations = when (val s = stationsState) {
        is StationsState.Success -> s.stations
        else -> emptyList()
    }
    val filtered = remember(stations, searchQuery) {
        if (searchQuery.isBlank()) stations
        else stations.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding().navigationBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Card)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "IMPOSTAZIONI",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 4.sp,
                    ),
                    color = Amber,
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    ) {
                        Text(
                            text = "TRATTA PREFERITA",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = Amber,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "All'apertura, vai direttamente agli orari di questa stazione",
                            style = TextStyle(fontSize = 10.sp),
                            color = TextDim,
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text("Cerca stazione...", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp), color = TextDim)
                            },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).border(1.dp, if (searchQuery.isNotBlank()) Amber.copy(alpha = 0.6f) else Divider, RoundedCornerShape(4.dp)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Amber,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = Amber,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = CardVariant,
                                unfocusedContainerColor = CardVariant,
                            ),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, letterSpacing = 1.sp),
                            singleLine = true,
                        )

                        if (savedName.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(BoardRowDark).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(modifier = Modifier.size(5.dp).background(Amber, RoundedCornerShape(1.dp)))
                                Spacer(Modifier.width(8.dp))
                                Text(text = savedName, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = TextPrimary, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(2.dp)).clickable {
                                        savedId = ""; savedName = ""
                                        prefs.edit().remove("station_id").remove("station_name").apply()
                                    }.padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text("RIMUOVI", style = TextStyle(fontSize = 9.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Monospace), color = Red)
                                }
                            }
                        }
                    }
                }

                if (searchQuery.isNotBlank()) {
                    items(filtered) { station ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).clickable {
                                savedId = station.id; savedName = station.name
                                prefs.edit().putString("station_id", station.id).putString("station_name", station.name).putBoolean("peninsula_enabled", true).apply()
                                peninsulaEnabled = true
                                onStationClicked(station, savedTipo)
                            }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(5.dp).background(Amber.copy(alpha = 0.4f), RoundedCornerShape(1.dp)))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = station.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp), color = TextPrimary)
                                if (station.lineName.isNotBlank()) {
                                    Text(text = station.lineName, style = TextStyle(fontSize = 10.sp), color = TextDim)
                                }
                            }
                            Text(
                                text = if (station.id == savedId) "✓" else "",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                color = Green,
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    ) {
                        Text(
                            text = "TIPO PREDEFINITO",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = Amber,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("P" to "PARTENZE", "A" to "ARRIVI").forEach { (value, label) ->
                                val selected = savedTipo == value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (selected) Amber else CardVariant)
                                    .clickable(onClick = {
                                        savedTipo = value
                                        prefs.edit().putString("tipo", value).apply()
                                    }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = label,
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                letterSpacing = 2.sp,
                                            ),
                                            color = if (selected) Background else TextDim,
                                        )
                                        if (selected) {
                                            Spacer(Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(2.dp)
                                                    .background(Background, RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    ) {
                        Text(
                            text = "FILTRO PENISOLA",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = Amber,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mostra solo linee della penisola sorrentina",
                                    style = TextStyle(fontSize = 12.sp),
                                    color = TextPrimary,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Nei tab AUTOBUS e NOTTURNI, filtra solo i servizi della Costiera Sorrentina e Notturni Vesuviani.",
                                    style = TextStyle(fontSize = 10.sp),
                                    color = TextDim,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = peninsulaEnabled,
                                onCheckedChange = {
                                    peninsulaEnabled = it
                                    prefs.edit().putBoolean("peninsula_enabled", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Amber,
                                    checkedTrackColor = Amber.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextDim,
                                    uncheckedTrackColor = CardVariant,
                                ),
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    ) {
                        Text(
                            text = "PREFERENZE",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = Red,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                .clickable {
                                    savedId = ""; savedName = ""
                                    peninsulaEnabled = false
                                    prefs.edit().clear().apply()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "CANCELLA TUTTE LE IMPOSTAZIONI",
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 2.sp,
                                ),
                                color = Red,
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Card).border(0.5.dp, Divider.copy(alpha = 0.4f), RoundedCornerShape(4.dp)).padding(14.dp),
                    ) {
                        Text(
                            text = "INFO APP",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                            ),
                            color = TextDim,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Versione 1.0",
                            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Orari trasporti pubblici per la Penisola Sorrentina e l'area vesuviana. Treni EAV, autobus EAV/SITA/Curreri/Marozzi/Itabus, metropolitane ANM/EAV/Trenitalia, e monitoraggio scioperi.",
                            style = TextStyle(fontSize = 11.sp),
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Dati: EAV · ANM · Trenitalia · SITA · Curreri · Marozzi · Itabus",
                            style = TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                            color = TextDim,
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

private val CardVariant = Color(0xFF252D44)
