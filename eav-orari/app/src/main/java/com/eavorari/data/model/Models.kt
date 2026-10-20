package com.eavorari.data.model

data class Station(
    val id: String,
    val name: String,
    val lineId: String,
    val lineName: String,
)

data class TrainInfo(
    val trainNumber: String,
    val category: String,
    val destination: String,
    val info: String,
    val platform: String,
    val time: String,
    val delay: String,
)

data class BusLine(
    val code: String,
    val name: String,
    val area: String,
    val pdfUrl: String,
    val description: String = "",
)

data class BusArea(
    val name: String,
    val lines: List<BusLine>,
)

data class StrikeInfo(
    val title: String,
    val link: String,
    val announcement: String,
    val guaranteeHours: String?,
    val lastNapoliSorrento: String?,
    val firstNapoliSorrento: String?,
    val lastSorrentoNapoli: String?,
    val firstSorrentoNapoli: String?,
    val rawDate: String,
)

data class StrikeDeparture(
    val from: String,
    val to: String,
    val time: String,
)

data class ExtraBusRoute(
    val operator: String,
    val routeName: String,
    val stops: List<String>,
    val returnStops: List<String> = emptyList(),
    val departures: List<String>,
    val returns: List<String>,
    val websiteUrl: String,
    val info: String,
    val color: Long,
)

data class AppSettings(
    val defaultStationId: String = "",
    val defaultStationName: String = "",
    val defaultTipo: String = "P",
)

sealed interface UiState {
    data object Initial : UiState
    data object Loading : UiState
    data class Success(
        val stationName: String,
        val tipo: String,
        val trains: List<TrainInfo>,
        val timestamp: Long,
    ) : UiState
    data class Error(val message: String) : UiState
}

sealed interface StationsState {
    data object Loading : StationsState
    data class Success(val stations: List<Station>) : StationsState
    data class Error(val message: String) : StationsState
}

data class MetroLine(
    val name: String,
    val colorName: String,
    val colorHex: Long,
    val operator: String,
    val stops: List<MetroStop>,
    val frequency: String,
    val firstLast: String,
    val info: String,
)

data class MetroStop(
    val name: String,
    val connections: String = "",
    val notes: String = "",
)

enum class TransportMode(val label: String, val icon: String) {
    TRAIN("TRENI", "\uD83D\uDE82"),
    BUS("AUTOBUS", "\uD83D\uDE8C"),
    NIGHT_BUS("NOTTURNI", "\uD83C\uDF19"),
    STRIKE("SCIOPERI", "\u26A0\uFE0F"),
    METRO("METR\u00D2", "\uD83D\uDE87"),
}
