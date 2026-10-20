package com.eavorari.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eavorari.data.EavRepository
import com.eavorari.data.model.Station
import com.eavorari.data.model.StationsState
import com.eavorari.data.model.StrikeInfo
import com.eavorari.data.model.TrainInfo
import com.eavorari.data.model.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScheduleViewModel : ViewModel() {
    private val repository = EavRepository()

    private val _stationsState = MutableStateFlow<StationsState>(StationsState.Loading)
    val stationsState: StateFlow<StationsState> = _stationsState.asStateFlow()

    private val _scheduleState = MutableStateFlow<UiState>(UiState.Initial)
    val scheduleState: StateFlow<UiState> = _scheduleState.asStateFlow()

    private val _selectedStation = MutableStateFlow<Station?>(null)
    val selectedStation: StateFlow<Station?> = _selectedStation.asStateFlow()

    private val _tipo = MutableStateFlow("P")
    val tipo: StateFlow<String> = _tipo.asStateFlow()

    private val _destinationFilter = MutableStateFlow<String?>(null)
    val destinationFilter: StateFlow<String?> = _destinationFilter.asStateFlow()

    private val _filteredTrains = MutableStateFlow<List<TrainInfo>>(emptyList())
    val filteredTrains: StateFlow<List<TrainInfo>> = _filteredTrains.asStateFlow()

    private val _strikes = MutableStateFlow<List<StrikeInfo>>(emptyList())
    val strikes: StateFlow<List<StrikeInfo>> = _strikes.asStateFlow()

    private val _strikesLoading = MutableStateFlow(false)
    val strikesLoading: StateFlow<Boolean> = _strikesLoading.asStateFlow()

    private val _strikeTimestamp = MutableStateFlow<Long>(0L)
    val strikeTimestamp: StateFlow<Long> = _strikeTimestamp.asStateFlow()

    private val _lastBusUpdate = MutableStateFlow(0L)
    val lastBusUpdate: StateFlow<Long> = _lastBusUpdate.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadStations()
        fetchStrikes()
    }

    fun loadStations() {
        viewModelScope.launch {
            _stationsState.value = StationsState.Loading
            try {
                val stations = repository.fetchStations()
                _stationsState.value = StationsState.Success(stations)
            } catch (e: Exception) {
                _stationsState.value = StationsState.Error(e.message ?: "Errore di connessione")
            }
        }
    }

    fun selectStation(station: Station) {
        _selectedStation.value = station
        _destinationFilter.value = null
    }

    fun setTipo(tipo: String) {
        _tipo.value = tipo
    }

    fun setDestinationFilter(dest: String?) {
        _destinationFilter.value = dest
    }

    fun loadSchedule() {
        val station = _selectedStation.value ?: return
        viewModelScope.launch {
            _scheduleState.value = UiState.Loading
            val result = repository.fetchSchedule(station.id, _tipo.value)
            _scheduleState.value = result
            applyFilter()
        }
    }

    fun applyFilter() {
        val state = _scheduleState.value
        val filter = _destinationFilter.value
        if (state is UiState.Success) {
            _filteredTrains.value = if (filter != null) {
                state.trains.filter { it.destination.contains(filter, ignoreCase = true) }
            } else {
                state.trains
            }
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                loadSchedule()
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
    }

    fun fetchStrikes() {
        viewModelScope.launch {
            _strikesLoading.value = true
            _strikes.value = repository.fetchStrikesCached()
            _strikeTimestamp.value = System.currentTimeMillis()
            _strikesLoading.value = false
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
